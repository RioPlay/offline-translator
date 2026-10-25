# Security and compatibility review

Reviewed 2026-10-25. This is a scoped review, not a vulnerability-free claim or
a substitute for installed-device testing. See [device-testing.md](device-testing.md)
for the supported-device acceptance matrix and signing/update requirements.

## Changes in this pass

- Constrain the selected build classpath to Netty 4.1.139.Final (aligned BOM),
  Commons Compress 1.28.0, jose4j 0.9.7, Bouncy Castle 1.86 and JDOM 2.0.6.1.
  These are build-tool dependencies, not new on-device networking libraries.
- Exclude all documented app-data domains from Android cloud backup. Android
  6–11 legacy backup/restore is disabled; Android 12+ user-initiated device
  migration remains enabled under OS policy. Ordinary package updates retain
  data. Public exports/shared storage and existing remote backups are outside
  this change. Settings explains these distinctions.
- Remove reviewed image request-size/ID, screen language/geometry/native-pointer
  diagnostics and per-overlay presentation timing logs. No measured battery or
  frame-rate claim is made. Native/system logging is not comprehensively audited.
- Check packaged ABI/library presence and 16 KB alignment in CI on both ARM
  builds. Cancel superseded CI runs only for the same workflow/PR/ref.
- Integrate the reviewed native PDF and CLD2 patches through immutable commits
  in the user-owned forks. Both app Cargo locks use the same engine revision.
  Require `--locked` for native and host-bindgen builds. CI checks the dependency
  pins before building and the three patched MuPDF source hashes afterward,
  saving those checks with each APK's SHA-256. Exact pins are recorded in
  `tools/native-security-pins.json`.

Backup policy follows [Android Auto Backup documentation](https://developer.android.com/identity/data/autobackup).
XML regression checks establish configuration, not OS/OEM backup behavior.

## Dependency findings and remaining work

The selected Gradle runtime, desugaring and plugin configurations plus crates.io
packages in the three app Cargo locks were queried against OSV on 2026-10-25
(786 unique ecosystem/name/version tuples). Selected Maven configurations and
the converter lock had zero advisory matches after the build-tool constraints.
This excludes git/C++ dependencies, Gradle distribution internals, unselected
configurations and unknown vulnerabilities. It is not a full APK security audit.

| Finding | Status and next verification |
| --- | --- |
| lopdf, RUSTSEC-2026-0187 | Source integrated: both app/engine locks now select 0.42.0. The old 0.40 parser aborts on the bounded 21 KB regression; the fixed parser succeeds. The published native source passes 72 locked host PDF tests with image translation enabled. Android CI and actual device PDF workflows are separate acceptance gates; test6 remains the older build. |
| MuPDF, CVE-2026-7233 and CVE-2026-3308 | Source integrated: backport missing vendor commits 611f75f0 and a08c5ee1, then pin the wrapper's submodule. The version header alone was insufficient: the previous source predates these fixes. Actual CFF ASan/UBSan checks reproduce an old heap-buffer overflow and verify fixed rejection with a valid control. Native image-stream checks reject three invalid argument cases while retaining the valid control. CFF app entry-point reachability is not established. CI requires exact patched C source hashes in each ARM build. |
| regex 0.1.80, rustc-serialize 0.3.25, thread_local 0.2.7 | Removed from the resolved app graph through the CLD2 build-script cleanup. Its fixed filename regex becomes suffix matching and its repository-manifest parser uses TOML 0.9. All 26 isolated CLD2 tests pass; the selected 24 C++ sources plus wrapper are unchanged. This is build-dependency remediation, not an established translation-input exploit path. |
| zip 2.4.2 extraction advisories RUSTSEC-2026-0367/0368/0369 | Open dependency remediation. Reviewed EPUB/ODT and piper-rs Kokoro -> ndarray-npy 0.9.1 callers read entries with by_name/by_index rather than extracting to the filesystem. Upgrade requires coordinated document and voice-model dependency changes; no suppression or universal-unreachability claim. |
| Maintenance advisories | buf_redux, multipart, paste, rustybuzz, safemem, ttf-parser, twoway and host bindgen basic-toml remain flagged. These are maintenance warnings, not evidence by themselves of an exploitable app flaw. |
| Runtime/privacy coverage | Device launch/update retention, backup transport behavior, native/system diagnostic review, full native/C++ dependency coverage and the cross-device matrix remain open. Full Android lint has pre-existing debt; passing fatal/API lint does not mean full lint is clean. |

The PDF finding is documented in [RustSec RUSTSEC-2026-0187](https://rustsec.org/advisories/RUSTSEC-2026-0187.html).
Do not treat host tests as proof that the Android PDF pipeline is fixed on every
device. The app now pins the published security branch; Android artifact and
device verification remain required. Source review and reproducible host probes
are preserved in the fork's [native security checkpoint](https://github.com/RioPlay/offline-translator/tree/security/build-tools-and-pdf-review/docs/reviews/native-security-2026-10-25).
Native dependency draft PRs: [engine](https://github.com/RioPlay/translator-rs/pull/1),
[wrapper](https://github.com/RioPlay/mupdf-rs/pull/1),
[MuPDF](https://github.com/RioPlay/mupdf/pull/1), and
[CLD2](https://github.com/RioPlay/rust-cld2/pull/1).
No upstream submission is authorized by this integration.

To regenerate the selected Gradle inventory using the configured toolchain:

```sh
./gradlew -I tools/dependency-snapshot.init.gradle :app:dependencySecuritySnapshot
```

The JSON is written to `app/build/reports/security/selected-dependencies.json`.
Query the exact selected versions against a current advisory database; scanning
every cached JAR can report obsolete versions that the build no longer uses.
Retain timestamps, scope and native-lock findings with the review.
