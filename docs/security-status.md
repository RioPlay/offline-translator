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
| lopdf 0.40.0, RUSTSEC-2026-0187 | Open. A synthetic 21,000-byte PDF aborts 0.40.0 with stack overflow; 0.42.0 parses it in an isolated host test. A native-engine manifest/lock/regression patch is prepared against bd32a4f. It is not integrated into this app's native pin or APK. Publish reviewed engine source, update app lock, build both ARM targets and test real PDF workflows before closing. |
| regex 0.1.80, rustc-serialize 0.3.25, thread_local 0.2.7 | Open build-dependency debt through cld2-sys 1.0.2. Its inspected build script compiles a fixed filename regex and parses repository Cargo.toml; this is not an established translation-input exploit path. Modernize the CLD2 dependency chain and validate language detection/native builds rather than suppressing the findings. |
| zip 2.4.2 extraction advisories RUSTSEC-2026-0367/0368/0369 | Open reachability review. Reviewed EPUB/ODT code reads entries instead of calling filesystem extraction. Other transitive callers still require review; do not claim these advisories are universally unreachable. |
| Maintenance advisories | buf_redux, multipart, paste, rustybuzz, safemem, ttf-parser, twoway and host bindgen basic-toml remain flagged; rustc-serialize also has a maintenance advisory. These are maintenance warnings, not evidence by themselves of an exploitable app flaw. |
| Runtime/privacy coverage | Device launch/update retention, backup transport behavior, native/system diagnostic review, full native/C++ dependency coverage and the cross-device matrix remain open. Full Android lint has pre-existing debt; passing fatal/API lint does not mean full lint is clean. |

The PDF finding is documented in [RustSec RUSTSEC-2026-0187](https://rustsec.org/advisories/RUSTSEC-2026-0187.html).
Do not treat a parser-only host test as proof that the Android PDF pipeline is
fixed. The app keeps its current upstream native revision until that change is
available and validated end to end.

To regenerate the selected Gradle inventory using the configured toolchain:

```sh
./gradlew -I tools/dependency-snapshot.init.gradle :app:dependencySecuritySnapshot
```

The JSON is written to `app/build/reports/security/selected-dependencies.json`.
Query the exact selected versions against a current advisory database; scanning
every cached JAR can report obsolete versions that the build no longer uses.
Retain timestamps, scope and native-lock findings with the review.
