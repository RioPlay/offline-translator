# Startup repair and upgrade compatibility

The distributed upstream-sync test APK was withdrawn after the user reported it would not launch. The defect was introduced by the local APK assembly process: an ELF metadata-only copy was named `libbindings-metadata.so`, and UniFFI used that basename as the generated fallback library name. All six loader functions in that APK request `bindings-metadata`, while the packaged native library is `libbindings.so`. The normal repository build already passes the correctly named library to UniFFI; this is not attributed to the upstream native engine.

The withdrawn APK hash is `434cc7caa33763cfdc7affc0e28bfc2d6360f46c38c4a369e22c1d590e6f31b9`. Its Library filename now starts with BROKEN-DO-NOT-INSTALL, and [the previous validation report](https://github.com/RioPlay/offline-translator/blob/test/host-gauntlet-2026-10-24/docs/reviews/host-gauntlet-2026-10-24.md) is marked withdrawn on its validation branch. Earlier host passes did not establish Android startup.

## Correction

Source commit: `fbcc831a1be18c6716d82e17af8af5862d02610d`; tree: `d0fe882019a6471d17c8b9b9c44b92ef8acf755e`.

- Pin Kotlin `cdylib_name = "bindings"` in uniffi.toml. The pin applies to all six components even if a temporary metadata input is renamed.
- Validate every generated loader's literal fallback against the native libraries for every selected ABI. The check waits for all selected bindings builds and runs before application compilation, including when a local validation skips generation.
- Retain `libbindings.so` as the basename of metadata-only copies in the local recipe. The original packaged native bytes remain unchanged.
- Bump only the isolated device-test suffix/code from test3/332 to test4/342. The regular release application ID and ordinary production signing configuration are unchanged.

No engine, translation, settings, model-path or permission behavior is changed by this repair.

## Observed validation

| Check | Result |
| --- | --- |
| Original unpinned generation from renamed metadata | All six loaders reproduced the wrong basename |
| Pinned generation from the same renamed metadata | All six loaders use bindings |
| Build guard with staged broken generated files | Rejected with the exact missing-library error |
| Build guard with corrected generated files | Six loaders validated for ARM64 |
| Default multi-ABI task graph | Both ABI binding builds precede validation |
| JVM suite | 103 tests, zero failures/errors/skipped |
| ktlint | Passed |
| Minified deviceTest assembly and fatal lint | Passed |
| Decoded corrected APK | All six native loader methods default to bindings; packaged libbindings.so is present |
| Native/package checks | Three inherited native outputs byte-identical to verified upstream outputs; bundled catalog v7 matches source |
| Regular-package validation build | Compiled/minified/packaged with fatal lint passing; dev.davidv.translator, version code 322 |
| Pixel launch | Not verified |
| Runtime data retention through an upgrade | Not verified |

The candidate is `translator-test4-arm64.apk`, SHA-256 `afee9ad9420e5ab9213919f8a077b6d8f13609cf46a049fa2591bbb8ffbf7ea0`. It is a non-debuggable ARM64 test build with launcher name Translator and application ID `dev.davidv.translator.devicetest`. It must not be described as a verified-working official update. Its signature differs from the withdrawn test APK, so it also cannot update that installed test copy in place.

Native outputs were reused from the previously verified matching upstream artifact; this was not a fresh Android C++/Rust build. Host UniFFI was rebuilt and bindings regenerated with checksums enabled. No loader aliases or disabled ABI checksum checks are packaged. Full Android lint's previously documented 25 errors and advisory findings are outside this startup repair; a clean full-lint verdict is not claimed.

## Runtime test limitation

An API-30 Google APIs x86_64 image with ARM64 native translation was attempted without KVM. The first configuration used 1536 MiB/two CPUs; a bounded retry used 3072 MiB/four CPUs. Android's framework restarted during testing, and the official unmodified v0.8.6 APK also failed installation in StorageManagerService because PackageManagerInternal was null. This is not an application launch result. The emulator was shut down. No reliable fresh-install or upgrade test completed, and no Pixel crash trace was supplied.

The next necessary check is a real Pixel launch of the corrected candidate, followed by installed-pack translation and same-signer upgrade/data-retention testing. The mechanical loader defect is corrected and independently checked in the APK; it is not yet established that this was the Pixel's only startup problem.

## Official in-place updates

The genuine GitHub v0.8.6 ARM64 APK was downloaded and its release digest verified: `b4ac1f4e782966630491a88a14fd6146f059bac904e6d0696115d325a6370ae3`.

| Identity | Official v0.8.6 | Local regular-package validation |
| --- | --- | --- |
| Application ID | dev.davidv.translator | dev.davidv.translator |
| ARM64 version code | 312 | 322 |
| Signing certificate SHA-256 | 2b3806e745d809018a51be58d0635ffc74cc9733439407ab1ed0424a4db3e1fb | 62e4f22147f5974875903ad2e34282d67abd667d1d7151de88ea1271e3b66dbb |

The certificates differ, so this local artifact cannot update that official installation. Android requires the installed distribution's signing identity or an authorized rotation. App ID and version-number changes cannot grant that authority. The local regular-package validation uses an uncommitted Gradle init script to select a higher version and the test signer; those overrides are not production defaults and that artifact is not distributed as an official update. The normal repository release remains unsigned until the appropriate publisher signs it.

Ordinary release identity, SharedPreferences name app_settings, persisted settings keys and supported model storage paths remain unchanged by this repair. The existing upstream catalog code cleans superseded catalog caches and an obsolete Kokoro ONNX core; therefore unconditional preservation of every historical cache/blob is not promised. No additional data-deletion code was introduced. Runtime retention of settings and supported installed packs still needs a same-signer upgrade test.

Do not uninstall/clear the user's official app, bypass signature checks, or silently switch application IDs to disguise an upgrade. A maintainer/distributor-signed release is the safe route for an official in-place update. A separately maintained fork requires a persistent user-controlled signing key. No official private signing key is available here, and no private key has been uploaded or published by this repair.

## References and continuation

- [Structured evidence](startup-repair-2026-10-25.json)
- [UniFFI Kotlin configuration](https://mozilla.github.io/uniffi-rs/latest/kotlin/configuration.html)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
- [Upstream synchronization context](upstream-sync.md)

All source and documentation changes are confined to RioPlay's fork. A source review in the owned fork does not authorize an upstream submission, merge or release.
