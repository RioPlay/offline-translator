# Focused security update, 24 October 2026

This candidate updates affected build tooling and the host binding generator. It does not claim that every advisory match has been fixed or that a version match establishes an exploitable app vulnerability. The review baseline is device-test commit `3424e6974174850deab4e193246699e7d7ecd129`.

## Implemented in the app repository

| Component | Before | Proposed | Reason |
| --- | --- | --- | --- |
| Gradle wrapper | 8.9 | 8.14.6 | Patched 8.x line for repository fallback and daemon/worker advisory families, including CVE-2026-22816 and CVE-2026-22865. |
| Kotlin build/Compose plugins | 2.0.0 | 2.4.21 | Includes CVE-2026-53914's KAPT deserialization fix. KAPT is not applied by the reviewed build scripts, so the vulnerable execution path was not established here. |
| Android Gradle Plugin | 8.7.3 | 8.13.2 | Keep the Kotlin upgrade on a supported AGP 8.x combination. |
| R8 | AGP bundled | 9.1.57 | Kotlin 2.4 requires at least R8 9.1.29. Use Google's documented buildscript override and an actually published 9.1 patch. This is a compatibility prerequisite, not a separately asserted CVE fix. |
| Android build tools | 34.0.0 | 35.0.0 | AGP 8.13.2's minimum; align the app, Docker SDK setup and test verification. The platform/target SDK remains 34. |
| Android API backports | disabled | desugar_jdk_libs 2.1.5 | Preserve API 23 support when the upgraded compiler selects Java collection methods; use iterator loops for ConcurrentLinkedQueue, whose forEach is not covered by the default backport. |
| Host bindgen `anyhow` | 1.0.102 | 1.0.104 | RUSTSEC-2026-0190 is fixed in 1.0.103. The app's runtime lock already selected 1.0.104. No affected `Error::downcast_mut` use was found in reviewed application source. |

Regenerate the wrapper scripts and JAR, and pin the official distribution SHA-256. The generated wrapper JAR matches the publisher's checksum `ad16bca46cb71bb5887c546860d93992aeee35c1fdaf4a242b0a612375d937e0`. Track the converter and host bindgen Cargo.lock files as Gradle inputs so a security lockfile edit cannot silently reuse an old output. Declare ordering between generated bindings and style checks when compilation and lint run together.

## Resolved dependency evidence and remaining work

`tools/dependency-snapshot.init.gradle` records selected `deviceTestRuntimeClasspath` modules, the API desugaring configuration and the root build plugin classpath, plus the R8 class actually loaded by AGP. This avoids treating old downloaded JARs as current dependencies. The report resolves Gradle 8.14.6, Kotlin 2.4.21, AGP 8.13.2 and effective R8 9.1.57. The selected Maven inventory and OSV query results accompany this review.

The 300 selected/explicit Maven package-version queries produced no advisory matches in the selected app runtime modules or API backport configuration, but did produce these build-classpath matches. This statement excludes native Cargo dependencies and is not a general security guarantee.

| Remaining build dependency | Selected version | Handling |
| --- | --- | --- |
| Netty modules | 4.1.110.Final | Matches remain in AGP's build classpath, including HTTP/HTTP2, codec, handler and common. Determine which remote-cache, test or networking operations use the affected APIs before a coordinated build-only upgrade. |
| Commons Compress | 1.21 | Archive-handling advisory matches remain; identify the build tool's callers and input trust. |
| jose4j | 0.9.5 | Build-tool advisory match remains; affected JOSE token handling has not been established in this app build. |
| Bouncy Castle bcprov/bcpkix | 1.79 | Newer advisory matches remain after the AGP update; review the actual signing/crypto paths and compatible aligned upgrades. |
| JDOM2 | 2.0.6 | XML advisory match remains; inspect build-tool parser configuration and input source. |

These are selected dependencies, not dismissed cache artifacts. They are not present in the selected app runtime graph. This patch does not blindly force unrelated transitive libraries through AGP's classloader, and does not label the build classpath CVE-free. Full IDs and versions are in `selected-maven-scan-2026-10-24.json`.

Other native matches remain tracked in the earlier dependency review. The reviewed ZIP callers use entry reads/rewrites rather than extraction, so the extraction symlink/mode advisories have no established call path there. Legacy regex/thread_local/rustc-serialize arrive through CLD2 build dependencies, requiring build-input/pattern reachability analysis. Maintenance notices are not automatically CVEs.

## Prepared PDF fix: not included in this APK

The app pins `DavidVentura/translator-rs` at `94f47bff9d80bcd4e2779bceb037a5ed2c330fab`. It selects lopdf 0.40.0, affected by RUSTSEC-2026-0187. `docs/patches/translator-rs-lopdf-0.42.patch` updates the two native workspace manifests to lopdf 0.42 and adds a generated nested-PDF parser regression case. It applies to that pinned native revision, not the unrelated latest native API/catalog changes.

A bounded host probe using the exact old and fixed parser versions accepted a valid control PDF in both versions. A roughly 21 KiB PDF with 10,380 nested arrays aborted lopdf 0.40.0 with stack overflow; lopdf 0.42.0 accepted it without a crash within the same time/memory bounds. Acceptance is allowed: the security property tested is bounded parsing without process stack overflow. The Android app first normalizes PDFs through MuPDF; survival of this input through that full pipeline has not been demonstrated.

Publication/integration requires a writable `RioPlay/translator-rs` fork, a commit based on the pinned revision, app manifest/lockfile repinning, and final native/Android validation. The GitHub connector cannot create that fork. The prepared patch is reviewable here, but the current app dependencies are deliberately not repinned to an unpublished local path. No PDF security fix is claimed in the candidate APK until this integration is complete.

## Validation status

Wrapper generation, publisher checksum verification, Gradle configuration and selected dependency resolution passed. Both Android Kotlin compilation and all 90 JVM tests pass under the updated toolchain, including the API backport configuration. The full app ktlint check passes. Keep release NewApi lint enabled with minSdk 23: final release lint and minified APK packaging remain pending at this checkpoint. Device keyboard placement/focus, translation, dictionary history, pack installation/removal, TTS and document handling still need testing; no battery or frame-time measurement is claimed.

The bounded old/fixed native parser probe passed. The exact prepared native patch applies cleanly to the pinned native revision, and formatting of translator-pdf passes. Full native ARM64 compatibility remains a separate validation gate before app repinning; the standalone regression case also needs execution under the normal Rust test harness.

All changes are drafts in RioPlay's fork. Nothing may be submitted to an upstream-maintained repository without RioPlay's explicit approval.

## Primary references

- https://github.com/gradle/gradle/security/advisories
- https://services.gradle.org/distributions/gradle-8.14.6-bin.zip.sha256
- https://services.gradle.org/distributions/gradle-8.14.6-wrapper.jar.sha256
- https://github.com/JetBrains/kotlin/security/advisories/GHSA-r937-wjx7-w2jp
- https://kotlinlang.org/docs/gradle-configure-project.html
- https://developer.android.com/build/kotlin-support
- https://r8.googlesource.com/r8/+/refs/heads/main/README.md
- https://rustsec.org/advisories/RUSTSEC-2026-0187.html
- https://rustsec.org/advisories/RUSTSEC-2026-0190.html

- https://developer.android.com/studio/write/java8-support
- https://developer.android.com/studio/write/java11-default-support-table
- https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md
