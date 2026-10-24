# Dependency security review — 2026-10-24

## Scope and evidence

Reviewed app source commit `3424e6974174850deab4e193246699e7d7ecd129` (the fork-only device candidate), its three Cargo lockfiles, declared Maven versions and native source `DavidVentura/translator-rs@94f47bff9d80bcd4e2779bceb037a5ed2c330fab`. OSV batch queries checked 511 distinct package/version pairs: crates.io lock entries plus declared Maven dependencies and build plugins. Fifteen packages matched 21 advisory records. Counts include alias duplicates, unmaintained notices and unsoundness reports; they are not a count of demonstrated app exploits.

A second snapshot checked 146 downloaded Maven JAR/AAR versions during build-tool resolution and matched 14 packages, overlapping the first scan. This is not a resolved APK runtime SBOM. Full selected Gradle runtime/build classpaths, all native vendored C/C++ dependencies, the base-image OS, platform libraries and exact APK dependency inclusion remain outside the completed scan.

The ARM64 Cargo metadata graph resolved 360 packages. Source inspection is used below to distinguish build dependencies from app code. No exploit was run against the APK. No evidence of exploitation was found or claimed.

## Priorities

| Priority | Component and pinned version | Evidence and relevance | Next action |
| --- | --- | --- | --- |
| High investigation/upgrade priority, app document path | `lopdf 0.40.0` | RUSTSEC-2026-0187 / GHSA-6fj4-rcr3-r7rr reports stack exhaustion from deeply nested PDFs, patched in 0.42.0. Native `translator-pdf/src/pdf_write.rs` and `pdf_image_translate.rs` call `Document::load_mem` on document bytes in enabled app features. However, `src/document.rs` first rewrites the input through `NormalizedPdf`/MuPDF. Whether malicious nesting survives that preprocessing has not been tested; a crash in the Android app is not established. | Upgrade the native PDF dependency to a compatible patched version, rebuild the pinned native library, and test malformed/deep PDFs through the complete preprocessing path plus valid-document regressions. Basic text-translation device testing can proceed separately. |
| High, build integrity | Gradle wrapper `8.9` | Maintainer advisories GHSA-w78c-w6vf-rw82 (CVE-2026-22816) and GHSA-mqwm-5m85-gmcv (CVE-2026-22865) include this version. Additional October advisories GHSA-j5m7-59rp-24f5, GHSA-xwqc-3h47-hg64 and GHSA-mvvg-497x-hmj8 are fixed in the 8.x line by 8.14.6. They concern repository fallback or same-host daemon/worker channels, not an on-phone Gradle service. | Test Gradle 8.14.6 with the existing project, add the official distribution SHA-256, then update the wrapper in a separate change. The local build is attempting this version; a completed APK build is still required. Ephemeral isolated CI runners mitigate the same-host attack scenario but do not replace the upgrade. |
| Medium, conditional build cache | Kotlin Gradle plugin `2.0.0` | CVE-2026-53914 / GHSA-r937-wjx7-w2jp matches the version. The referenced JetBrains fix specifically filters KAPT incremental-cache deserialization. No KAPT plugin/application was found in the reviewed project build scripts. The version match is confirmed; an active affected KAPT task is not established here. | Plan a compatible stable Kotlin/Compose/compiler toolchain update using vendor compatibility tables. Avoid importing shared KAPT/project build-cache state in the meantime. Do not force a beta or ignore R8 compatibility merely to remove a scanner alert. |
| Medium, host generator | `anyhow 1.0.102` in `app/bindings-bindgen/Cargo.lock` | RUSTSEC-2026-0190 describes unsound `Error::downcast_mut` behavior with added context, corrected in 1.0.103. The app's main native lockfile already resolves 1.0.104. No affected downcast call was found in the inspected app/native source; transitive host code was not exhaustively traced. | Update the host-generator lockfile to a compatible patched release and regenerate bindings/build. Keep this distinct from a runtime app memory-corruption claim. |
| Investigate applicability | `zip 2.4.2` | OSV reports broad affected ranges for RUSTSEC-2026-0367, -0368 and -0369. The symlink advisory describes behavior tested on 9.0.0-pre2. Reviewed ODT/EPUB code uses in-memory `ZipArchive::by_index` and rewrites archives; no `ZipArchive::extract` or `ZipStreamReader::extract` calls were found in the inspected native source. | Verify the specific 2.4.2 implementation and all callers, including transitive `ndarray-npy`, before assigning an app escape/permission vulnerability. A jump to zip 9 is a major compatibility change and is not an automatic safe remediation. |
| Lower immediate app exposure, build dependencies | `regex 0.1.80`, `thread_local 0.2.7`, `rustc-serialize 0.3.25` | CVE-2022-24713 / RUSTSEC-2022-0013, RUSTSEC-2022-0006 and RUSTSEC-2022-0004 match these versions. They enter through `cld2-sys` build dependencies (`regex` and legacy `toml`); this is not the current runtime regex 1.13.1. Regex advisory exploitation requires untrusted patterns. | Modernize the CLD2 build dependency chain with regression checks. Do not describe these legacy host dependencies as proven remote translation-input vulnerabilities. |
| Conditional HTTP API maintenance | `multipart 0.18.0`, `buf_redux 0.8.4`, `safemem 0.3.3`, `twoway 0.1.8` | RustSec maintenance/deprecation notices. The HTTP implementation calls Rouille multipart parsing; API defaults and exposure are documented in the privacy review. Maintenance notices do not by themselves prove an exploit. | Replace or maintain the old HTTP/multipart chain before relying on LAN exposure; test request limits and compatible clients. |
| Maintenance | `paste 1.0.15`, `basic-toml 0.1.10`, `rustybuzz 0.20.1`, `ttf-parser 0.25.1`, plus legacy `rustc-serialize` | Published maintenance/deprecation notices, with a separate vulnerability record for rustc-serialize above. | Track replacement options and ownership. Avoid unrelated major migrations before a working baseline. |

## Additional build-tool matches

The downloaded Maven snapshot matched Bouncy Castle 1.77 (`bcprov` and `bcpkix`), Commons Compress 1.21, JDOM2 2.0.6, several Netty modules 4.1.93.Final, jose4j 0.9.5, protobuf-java 3.22.3 and Commons IO 2.13.0, as well as Kotlin. Their exact advisory IDs are retained in the scan summary. These were observed during build resolution; APK inclusion, vulnerable functions and configuration are not established. Prefer upgrading the owning Android/build plugin and testing it over arbitrary global dependency substitutions that can break the compiler, signing or packaging tools.

## Validation and release gates

The original toolchain produced the fork's signed ARM64 test APK in run 40110607784. APK SHA-256: `d507cb98cd8d784ef3212430e09cffb2e548428d775c2b20b0448564561afbdb`. Hash, signature, original `Translator` label, `.devicetest` package, ARM64 ABI and non-debuggable flag were verified again locally. This does not resolve the dependency findings above or constitute a device test.

The separate local environment has JDK 17.0.20, Rust 1.88.0, cargo-ndk 4.1.2, Android platform/build tools 34, NDK 28.0.13004108, CMake 3.28.3 and Ninja 1.11.1. An ARM64 compiler smoke test passed. Gradle 8.14.6 was downloaded with its official SHA-256, and the local full app build reached native compilation. Local APK completion and device validation remain pending.

Keep small upgrade PRs independent. For each, record the selected dependency graph and advisories before/after, compile and package, preserve valid text/document behavior, and test the affected failure path. Never suppress all advisories or equate version age with a CVE.

## Sources

- https://api.osv.dev/v1/querybatch and https://api.osv.dev/v1/vulns/ADVISORY_ID — exact-version queries; summary accompanies this review.
- https://rustsec.org/advisories/RUSTSEC-2026-0187.html
- https://rustsec.org/advisories/RUSTSEC-2026-0190.html
- https://rustsec.org/advisories/RUSTSEC-2026-0368.html
- https://rustsec.org/advisories/RUSTSEC-2022-0013.html
- https://github.com/gradle/gradle/security/advisories
- https://github.com/JetBrains/kotlin/commit/bf51df665b458fda7c3eaf436c4d88dc119d7ec6
- https://cveawg.mitre.org/api/cve/CVE-2026-53914 — JetBrains CNA record.
- https://kotlinlang.org/docs/gradle-configure-project.html
- https://developer.android.com/build/kotlin-support

All work and proposed changes remain in RioPlay's fork. Every upstream mutation or submission requires RioPlay's explicit approval, with device testing first.
