# Maintainer handoff: native PDF security and Android integration

**AI disclosure and scope:** This candidate was developed with substantial OpenAI ChatGPT/Codex assistance using **Sol 6.1 and Astra**. See [what changed, why, how it was researched/tested, and the proposed smaller contribution scope](ai-transparency-and-scope.md). The combined build demonstrates integration; it is not a request to adopt every change. Optional UI/backup policy and fork-specific testing choices can be reviewed separately. No independent security audit or complete human code review is claimed.

**Start here.** The patches are published in RioPlay's forks and the integrated ARM64/ARMv7 builds pass. They are ready for source review. Phone runtime and signed-update validation are still pending; this is not a release or an all-CVEs-cleared claim.

[Coordinated review order and exact commits](submission-plan.md) · [Android draft PR](https://github.com/RioPlay/offline-translator/pull/12) · [Passing CI](https://github.com/RioPlay/offline-translator/actions/runs/40240420210)

## What changes and why

| Repository | Problem and resulting behavior | Files to review |
| --- | --- | --- |
| MuPDF | The pinned snapshot calls itself 1.28.0 but lacks two vendor fixes. Bounds/arithmetic checks reject malformed CFF offsets and invalid image arguments while valid controls still work. | source/fitz/subset-cff.c, source/fitz/draw-unpack.c, source/pdf/pdf-image.c |
| rust-cld2 | A build-only filename regex and TOML parser bring obsolete vulnerable dependencies. Standard suffix matching and TOML 0.9 preserve source selection. | cld2-sys/Cargo.toml, build.rs and the existing test's c_char import in src/lib.rs |
| mupdf-rs | The wrapper must compile the patched nested MuPDF source. Update the submodule URL and gitlink together. | .gitmodules and mupdf-sys/mupdf |
| translator-rs | lopdf 0.40 aborts on a bounded deeply nested catalog. Upgrade to 0.42, retain the regression and consume the reviewed MuPDF/CLD2 commits. | root Cargo.toml/Cargo.lock, crates/translator-pdf/Cargo.toml and src/pdf.rs, crates/translator-translate/Cargo.toml |
| offline-translator | Wire both native consumers to the same reviewed engine; prevent stale dependencies and validate packaged native compatibility. Earlier app fixes also improve UI responsiveness, downloads and privacy. | both native Cargo.toml/Cargo.lock pairs, app/build.gradle.kts, tools/native-security-pins.json, security/compatibility guards and CI; see PR #12 for app changes |

MuPDF vendor patches are [611f75f0](https://github.com/DavidVentura/mupdf/commit/611f75f0c8657b92460554009196c5ac4b68d909) and [a08c5ee1](https://github.com/DavidVentura/mupdf/commit/a08c5ee1637580658184ab8b1860779ed2e9f73e). The lopdf finding is [RUSTSEC-2026-0187](https://rustsec.org/advisories/RUSTSEC-2026-0187.html). Existing vendor attribution and licenses are retained. CFF reachability from the app is not established; the report distinguishes a native defect from a demonstrated app exploit.

## How to adopt

1. Review and adopt the MuPDF and CLD2 patches in the maintainer-owned forks. These are independent.
2. Update the MuPDF wrapper's submodule to the accepted MuPDF commit. Keep the maintainer's submodule URL when that repository now contains the fix; the RioPlay URL exists to make this proof of concept reproducible.
3. In the engine, apply the lopdf update/regression and select the accepted wrapper and CLD2 commits. Replace temporary fork URLs with agreed maintainer repositories, retain full immutable revisions, regenerate Cargo.lock and inspect the diff.
4. In the Android app, update both bindings and model-converter manifests/locks to the same accepted engine commit. Update the four-repository record in tools/native-security-pins.json, including the three MuPDF source hashes if the accepted source differs.
5. Repeat the host tests and both Android jobs after repinning. Do not substitute the old fork's green run for validation of newly selected commits.
6. Complete signed-device acceptance before release. Keep release application ID, label, version policy and signing under the maintainer's control.

Do not apply the archived preparation patches on top of the published commits. Those older patches and tested-engine.lock document an earlier local test graph; the five current fork PRs are the implementation source of truth. Recheck upstream branch heads before applying anything.

## Reproduce the checks

Use Rust 1.88 and the repository's native prerequisites. The successful host run used CMake 3.31.10; Android CI uses the checked-in Dockerfile with NDK 28.0.13004108. Tests below are run from the engine checkout at the recorded commit:

```sh
cargo test --locked -p translator-pdf --lib --features image-translate
cargo test --locked -p translator-core -p translator-translate -p translator-doc -p translator-render --lib --features translator-doc/epub,translator-doc/odt,translator-translate/html
```

Expected recorded results: 72 PDF tests plus 244 other library tests, all passing. The second group covers core/catalog behavior, language detection/routing, rendering, HTML and EPUB/ODT document handling. These are host library checks; they do not establish model-backed Android translation or phone launch.

From the Android checkout, using its configured Docker toolchain, CI executes:

```sh
python3 -m unittest discover -s tools -p 'test_*.py'
python3 tools/check_native_security.py
./gradlew --stacktrace :app:testDebugUnitTest :app:ktlintCheck :app:assembleRelease -PtargetAbi=arm64-v8a
python3 tools/check_apk_native.py app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk --abi arm64-v8a
python3 tools/check_native_security.py --abi arm64-v8a --apk app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk --report app/build/reports/security/native-arm64-v8a.json
```

Repeat the Gradle and APK checks with armeabi-v7a. The workflow supplies a separate clean job for each ABI. The report binds reviewed source hashes and lock hashes to the APK SHA-256. It is build evidence, not a cryptographic third-party attestation.

The bounded native controls are preserved in run_cff_probe.py and run_image_probe.py with their C harnesses and JSON results. Each runner's --help describes baseline/patched inputs. CFF uses ASan/UBSan; LeakSanitizer was disabled because this host denies the required /proc task access. Valid controls pass alongside malformed-input rejection. Earlier CLD2 validation passed 26 tests and verified the unchanged 24 C++ source files plus wrapper.

## Evidence and interpretation

- [Android run record](android-ci-validation.json): both jobs succeeded for d77b0062a440e7abc1036ef8b5e7ac39f76afa79; includes unsigned artifact links and archive digests. Uploaded native reports carry APK digests.
- [Additional host regression record](regression-validation.json): 244 passing tests, command, source identity and log hash.
- [Published-source PDF validation](published-source-validation.json): 72 locked tests against published dependencies.
- [Fresh app lock scan](integrated-app-osv-review.json): 480 queries and remaining advisory matches.
- [Native feature review](compiled-feature-review.md): narrowly scoped compiled-feature findings.

The first ARM64 integration run failed at packageRelease without an underlying exception in the log. A clean run with stack traces and disk reporting added passed without changing application/native code. The original cause is undetermined; this handoff does not present the diagnostics change as a diagnosed bug fix. ARMv7 passed both runs.

## Compatibility and release limits

No new wrapper runtime API is introduced. The app retains its generic label, release package ID and minimum API. Its backup policy intentionally changes: app data is excluded from cloud backup, Android 6–11 legacy backup/restore is disabled, and Android 12+ device migration remains subject to OS policy. Ordinary updates and shared/exported files are separate from backup behavior.

The earlier app work spans UI, downloads, logs, backup and build tooling. Review the Android PR's commit groups separately: 0d5671d (cumulative app fixes), 5fe6634 (backup/build hardening), fdd72fd (native security integration), d77b006 (CI diagnostics). The user reported the older test6 UI and downloads worked well; that is not evidence for the new native revision. No measured battery/frame-rate claim is made.

Remaining: ZIP 2.4.2 extraction advisories, maintenance warnings, incomplete native/C++ and native-log coverage, full-lint debt, and representative-device testing. Reviewed ZIP callers read entries rather than extract files; the dependency is still affected and not suppressed.

Use [device-testing.md](https://github.com/RioPlay/offline-translator/blob/upstream/consolidated-fixes/docs/device-testing.md) for fresh install, same-signer retained-data update, real-model PDF import/translation/export, offline behavior, optional-permission denial, minimum API, ARMv7 and 16 KB testing. CI artifacts are unsigned; the previous test6 signer is unavailable. Never uninstall or clear the existing app merely to make an update test pass.

No upstream repository has been modified. RioPlay's explicit approval is required before submission; approval to submit does not authorize a merge or release.
