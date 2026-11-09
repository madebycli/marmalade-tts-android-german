#!/usr/bin/env python3
"""Export the current public German checkpoint through the existing five-graph exporter.
Export-time dependencies only. Never installed into Android or downloaded by the app.
"""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import os

from prepare_german_bundle import checked_download, HERE

EXPORTER_COMMIT = '8f52199533098963fe1cbd90bed35bb20d871b5e'


def convert_tokenizer(source, target):
    from sentencepiece import sentencepiece_model_pb2 as pb
    import sentencepiece as spm
    from tokenizers import Tokenizer
    data = json.loads(source.read_text())
    assert data['model']['type'] == 'Unigram' and data['model']['byte_fallback']
    model = pb.ModelProto()
    trainer = model.trainer_spec
    trainer.model_type = pb.TrainerSpec.UNIGRAM
    trainer.vocab_size = len(data['model']['vocab'])
    trainer.byte_fallback = True
    trainer.unk_id, trainer.bos_id, trainer.eos_id, trainer.pad_id = 0, 1, 2, 3
    normalizer = model.normalizer_spec
    normalizer.name = 'identity'
    normalizer.add_dummy_prefix = True
    normalizer.remove_extra_whitespaces = False
    normalizer.escape_whitespaces = True
    for index, (piece, score) in enumerate(data['model']['vocab']):
        entry = model.pieces.add()
        entry.piece, entry.score = piece, score
        entry.type = (pb.ModelProto.SentencePiece.UNKNOWN if index == 0 else
                      pb.ModelProto.SentencePiece.CONTROL if index in (1, 2, 3) else
                      pb.ModelProto.SentencePiece.BYTE if piece.startswith('<0x') and piece.endswith('>') else
                      pb.ModelProto.SentencePiece.NORMAL)
    target.write_bytes(model.SerializeToString())
    official = Tokenizer.from_file(str(source))
    converted = spm.SentencePieceProcessor(model_file=str(target))
    checks = ['', 'Hallo Welt.', 'Äpfel, Öl und süße Grüße!', '14. März 2026 um 16:30 Uhr',
              '50 % & Köln → Düsseldorf', 'a  b', '  Text  ', '日本語', 'Ⅳ', '👋']
    for text in checks:
        assert official.encode(text).ids == converted.encode(text), ('Tokenizer mismatch', text)
    print(f'Tokenizer parity: {len(checks)} checks', flush=True)


def canonicalize_graph(path):
    """Remove non-semantic exporter traces/temporary paths; retain every operator and weight."""
    import onnx
    from google.protobuf.descriptor import FieldDescriptor
    model = onnx.load(str(path))
    def strip(message):
        for field, value in list(message.ListFields()):
            if field.name in ('doc_string', 'metadata_props'):
                message.ClearField(field.name)
            elif field.type == FieldDescriptor.TYPE_MESSAGE:
                if field.is_repeated:
                    for item in value:
                        strip(item)
                else:
                    strip(value)
    strip(model)
    onnx.checker.check_model(model)
    onnx.save(model, str(path))


def main():
    import yaml
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--exporter-dir', type=Path, required=True)
    ap.add_argument('--cache', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    args = ap.parse_args()
    exporter = args.exporter_dir.resolve()
    revision = subprocess.check_output(['git', '-C', str(exporter), 'rev-parse', 'HEAD'], text=True).strip()
    if revision != EXPORTER_COMMIT:
        raise ValueError(f'Expected exporter {EXPORTER_COMMIT}; use the pinned preserved copy')
    patch = HERE / 'exporter-compat.patch'
    check = subprocess.run(['git', '-C', str(exporter), 'apply', '--check', str(patch)], capture_output=True)
    if check.returncode == 0:
        subprocess.run(['git', '-C', str(exporter), 'apply', str(patch)], check=True)
    else:
        subprocess.run(['git', '-C', str(exporter), 'apply', '--reverse', '--check', str(patch)], check=True)
    for entry in json.loads((HERE / 'german-sources.json').read_text()):
        if entry['path'].startswith('checkpoint/'):
            checked_download(entry, args.cache / entry['path'])
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    tokenizer = output / 'german-tokenizer.model'
    convert_tokenizer(args.cache / 'checkpoint/tokenizer.json', tokenizer)
    config = yaml.safe_load((exporter / 'pocket_tts/config/german.yaml').read_text())
    config['weights_path'] = str((args.cache / 'checkpoint/german.safetensors').resolve())
    config['flow_lm']['lookup_table']['tokenizer_path'] = str(tokenizer)
    config_path = output / 'german.yaml'
    config_path.write_text(yaml.safe_dump(config))
    env = dict(os.environ, OMP_NUM_THREADS='1', MKL_NUM_THREADS='1')
    runner = HERE / 'run_export.py'
    for script in ['scripts/export_mimi_and_conditioner.py', 'scripts/export_flow_lm.py']:
        subprocess.run([sys.executable, str(runner), str(exporter), script,
                        '--config', str(config_path), '--output_dir', str(output)],
                       check=True, env=env, cwd=exporter)
    subprocess.run([sys.executable, str(runner), str(exporter), '--quantize-main', str(output)],
                   check=True, env=env, cwd=exporter)
    from prepare_german_bundle import GRAPHS
    for name in GRAPHS.values():
        canonicalize_graph(output / name)


if __name__ == '__main__':
    main()
