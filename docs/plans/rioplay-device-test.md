# RioPlay device test — candidate 1

This branch combines the bounded logging fix from RioPlay/offline-translator#1 and the language-filter cache from RioPlay/offline-translator#2. It is for RioPlay's device testing only. It does not reconstruct the earlier bilingual APK, fix initial screen/drawer stalls, or establish battery savings.

Nothing may be submitted to DavidVentura/offline-translator without RioPlay's explicit approval, after their device test. The separate code PRs remain draft.

## Build and installation

The branch push starts `RioPlay device test` in GitHub Actions if workflows are enabled for the fork. No package registry upload, release publication or release-signing secret is used. The existing Dockerfile supplies the build toolchain; this first build can take time. ARM64 targets RioPlay's Pixel 8 Pro.

The artifact contains `Translator-RioPlay-Test-arm64.apk`, SHA-256, certificate details, packaged app metadata and the exact source commit. The workflow verifies the signature, application ID, ABI and non-debuggable flag before uploading. Compilation and those packaging gates must actually pass; source inspection is not a substitute.

The `deviceTest` build type inherits release shrinking and behavior, uses an automatically generated development signing key, and enables shell profiling. It is not a production release. Each clean runner may generate a different test certificate, so a later candidate may require uninstalling **only the RioPlay test app**. Do not uninstall the original app to resolve a signature mismatch.

Expected application ID: `dev.davidv.translator.rioplaytest`. Launcher name: `Translator — RioPlay Test`. The separate app identity keeps internal files/settings and app-specific external files separate. Public model storage uses the runtime package name, so the test app uses its own folder as well. No model/settings migration is attempted; install test packs separately. The static camera shortcut is overridden to target the test package. Public translation intent actions are preserved, so Android may offer both apps as handlers. Explicit clients targeting the official package still target the original app.

## Small first test

1. Keep the current app installed. Install and open the distinctly named test app. Confirm it launches without a crash.
2. Download one source/target language pair. Confirm completion, translate a short fictional sentence in both directions, restart the app and repeat offline.
3. On the language-management screen, type and clear several filters; confirm results and ordering stay correct, including after installing/deleting a test pack. Compare the same installed models, query, locale and device state against the original app. Filtering is the only optimization in this candidate; opening/drawer lag remains a separate report.
4. Check dictionary lookup and share/PROCESS_TEXT results with fictional unique text. If capturing logs, check that those explicit inputs/results no longer appear. This requires a real log inspection, not merely seeing translation succeed. Other diagnostics may still contain private content.
5. Confirm the original app still opens with its original settings and packs, and that the test camera shortcut opens the test app.

Record the APK hash, device/OS, language packs, which checks passed, failures and reproduction steps. No full raw logs or real translation text need to be posted publicly. A successful smoke test permits further review; it does not establish complete privacy, measured speed improvements or upstream approval.

## Pending gates

- GitHub build and APK packaging/signature checks.
- Installation and functional checks on RioPlay's device.
- Debug and release log checks for the independent logging PR.
- Same-device performance measurements and broader regressions before changing the main screen/drawers.
- RioPlay approval before upstream contact or submission.
