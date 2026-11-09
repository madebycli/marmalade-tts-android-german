# Test artifacts — German Pocket

APKs and model weights are GitHub Release assets, never new Git source files.

- [ARM64 test APK and JVM/signature reports](https://github.com/madebycli/marmalade-tts-android-german/releases/tag/pocket-de-test-11b342dc) — source11b342dc;124,836,372bytes.
  SHA-256: `87c491adf10673765d4d2ad390b6fdeee6d7e00b8a7b154803dd14f4b5536cd1`.
- [Opt-in German model download, all27voices](https://github.com/madebycli/marmalade-tts-android-german/releases/tag/pocket-de-bundle-v1) —245,862,258bytes;367,588,914bytes installed.
  SHA-256: `47f36b2862f81fe8faf85722377e1898417206aa2faf2ffff37a9c4214e517ef`.

Both actual download URLs were read in full and checksums verified. APK signature and ARM64 package were additionally inspected locally. CI:1933tests for each of fdroid/play,0failures/errors/skips. No device or emulator test. The temporary debug signer may change on another CI build.

Install the APK, then Engines → Pocket TTS German → Install; select Anna or another voice. Do not manually extract the tarball. The original Kitten seed stays as upstream; German weights and all27voice states are separate downloads. Full instructions/limits: [GERMAN_POCKET.md](../GERMAN_POCKET.md).
