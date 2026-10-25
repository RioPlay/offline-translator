# Upstream synchronization review

The integration branch combines the fork candidate at `c3c5f726bd815f98dbd821a448fefc14d861748c` with upstream master `bd675fecea875d4e5b3c0f62e24b2a9c3c84f6da`. The merge retains both parents. The validated application source tree is `e2f1dd1e6161fe04300a8345d13ef1a065e4b66c`; the published source merge commit is `924e7ec2b0019b8904d2fc730660658ff402ccd5`. Existing branches and master are preserved.

## Overlap and resolution

| Shared file | Resolution |
| --- | --- |
| LanguageAssetManagerScreen.kt | One textual conflict: retain cached sorting, compare the stable `voiceName` for the default, and show/sort the upstream `displayName` label. Preserve cached language rows and localized search keys. |
| FilePathManager.kt | Keep upstream's v7 catalog filename and the fork's package-specific development-test storage path. |
| values/strings.xml | Keep both upstream TTS-upgrade text and fork processing/network explanations; resource names remain unique. |

Upstream contributes ten commits since the common base, including catalog v7, compact/multi-speaker Kokoro packs and upgrades, native engine locks, stable voice identity/display labels, pending download-size reporting, and the Android 8 Magnifier isolation fix. The app catalog asset, default URL, cache filename, native catalog API and native lockfile changes are synchronized together.

The fork's translation-payload log reductions, asynchronous dictionary lookup, language-list caching, keyboard-accessible swap, Settings explanations, Android API compatibility changes and download reliability guards remain present. No already-merged duplicate fork patch was identified in this upstream interval.

## Validation method

The native sources and lockfiles under `app/src/main/bindings` and `app/src/main/model-converter` exactly match this upstream revision. The upstream Android workflow at run `40113838618` completed successfully. Its ARM64 artifact `12568147153` was downloaded and verified against published archive SHA-256 `ab8c420ccdc2167960a7aea092d835ed8f034c7abb52c2fca4e7f29f03bd0336`.

Local Android validation reuses that artifact's three native outputs: libbindings, libmodel_converter and libc++_shared. This is not a fresh local native compilation. Kotlin bindings were regenerated from the same library's exported UniFFI metadata. Because the APK library lacks a standard ELF symbol table and UniFFI 0.31 reads that table, a scratch metadata-only copy exposes the existing dynamic symbol table to the reader. The original library bytes used for packaging are unchanged. No substitute API stubs or modified native library are packaged. No validation-only workaround changes the committed build scripts.

The 144 default-feature translator-core tests passed in an isolated copy of the pinned core crate on the host. This avoids resolving the full native workspace; it is not the complete native workspace suite or an Android runtime test. JSON catalog parsing, XML resource uniqueness, Python script compilation and Git whitespace/conflict checks also passed.

## Android validation results

- Debug and deviceTest Kotlin/Java compilation passed against regenerated bindings.
- The complete debug JVM suite passed: **103 tests, zero failures, zero errors, zero skipped**, including all 13 HTTP downloader tests and all four dictionary concurrency tests.
- App-wide `ktlintCheck` passed.
- Fresh deviceTest `NewApi` lint at minSdk 23 passed with “No issues found.” This was the targeted API compatibility check, not an assertion that every Android lint category passed.
- R8 minification and minified ARM64 deviceTest assembly passed.
- Final APK verification passed: valid signature, isolated application ID, Translator label, minSdk 23/targetSdk 34, non-debuggable, ARM64 only, all six native libraries present, all three inherited native outputs byte-identical to the verified upstream artifact, and bundled catalog v7 byte-identical to source. The old v6 asset is absent. This is an internal validation package, not a newly published device candidate.

The local Gradle invocation selected `:app:testDebugUnitTest :app:ktlintCheck :app:lintDeviceTest -PlintNewApiOnly :app:assembleDeviceTest -PtargetAbi=arm64-v8a`. It excluded the two native build tasks and regeneration/build of the already-generated UniFFI bindings, as described above. The initial validation command also skipped native stripping, which omitted the libraries from its APK; the package-content check caught this. The corrected assembly keeps that task enabled and packages the existing native libraries as-is, with all final content checks passing. The committed build scripts were not changed. This checks the combined app source against matching upstream native outputs, not a fresh native build. No Android device was connected.

## Outstanding checks

A fresh native build through the normal repository workflow is still a separate verification step. Actual phone testing of translation, catalog refresh, TTS upgrades/default selection, dictionary cancellation, keyboard swap and large-text Settings remains necessary. The previously prepared lopdf update is still separate: the current native lockfile selects lopdf 0.40, and the archived update patch still passes an apply check at the new pinned native revision. It has not been integrated by this sync.

This branch is for fork review. No upstream submission, production release or master merge is included.
