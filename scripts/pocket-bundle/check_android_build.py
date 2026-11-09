#!/usr/bin/env python3
"""Post-build release gate: real XML reports, arm64 only, side-by-side identity, no German weights in APK."""
import json
from pathlib import Path
import subprocess
import os
import zipfile
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
results = {}
required = {'PocketGermanVoiceCatalogTest', 'PocketGermanTextTest', 'PocketPreparedVoiceTest', 'PocketGermanEngineTest', 'EngineRegistryTest'}
for variant in ['testFdroidDebugUnitTest', 'testPlayDebugUnitTest']:
    reports = list((root / 'app/build/test-results' / variant).glob('TEST-*.xml'))
    assert reports, f'Missing test reports: {variant}'
    counts = dict(tests=0, failures=0, errors=0, skipped=0)
    classes = set()
    for file in reports:
        suite = ET.parse(file).getroot()
        classes.add(suite.attrib.get('name', '').rsplit('.', 1)[-1])
        for key in counts:
            counts[key] += int(suite.attrib.get(key, '0'))
    assert required <= classes, (variant, required - classes)
    assert counts['tests'] > 0 and counts['failures'] == counts['errors'] == 0, counts
    results[variant] = counts
apk = root / 'app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk'
with zipfile.ZipFile(apk) as zipped:
    names = zipped.namelist()
    abis = {p.split('/')[1] for p in names if p.startswith('lib/') and p.endswith('.so')}
    assert abis == {'arm64-v8a'}, abis
    assert not any('pocket-tts-de' in p or 'voice_states/' in p or p.endswith('.safetensors') for p in names), 'German weights in APK'
    pocket_payloads = {'flow_lm_main_int8.onnx', 'flow_lm_flow.onnx', 'mimi_encoder.onnx', 'mimi_decoder.onnx', 'text_conditioner.onnx'}
    assert not any(p.startswith('assets/') and (p.rsplit('/', 1)[-1] in pocket_payloads or p.endswith('.prompt.gz')) for p in names)
    assert 'lib/arm64-v8a/libespeak-ng.so' in names
sdk = Path(os.environ['ANDROID_HOME'])
aapt = sdk / 'build-tools/36.0.0/aapt2'
metadata = subprocess.check_output([str(aapt), 'dump', 'badging', str(apk)], text=True)
assert "package: name='app.marmalade.tts.de.debug'" in metadata, metadata[:200]
assert 'Marmalade TTS German' in metadata
results.update(device_tests=False, apk_size=apk.stat().st_size, abi='arm64-v8a', package='app.marmalade.tts.de.debug', german_weights_in_apk=False)
(root / 'TEST_RESULTS.json').write_text(json.dumps(results, indent=2) + '\n')
print(json.dumps(results, indent=2))
