#!/usr/bin/env python3
"""Reproduce the German mixed-precision bundle from pinned, licensed public exports.
No tokens, PyTorch, model execution on Android, or unverified replacement URLs.
Requires numpy and safetensors. The app reads PPS1 via PocketPreparedVoice.
"""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import struct
import tarfile
import urllib.request

import numpy as np
from safetensors.numpy import load_file

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
GRAPHS = {
    'flow_lm_main': 'flow_lm_main_int8.onnx',
    'flow_lm_flow': 'flow_lm_flow.onnx',
    'mimi_encoder': 'mimi_encoder.onnx',
    'mimi_decoder': 'mimi_decoder.onnx',
    'text_conditioner': 'text_conditioner.onnx',
}


def checked_download(entry, target):
    if not target.exists():
        target.parent.mkdir(parents=True, exist_ok=True)
        part = target.with_suffix(target.suffix + '.part')
        with urllib.request.urlopen(entry['url'], timeout=90) as src, part.open('wb') as out:
            shutil.copyfileobj(src, out)
        part.rename(target)
    digest = hashlib.file_digest(target.open('rb'), 'sha256').hexdigest()
    if target.stat().st_size != entry['size'] or digest != entry['sha256']:
        raise ValueError(f"Source checksum mismatch: {entry['path']}")


def adapt_voice(source, manifest):
    """Exporter state paths are stable; newer upstream calls `step` `offset`.
    Match the published reference importer, rejecting missing tensors/truncation.
    Cache padding stays NaN, not zero; current_end remains the export's empty sentinel.
    """
    result = []
    rows = set()
    for spec in manifest:
        dtype = {'float32': '<f4', 'int64': '<i8', 'bool': '?'}[spec['dtype']]
        shape = tuple(spec['shape'])
        value = np.full(shape, np.nan if spec['fill'] == 'nan' else 1 if spec['fill'] == 'ones' else 0, dtype=dtype)
        key = spec['path']
        original = source.get(key)
        if original is None and spec['key'] == 'step':
            original = source.get(spec['module'] + '/offset')
        if spec['key'] == 'current_end' and original is None and shape == (0,):
            result.append(value)
            continue
        if original is None:
            raise ValueError(f'Missing official voice tensor: {key}')
        original = np.asarray(original, dtype=dtype)
        if spec['key'] == 'step':
            rows.add(int(original.reshape(-1)[0]))
        if original.ndim != value.ndim or any(a > b for a, b in zip(original.shape, shape)):
            raise ValueError(f'Official voice tensor does not fit manifest: {key}')
        value[tuple(slice(0, size) for size in original.shape)] = original
        result.append(value)
    if len(rows) != 1 or not 0 < next(iter(rows)) < 1000:
        raise ValueError(f'Inconsistent official voice offsets: {rows}')
    return next(iter(rows)), result


def write_prompt(path, rows, states, manifest):
    with path.open('wb') as raw, gzip.GzipFile(filename='', fileobj=raw, mode='wb', mtime=0, compresslevel=6) as out:
        out.write(struct.pack('>iii', 0x50505331, rows, len(manifest)))
        for spec, data in zip(manifest, states):
            dtype = {'float32': 0, 'int64': 1, 'bool': 2}[spec['dtype']]
            out.write(struct.pack('>iii', spec['index'], dtype, len(spec['shape'])))
            out.write(struct.pack('>' + 'q' * len(spec['shape']), *spec['shape']))
            out.write(struct.pack('>i', data.nbytes))
            out.write(data.tobytes(order='C'))


def deterministic_archive(bundle, archive):
    with archive.open('wb') as raw, gzip.GzipFile(filename='', fileobj=raw, mode='wb', mtime=0, compresslevel=6) as zipped:
        with tarfile.open(fileobj=zipped, mode='w|', format=tarfile.GNU_FORMAT) as tar:
            for file in sorted(bundle.rglob('*')):
                if not file.is_file():
                    continue
                info = tar.gettarinfo(str(file), arcname='pocket-tts-de/' + str(file.relative_to(bundle)))
                info.uid = info.gid = info.mtime = 0
                info.uname = info.gname = ''
                info.mode = 0o644
                with file.open('rb') as src:
                    tar.addfile(info, src)


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--cache', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    args = ap.parse_args()
    lock = json.loads((HERE / 'german-sources.json').read_text())
    bundle = args.output / 'pocket-tts-de'
    bundle.mkdir(parents=True, exist_ok=True)
    for entry in lock:
        cached = args.cache / entry['path']
        checked_download(entry, cached)
        if 'voice' not in entry:
            shutil.copyfile(cached, bundle / entry['path'])
    spec = json.loads((bundle / 'bundle.json').read_text())
    voices = json.loads((HERE / 'german-voices.json').read_text())
    spec.update(bundle_name='pocket-tts-de', language='de', predefined_voices=[v['key'] for v in voices],
                onnx_files=GRAPHS, quantization_variant='mixed-fp32-mimi',
                voice_state_format='PPS1', default_voice='anna')
    (bundle / 'bundle.json').write_text(json.dumps(spec, indent=2, sort_keys=True) + '\n')
    prepared = bundle / 'voice_states'
    prepared.mkdir(exist_ok=True)
    manifest = spec['flow_lm_state_manifest']
    for voice in voices:
        source = load_file(str(args.cache / 'official_voices' / (voice['key'] + '.safetensors')))
        rows, states = adapt_voice(source, manifest)
        write_prompt(prepared / (voice['key'] + '.prompt.gz'), rows, states, manifest)
        print(f"{voice['key']}: {rows} official prompt frames", flush=True)
    (bundle / 'sources.json').write_text(json.dumps(lock, indent=2) + '\n')
    shutil.copyfile(HERE / 'german-voices.json', bundle / 'voices.json')
    licences = bundle / 'LICENSES'
    licences.mkdir(exist_ok=True)
    for name in ['CC-BY-4.0.txt', 'CC0-1.0.txt', 'CC-BY-NC-4.0.txt', 'Pocket-TTS-MIT.txt']:
        shutil.copyfile(REPO / 'LICENSES/full-texts' / name, licences / name)
    shutil.copyfile(REPO / 'LICENSES/pocket-tts-de.md', bundle / 'NOTICE.md')
    archive = args.output / 'pocket-tts-de.tar.gz'
    deterministic_archive(bundle, archive)
    record = {'archive': archive.name, 'sha256': hashlib.file_digest(archive.open('rb'), 'sha256').hexdigest(),
              'sizeBytes': archive.stat().st_size, 'installedSizeBytes': sum(p.stat().st_size for p in bundle.rglob('*') if p.is_file()),
              'voices': len(voices), 'flowLayers': len(manifest) // 3, 'device_tested': False}
    (args.output / 'bundle-result.json').write_text(json.dumps(record, indent=2) + '\n')
    print(json.dumps(record, indent=2))


if __name__ == '__main__':
    main()
