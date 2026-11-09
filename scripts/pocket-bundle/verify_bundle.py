#!/usr/bin/env python3
"""CPU-only technical audio smoke test; not a listening grade or an Android benchmark.
Uses the pinned public KevinAHM reference only on the build host. Not shipped in Android.
"""
import argparse
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import resource
import struct
import time
import wave
import zipfile
import numpy as np

REFERENCE_SHA256 = '4381a4396ba08b2626a25a87001e3c51dbacd136e1022d2d40a8cefb14b44be0'
REFERENCE_URL = 'https://huggingface.co/KevinAHM/pocket-tts-onnx/resolve/58a6d00cf13d239b6748cb0769f35c580a8f606c/pocket_tts_onnx.py'


def read_prompt(file, manifest):
    with gzip.open(file, 'rb') as stream:
        magic, rows, count = struct.unpack('>iii', stream.read(12))
        assert magic == 0x50505331 and 0 < rows < 1000 and count == len(manifest)
        states = {}
        for entry in manifest:
            index, kind, rank = struct.unpack('>iii', stream.read(12))
            shape = struct.unpack('>' + 'q' * rank, stream.read(8 * rank))
            size = struct.unpack('>i', stream.read(4))[0]
            data = stream.read(size)
            assert index == entry['index'] and shape == tuple(entry['shape']) and len(data) == size
            assert kind == {'float32': 0, 'int64': 1, 'bool': 2}[entry['dtype']]
            states[entry['input_name']] = np.frombuffer(data, dtype=['<f4', '<i8', '?'][kind]).reshape(shape).copy()
        assert stream.read() == b''
        return states


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--bundle', type=Path, required=True)
    ap.add_argument('--reference', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    args = ap.parse_args()
    assert hashlib.file_digest(args.reference.open('rb'), 'sha256').hexdigest() == REFERENCE_SHA256
    module_spec = importlib.util.spec_from_file_location('pocket_reference', args.reference)
    module = importlib.util.module_from_spec(module_spec)
    module_spec.loader.exec_module(module)
    def options(self):
        opts = module.ort.SessionOptions()
        opts.intra_op_num_threads = opts.inter_op_num_threads = 1
        opts.add_session_config_entry('session.intra_op.allow_spinning', '0')
        return opts
    module.PocketTTSOnnx._make_session_options = options
    started = time.monotonic()
    engine = module.PocketTTSOnnx(models_dir=str(args.bundle), language='german', precision='int8', device='cpu', temperature=.3)
    load_ms = (time.monotonic() - started) * 1000
    assert len(engine.predefined_voices) == 27
    assert len(engine.flow_state_manifest) == 18 and len(engine.mimi_state_manifest) == 56
    args.output.mkdir(parents=True, exist_ok=True)
    results = []
    for voice in engine.predefined_voices:
        engine._voice_state_cache.clear()
        engine._voice_state_cache['prepared'] = read_prompt(args.bundle / 'voice_states' / (voice + '.prompt.gz'), engine.flow_state_manifest)
        np.random.seed(12345)
        started = time.monotonic()
        first = None
        chunks = []
        for part in engine.stream('Guten Morgen. Heute scheint die Sonne.', voice='prepared', max_frames=180):
            if first is None:
                first = time.monotonic()
            chunks.append(part)
        audio = np.concatenate(chunks) if chunks else np.zeros(0, dtype=np.float32)
        seconds = len(audio) / 24000
        elapsed = time.monotonic() - started
        rms = float(np.sqrt(np.mean(audio ** 2))) if len(audio) else 0
        assert np.isfinite(audio).all() and 1 < seconds < 10 and rms > .001, (voice, seconds, rms)
        with wave.open(str(args.output / (voice + '.wav')), 'wb') as out:
            out.setnchannels(1); out.setsampwidth(2); out.setframerate(24000)
            out.writeframes((np.clip(audio, -1, 1) * 32767).astype('<i2').tobytes())
        row = dict(voice=voice, audio_seconds=round(seconds, 3), rms=round(rms, 4),
                   first_chunk_ms=round((first - started) * 1000, 1), rtf=round(elapsed / seconds, 3))
        results.append(row)
        print(json.dumps(row), flush=True)
    report = dict(host_only=True, device_tested=False, runtime='onnxruntime 1.23.2 CPU, Python reference, one thread',
                  load_ms=round(load_ms, 1), max_rss_kib=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss, results=results)
    (args.output / 'host-voices.json').write_text(json.dumps(report, indent=2) + '\n')
    with zipfile.ZipFile(args.output / 'German-Pocket-27-voice-samples.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for voice in engine.predefined_voices:
            archive.write(args.output / (voice + '.wav'), voice + '.wav')
        archive.write(args.bundle / 'NOTICE.md', 'NOTICE.md')
        archive.write(args.bundle / 'voices.json', 'voices.json')
        archive.writestr('README.txt', 'Synthetic German CPU test speech, not an Android recording or a voice quality rating.\nSource voices and licences: voices.json and NOTICE.md.\n')


if __name__ == '__main__':
    main()
