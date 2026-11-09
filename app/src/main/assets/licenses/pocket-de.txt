# German Pocket TTS — model and voice attribution

This fork downloads a separate German bundle only when the user installs it.
No German weights, voice states or audio are embedded in the APK or Git history.
The app source remains MIT; the APK remains GPL-3.0-or-later because it includes
espeak-ng, even though Pocket itself consumes text directly without espeak.

## Model and exported graphs

Pocket TTS by Kyutai: https://huggingface.co/kyutai/pocket-tts
and https://github.com/kyutai-labs/pocket-tts (code MIT, weights CC-BY-4.0).
German six-layer checkpoint, 24 kHz mono, 1024 conditioning dimensions.
Public ONNX conversion by KevinAHM:
https://huggingface.co/KevinAHM/pocket-tts-onnx/tree/58a6d00cf13d239b6748cb0769f35c580a8f606c/onnx/german
(CC-BY-4.0 model card). The five graph files and German SentencePiece model are
unchanged. flow_lm_main is INT8; flow_lm_flow, Mimi encoder/decoder and text
conditioner remain FP32. No English encoder weights or English voice cache
are substituted. The English download URLs and hashes remain unchanged.

The matching exporter is available as a preserved MIT copy:
https://github.com/lomotron/pocket-tts-onnx-export/tree/8f52199533098963fe1cbd90bed35bb20d871b5e
No exporter code is copied into the Android app. This bundle repackages existing
exports; it is not a newly performed or independently verified PyTorch export.

## All 27 official German voice states

Source: Kyutai's public Pocket model without voice cloning,
`languages/german/embeddings/`, revision
a7056f505a4a8ca72d8b5fb1440071d6f1a35739 (CC-BY-4.0 model card).
All official voice IDs are included, without substituting the fork's English
Mary or omitting its retired Marius. The encoder is not run to create these
states. Upstream's already conditioned KV tensors are mapped to the ONNX
state manifest (offset → step, NaN cache padding), then stored as PPS1 gzip.
This is a change of data format, not a new runtime or a voice-cloning feature.

Per-voice reference sources and licences are in the bundled `voices.json`
and the repository's `scripts/pocket-bundle/german-voices.json`.
Sources follow Kyutai's official `_ORIGINS_OF_PREDEFINED_VOICES` at
c91f50f38c6b914d49dbdff51b8d322edf843db2.
https://huggingface.co/kyutai/tts-voices documents the original recordings:

- Alba: Alba MacKenna (Kyutai), CC-BY-4.0.
- Anna, Azelma, Charles, Eponine, Eve, Fantine, George, Jane, Mary, Michael,
  Paul, Vera: CSTR VCTK, University of Edinburgh, CC-BY-4.0.
- Javert and Marius: Kyutai Unmute voice donations, CC0.
- Bill Boerst, Caro Davy, Peter Yearsley, Stuart Bell: Voice-Zero / LibriVox, CC0.
- Estelle: Kyutai Unmute's own recording, CC0.
- **Cosette: Expresso, CC-BY-NC-4.0. Jean: EARS, University of Hamburg,
  CC-BY-NC-4.0. Non-commercial use only for these two voices.**
- Daan, Giovanni, Jürgen, Lola and Rafael: official Kyutai Pocket catalog states,
  distributed under the model repository's CC-BY-4.0 notice. Reference URLs
  are recorded for provenance; no separately gated reference audio is bundled,
  and no additional, separately unverified audio licence is asserted.

The model's CC-BY-4.0 notice applies to all distributed trained voice states;
known more restrictive recording licences are retained above and in the picker.
The all-voices bundle includes non-commercial material and must not be described
as an unrestricted commercial voice bundle. Private non-commercial testing is
its intended use. Voice source language does not promise a native German accent:
all voices synthesize German through the German checkpoint.

Full licence texts: CC-BY-4.0, CC0-1.0, CC-BY-NC-4.0 and Pocket-TTS-MIT in
LICENSES/full-texts and inside the downloadable bundle.
Kyutai prohibits impersonation or voice cloning without explicit lawful consent,
deception, and unlawful or harmful use. No user recording is requested or uploaded.

## German text preprocessing

Kyutai config/german.yaml supplies punctuation replacements. Number, time, date,
currency and abbreviation expansion reuses the existing GermanTextNormalizer
(Misaki German port, Apache-2.0; see LICENSES/kokoro-de.md).
No additional pronunciation runtime or Android dependency is added.
