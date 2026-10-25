# Continue Offline Translator work

**Startup repair, 2026-10-25:** Continue from `fix/native-library-startup-contract`. Read `docs/reviews/startup-repair-2026-10-25.md` first. The upstream-sync APK with hash 434cc7caa33763cfdc7affc0e28bfc2d6360f46c38c4a369e22c1d590e6f31b9 is withdrawn: renamed metadata produced native loader names that did not match the packaged library. Source fbcc831a1be18c6716d82e17af8af5862d02610d pins and checks all six loaders. Test4/342 is built and checked at APK level, but Pixel launch and same-signer upgrade retention remain unverified; the local software emulator could not reliably install even the official baseline. Our test signer cannot replace the official signer. Do not clear the official app or claim a working official in-place update.

The combined baseline before this repair is on `integration/upstream-sync` in RioPlay/offline-translator. It incorporates the prior combined candidate and upstream master `bd675fecea875d4e5b3c0f62e24b2a9c3c84f6da`. Start with `docs/reviews/upstream-sync.md` for overlap resolutions, validation provenance and remaining checks. Earlier validation records describe their recorded revisions, not this combined build.

## Boundaries

- Work only in RioPlay's forks. Upstream reading is allowed; upstream submissions or other writes require explicit approval.
- Keep the generic launcher name Translator and the separate `dev.davidv.translator.devicetest` package for test builds.
- Keep PRs as drafts. This sync does not authorize a master merge, upstream submission or official release.
- Prefer small, validated changes. Do not delegate to agents unless explicitly requested.

## Current changes

The combined branch retains payload-log reductions, asynchronous dictionary lookup, cached catalog filtering, a keyboard-accessible language swap, clear processing/network Settings, API compatibility fixes and download reliability guards. Upstream contributes catalog v7, TTS upgrades, native engine updates and Android 8 image-selection compatibility. One voice-sorting merge conflict was resolved by retaining caching and using upstream's stable voice identity.

The minimal `upstream/fix-pack-downloads` branch remains a separate contribution candidate based on the same upstream revision. Earlier fork PRs remain separate drafts; review their bases before merging them. The existing distributed test APK predates this sync.

## Verified baseline

The synchronized app compiles in debug and deviceTest modes. All 103 JVM tests and app-wide formatting pass. Fresh minSdk 23 NewApi lint, R8 minification, ARM64 assembly, package identity/ABI checks, native library contents and catalog asset checks pass. Native outputs come from the verified upstream build rather than a fresh local native build; the review record explains that boundary. The validation APK has not been published as a new device candidate.

## Remaining work

Complete phone checks for translation, pack downloads and cancellation/retry, catalog refresh, voice upgrades/default selection, dictionary lookup, keyboard swap, and Settings accessibility/layout. Host compilation and tests do not establish device behavior.

There is still no enforced app-local networking policy. External speech recognition belongs to the chosen Android provider; the app cannot promise control over keyboard dictation. Distinct pane movement and explicit reverse translation remain proposed rather than implemented.

The prepared native PDF parser update remains separate. The current native revision is `8266da0aafce3842808e5911314c16b52e65dc61`; its app lockfile still selects lopdf 0.40. The archived update patch applies at this revision, but applying it was not part of this sync. Do not describe it as shipped or claim the tree is vulnerability-free.

Use the repository Dockerfile and Gradle wrapper for a fresh native build. The local sync validation reuses verified upstream native outputs and records that limitation explicitly. Preserve minSdk 23 and the existing NewApi lint gate. Keep source changes and meaningful validation evidence in the fork; keep private credentials and machine-specific tooling out of it.
