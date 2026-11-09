# German Pocket TTS — model and voice attribution

This fork downloads a separate German bundle only when the user installs it.
No German weights, voice states or audio are embedded in the APK or Git history.
The app source remains MIT; the APK remains GPL-3.0-or-later because it includes
espeak-ng, even though Pocket itself consumes text directly without espeak.

## Model and exported graphs

Pocket TTS by Kyutai: https://huggingface.co/kyutai/pocket-tts
and https://github.com/kyutai-labs/pocket-tts (code MIT, weights CC-BY-4.0).
German six-layer checkpoint, 24 kHz mono, 1024 conditioning dimensions.
Exported locally with the preserved MIT KevinAHM exporter copy:
https://github.com/lomotron/pocket-tts-onnx-export/tree/8f52199533098963fe1cbd90bed35bb20d871b5e
The current checkpoint is Kyutai's official public non-cloning model:
`pocket-tts-without-voice-cloning`, revision
1e08e6a23401048648a9fdcfde2f89348215c2a7. The matching JSON tokenizer is pinned
at 4e1e0a3e611c51c0b4ed8174fc10f32a54644303. Both are CC-BY-4.0.
The JSON Unigram vocabulary was converted losslessly to SentencePiece for the
existing lightweight Kotlin tokenizer; token IDs and identity normalization are
preserved. flow_lm_main is dynamically INT8-quantized (MatMul/QInt8); flow_lm_flow,
Mimi encoder/decoder and text conditioner stay FP32, like the English mixed bundle.
Non-semantic ONNX trace metadata is stripped for reproducibility and to avoid embedding local export paths. Operators and trained tensors are unchanged.
The non-cloning checkpoint's encoder stays unmodified; this German engine exposes
only the 27 official voices and explicitly rejects the dormant cloning API.
No gated original weights, gated reference audio or unrelated English encoder is used.
The English bundle URLs and hashes remain unchanged.

An older public German ONNX conversion (KevinAHM revision58a6d00c) was rejected:
its weights/tokenizer are from a different model generation than today's official
voice states. They are not used in this fork's final bundle. The exporter itself
remains usable with the current checkpoint after a small --config selection fix.
Reproduction scripts and input checksums are in scripts/pocket-bundle/.
No exporter/PyTorch code executes in or ships with the Android app.

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
