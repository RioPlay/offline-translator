# Host validation and Pixel follow-up

> **Candidate withdrawn — 2026-10-25:** Do not install the APK with SHA-256 `434cc7caa33763cfdc7affc0e28bfc2d6360f46c38c4a369e22c1d590e6f31b9`. The user reported a startup failure. Bytecode inspection confirmed that all six generated UniFFI loaders default to `bindings-metadata`, while the APK only packages `libbindings.so`. The metadata-only extraction filename leaked into the generated library name. The host checks below did not cover Android startup. The correction is being validated on branch [fix/native-library-startup-contract](https://github.com/RioPlay/offline-translator/tree/fix/native-library-startup-contract). No working-device verdict is implied by these historical checks.

Session date: 2026-10-24. Tested source: `8f879f99f604e73bfc3f3faa44642b7cec1ed2fa`, tree `5d5515aadedf6893121d419a5826906236052bae`, from `integration/upstream-sync`. This report does not change application code. Structured findings and provenance are in [the accompanying JSON](host-gauntlet-2026-10-24.json).

## Result

The existing JVM suite and repeated download/dictionary tests pass. Debug and deviceTest compilation, formatting, strict hardcoded-string checks, release fatal lint, R8 minification and ARM64 APK packaging pass. **Full Android lint fails** and needs separate triage. No Android device was connected. These results are not a clean full-lint verdict, a fresh native build, a complete security audit, or measured battery/frame-time evidence.

| Check | Observed result |
| --- | --- |
| Existing JVM suite | 103 tests; zero failures, errors or skipped tests |
| Download and dictionary stress | 17 distinct tests repeated 20 times: 340 additional executions, zero failures, 12.025 seconds |
| App-wide ktlint | Pass |
| Strict hardcoded UI strings | Pass |
| General detekt | 68 advisory findings; the repository config allows these without failing the task |
| Full deviceTest Android lint | Fail: 25 errors, 148 warnings, 7 hints |
| NewApi in full lint | Zero findings at minSdk 23 |
| Release fatal lint and minified assembly | Pass through the normal assembleDeviceTest gate |
| Catalog JSON and localized strings | JSON parses; 33 strings.xml files parse with unique resource names |
| Git whitespace | Pass before report creation |
| Pixel/runtime/accessibility/performance | Not run |

### JVM coverage

| Suite | Tests | Failures |
| --- | --- | --- |
| InputSplitTest | 2 | 0 |
| MainSelectionRulesTest | 28 | 0 |
| DictionarySessionTest | 4 | 0 |
| ExampleUnitTest | 1 | 0 |
| SessionTransitionsTest | 8 | 0 |
| TargetTabsTest | 10 | 0 |
| LayoutDensityTest | 4 | 0 |
| OverlayLanguageSelectionTest | 22 | 0 |
| PackDownloadTest | 13 | 0 |
| ReadonlyPopupSizeTest | 6 | 0 |
| SharedTextTest | 5 | 0 |

The download tests exercise real loopback HTTP framing, truncation, HTTP failure, stalled headers/body, chunked and connection-close bodies, compressed-byte accounting, corrupt/truncated gzip, and cancellation before publishing a file. Dictionary tests cover UI responsiveness during blocking work, obsolete results, dismissal/back and owner disposal. Repetition is additional execution of existing tests, not 340 newly distinct cases. JVM HTTP behavior does not establish Android HttpURLConnection behavior.

## Android lint triage

| Error category | Count | Next verification |
| --- | --- | --- |
| MissingPermission | 4 | Review notification permission handling in document/download/API services and tap notification; test denial and revocation on a phone. Lint alone does not prove a crash. |
| UnsafeOptInUsageError | 17 | Review Camera2 interop usage and acknowledge the intended experimental API contract at the proper scope. |
| AppCompatCustomView | 2 | Review text/edit view theme and compatibility requirements before changing their base classes. |
| StartActivityAndCollapseDeprecated | 1 | Existing quick-tile code already uses PendingIntent on API 34+ and the Intent overload below 34; review this guarded compatibility branch before applying a lint-specific change. |
| StateFlowValueCalledInComposition | 1 | Review reactive catalog collection in DocAlignScreen and test a catalog update after screen composition. |

All nine files containing the 25 errors are byte-for-byte identical to upstream `bd675fecea875d4e5b3c0f62e24b2a9c3c84f6da`. All flagged statements are present there. This establishes source provenance, not that the exact upstream toolchain would produce identical diagnostics; a standalone upstream full-lint run was not performed. Do not label these as new fork regressions or dismiss them as harmless without checking the individual behavior.

General detekt includes a nesting advisory in the new downloader and a loop-jump advisory in its test fixture. The full advisory list is preserved in JSON. No lint baseline, suppression or production-code change was added by this validation pass.

## Native and build provenance

Workspace cleanup had removed the prior checkout, installed SDK and generated outputs. The source and required tools were restored. JDK 17.0.20 and Gradle 8.14.6 were used; the downloaded Gradle archive matched the wrapper checksum. Android platform 34 and Build Tools 35.0.0 were restored.

The native bindings/converter source trees and lockfiles exactly match the upstream base. This run reuses the verified upstream ARM64 artifact from run 40113838618, artifact 12568147153, archive SHA-256 `ab8c420ccdc2167960a7aea092d835ed8f034c7abb52c2fca4e7f29f03bd0336`. It is **not a fresh C++/Rust Android build**. Host UniFFI 0.31.2 was rebuilt and Kotlin bindings regenerated from the matching library metadata. A scratch metadata-only copy exposes the existing dynamic symbols to the reader; the original packaged native bytes are unchanged. See [the upstream integration review](upstream-sync.md) for this method's boundary.

The initial combined Gradle invocation lost its daemon while full lint and shrinking overlapped. Machine OOM kills were observed, consistent with memory pressure; precise process attribution was not established. The completed unit, stress and formatting results were retained. Full lint and assembly were rerun separately with one worker. Full lint then produced the findings above; assembly passed independently. No failed check was converted into a pass by adding a baseline or disabling its findings.

The local validation excludes only `buildBindingsAarch64`, `buildModelConverterAarch64`, `buildUniffiBindgen` and `generateUniffiBindings`, using the prepared matching outputs described above. Native stripping reports that existing libraries are packaged as-is. All six expected native libraries are present, and the three inherited native outputs are byte-identical to the verified artifact. A normal clean native build remains a separate outstanding check. No standalone native core test rerun is claimed by this report.

The prepared lopdf update is still separate and unshipped. Passing this gauntlet does not resolve that known security work.

## Device candidate

- File: `offline-translator-upstream-sync-8f879f9-arm64.apk`; 45,035,005 bytes.
- SHA-256: `434cc7caa33763cfdc7affc0e28bfc2d6360f46c38c4a369e22c1d590e6f31b9`.
- Source commit: `8f879f99f604e73bfc3f3faa44642b7cec1ed2fa`.
- Launcher name: Translator. Package: `dev.davidv.translator.devicetest`. ARM64 only, minSdk 23, targetSdk 34, non-debuggable and minified.
- Version remains 0.8.6-test3 / code 332, as defined by the tested source. Identify this candidate by source commit and hash rather than version alone.
- Signature verification passes. Public test certificate SHA-256: `62b6342f7528a42e8db321c8ee5c3339d944dea50bf2b739f20736bbdb0f52d7`.
- This rebuild uses a fresh development key. Android may reject an update over a previously signed test copy. Reinstalling that test copy removes its private data; preserve anything needed first. Do not uninstall the regular app. No private key is published.
- Catalog v7 in the APK exactly matches source; the v6 asset is absent. ZIP entries are unique and all expected native libraries are included.

## Pixel 8 Pro follow-up — not yet executed

1. Install the isolated candidate, launch it and download a language pair. Cancel a download, interrupt connectivity, then retry. Confirm incomplete packs are not shown as installed and the completed pack translates successfully. Repeat with notifications denied.
2. With packs installed, disable the app's network access and translate fictional phrases in both directions. This checks text translation with installed packs. External microphone recognition and keyboard dictation follow their providers' policies and require separate testing.
3. Search and scroll the language list, perform rapid dictionary taps, dismiss a pending lookup and press Back. Confirm obsolete results do not reopen or replace the current sheet.
4. Use language swap with the keyboard visible, in portrait and landscape. Current swap changes the pair and retranslates the existing input; separate pane movement/back-translation controls are not implemented.
5. Check Settings at large font size and with TalkBack. Verify local/download/website/external-speech descriptions and API enabled-versus-running status are understandable. Leave network-facing API exposure disabled unless deliberately testing it.
6. Test image/document translation, voice selection/defaults and a catalog refresh. Review notification denial/revocation and document-alignment catalog updates against the lint findings.

After smoke tests pass, collect app-scoped frame timing for a fixed interaction sequence on the Pixel and compare controlled builds. Battery claims need a repeatable device workload and idle baseline. No responsiveness or battery improvement is claimed from host tests alone.

The existing instrumentation setup also references three absent English-to-Spanish assets: model.enes.intgemm.alphas.bin, vocab.enes.spm and lex.50.50.enes.s2t.bin. They are absent in both this checkout and the synced upstream tree. Those fixtures must be repaired before claiming the affected device tests pass. Manual phone smoke testing remains possible.

All work stays in the owned fork. This validation does not authorize a master merge, release or upstream submission.
