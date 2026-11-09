# Prepared voice startup / cancellation (2026-11-09)

## Evidence from the user-provided Android log

The original private log is not checked in. It contains no recorded model crash
or `tts_speak_failure`. Earlier system TTS successes do not establish that all
requested utterances produced audible speech.

For the detailed German Eponine playback, the first prepared-state phase takes
15,398 ms. Several playback sessions are stopped before first PCM. The cancelled
loader continues holding the synthesis mutex; the newest request waits behind
it. After the voice state is cached, later chunks report phase 1 = 0 ms and
first-PCM latencies of 836 and 918 ms. These are observations from the user's
phone log, not measurements performed on a device by this project.

## Change

- Prepared slots allocate their direct buffers without first filling the entire
  fixed KV cache with NaNs. A prepared file supplies every byte instead. Ordinary
  English/model initialization retains its existing fill policies by default.
- Incomplete, truncated or cancelled prepared slots cannot be bound to ORT.
- Gzip input and its buffering use 64 KiB buffers.
- Stop/cancellation is checked before loading, between manifest entries and
  every 64 KiB payload block, before binding/caching, and before text conditioning.
  Cancelled warmup propagates cancellation instead of logging it as a failure.
- Allocation, decompression/read and tensor binding are timed separately. Logs
  contain durations and the engine name, without adding utterance or file data.

The old log times the complete prepared-state phase. It cannot establish how
much of its 15.4 seconds belongs to allocation, redundant fill, decompression or
binding. A faster phone startup is therefore not yet a verified result.

No model/export/download URL, voice, graph, inference math or dependency changes.
The existing model archive SHA-256 remains
`47f36b2862f81fe8faf85722377e1898417206aa2faf2ffff37a9c4214e517ef`.

## Verification

Eleven regression tests added: complete/empty/truncated/wrong-size states,
default initialization, cancellation before and during loading, retry, failed
overwrite, and reader cancellation.

- Source `9d87b536c3097777b82a0717f0c29c6ab4ed0b47`;
  [Android CI 42703052871](https://github.com/madebycli/marmalade-tts-android-german/actions/runs/42703052871)
  succeeded. 1,944 F-Droid and 1,944 Play tests, 3,888 total; no failures, errors
  or skips. The downloaded XML includes all 9 state tests and 12 reader tests
  for each distribution (including the 11 newly added cases).
- Recompiled production Kotlin reader validated all 27 complete published
  German prompt files. No model or voice file was regenerated.
- ARM64 debug APK built, downloaded from the release, SHA-256 and v2 signature
  checked locally; new diagnostic string present in DEX. No German weights or
  voice states embedded. See `voice-start-verification.json` and
  `voice-start-android-ci.json` for machine-readable results.
- No emulator or device test was performed. Phone performance after the fix
  has not been measured.

[Test APK](https://github.com/madebycli/marmalade-tts-android-german/releases/tag/pocket-de-test-9d87b53),
124,836,372 bytes; SHA-256
`a5df696b98abfcf66f7e8c7b2b7b2d381cfa95fa529e8630b82f750bab7aa6c9`.

The new temporary debug certificate differs from the previous `11b342dc` APK.
It cannot update that installation in place. Removing the previous test app
deletes its private settings and downloaded models; download/install the German
model again afterwards. The original Marmalade app uses a different package.

## Phone checks

1. Select Eponine, read a short fictional sentence after a cold engine start.
2. Stop immediately during loading, then start a different short sentence.
   Check that stopped text is not played and the newest request gets audio.
3. Repeat with Anna and a warm voice, and from another app using system TTS.
4. Export the log after any silence. Look for `prepared voice allocate=… read=…
   bind=…` and subsequent first-PCM/audio-start events.

Use speed 1.0 initially. The model archive itself is unchanged. See the signing
and installation limitation above before replacing the old test app.
