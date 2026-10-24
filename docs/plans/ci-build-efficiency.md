# CI efficiency: measure one cache change first

## Measured baseline

Fork candidate `3424e6974174850deab4e193246699e7d7ecd129`, run https://github.com/RioPlay/offline-translator/actions/runs/40110607784 : image preparation 2m 6s; ARM64 assembly 25m 38s; packaging verification 1s; upload 2s. All steps passed. There is no measured warm-cache result yet.

The test workflow already builds one ABI, has read-only repository permissions, avoids publishing a container image/release, and cancels superseded builds. Preserve those properties and the APK verification/provenance checks.

## First experiment

Cache Rust downloads/checkouts and the three Cargo target directories: `app/src/main/bindings/target`, `app/src/main/model-converter/target` and `app/bindings-bindgen/target`. Keep consistent container checkout paths. The aim is to stop recompiling unchanged native dependencies when only Kotlin/UI changes.

Key the cache by OS/ABI, image/toolchain revision, Rust/NDK/compiler flags, Cargo manifests/lockfiles and native binding inputs. Initially use exact keys on the trusted fork-only push workflow; avoid broad restore prefixes and untrusted PR cache writers. Cargo must still check/rebuild changed inputs. Do not cache signing keys, credentials or user/model data. Cargo download integrity and dependency locks remain required.

Compare cold and warm runs of the same source and toolchain, including cache transfer time/size, native task output, full assembly time, APK checks and a smoke test. Then change one native source/lock input to verify invalidation. Cached executables/build scripts require a trustworthy cache producer. Record any benefit; do not claim a speedup before these runs.

## Later, if measurements justify it

1. Cache Docker toolchain layers using the official BuildKit/GitHub cache integration. Image preparation is secondary to assembly in the measured run. Pin and periodically review the base-image digest; pinning without updates would freeze security fixes.
2. Cache Gradle distribution and downloaded modules separately. Review checksum/dependency verification and the dependency security report first. Do not blanket-cache `.gradle`, `.kotlin`, KAPT state or project build outputs while assessing the Kotlin cache advisory.
3. Skip app compilation for documentation-only changes and run cheap checks first. Preserve full compilation for native, Kotlin, resource, build configuration and packaging changes. Keep the APK/device gate.
4. Pin reusable actions to reviewed full commit SHAs and keep them updated. Maintain least privilege and fork-only scope; do not add registry write permission just to save build time.

A compatible Gradle 8.14.6 wrapper update and host-generator dependency fixes are separate reviewable changes. Kotlin/AGP/R8 upgrades need an aligned compatibility check. Avoid changing compiler versions, caching strategy and app behavior simultaneously when measuring a performance improvement.

Sources: https://docs.github.com/en/actions/concepts/workflows-and-actions/dependency-caching , https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching , https://docs.docker.com/build/ci/github-actions/cache/ .

No workflow change in this plan has been deployed or benchmarked. No upstream repository may be changed or contacted without RioPlay's explicit approval.
