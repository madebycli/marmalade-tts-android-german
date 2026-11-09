# Executed verification

- `voice-start-fix.md`: sanitized diagnosis and follow-up phone checks for slow
  prepared-voice loading and cancellation. The original private log is not in Git.
- `voice-start-android-ci.json`, `voice-start-verification.json`: source9d87b53,
  run42703052871;1944tests per distribution,0failures/errors/skips. Downloaded
  APK hash, signature, ARM64 and fix-in-DEX checks. Different test certificate
  from the earlier APK; no in-place update. No post-fix phone result claimed.

- `android-ci.json`: actual final Gradle XML counts,1933tests per distribution,0failures/errors/skips; source11b342dc, run42694557306.
- `apk-verification.txt`: CI apksigner output; the published APK was also downloaded and checked locally.
- `verification.json`: exact APK/model sizes, hashes, published-download checks, source and run IDs, and honest device-test flags.
- `bundle.json`: exact final German data package; independently reproduced with the same SHA-256 by two GitHub builds.
- `host-voices.json`: local CPU Python/ORT smoke test, all27voices.
- `ci-host-voices.json`: corresponding latest GitHub CPU smoke-test report, all27voices; timings include the host environment, not a Pixel.
- `tokenizer-parity.json`:25normalized texts and matching production Kotlin/official JSON-tokenizer IDs.
- `export-mimi.txt`, `export-flow.txt`: successful numerical exporter checks against PyTorch; warning lines are preserved.

The unit-test XML ZIP is attached to the APK release. No phone/emulator, Android audio path, listening quality, on-device stop/streaming, or Pixel RTF/PSS result is claimed. The German prototype uses fixed1000-position KV caches; English's dynamic-cache export optimization is not yet ported.
