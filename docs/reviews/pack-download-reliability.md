# Pack download reliability validation

Status: JVM-validated source; full Android build and device validation pending.

The change sets 30-second connection/read inactivity timeouts, validates response bytes against a supplied Content-Length, checks cancellation before publishing, and cleans up temporary files and HTTP connections. Explicit gzip pack decoding and unknown-length responses remain supported.

Source branches in this fork:
- `fix/upstream-download-reliability`: integrates with the combined test branch.
- `upstream/fix-pack-downloads`: three changed Kotlin files on upstream `bd675fecea875d4e5b3c0f62e24b2a9c3c84f6da`, without fork-specific UI or build changes.

Validation:
- Kotlin 2.4.21 standalone compilation, JVM target 11, OpenJDK 17.
- All 13 JUnit 4.13.2 loopback HTTP tests passed on both source trees.
- Tests cover complete/truncated downloads, preserving an existing file, HTTP failures, stalled headers/body, chunked and unknown-length responses, valid/corrupt/truncated gzip, progress, and cancellation.
- Removing the response-length and final cancellation guards makes three targeted regression tests fail.
- ktlint 1.0.1 passed for all touched Kotlin files; git diff whitespace checks passed.
- The minimal patch applied cleanly to current upstream. Published code trees match tested local trees.

Pending before submission:
- Full app compilation, full JVM suite, Android minSdk 23 NewApi lint, and APK assembly.
- Android tests of real pack/catalog downloads, interrupted downloads, cancel/retry, and installed-pack usability after failures.
- Recheck upstream changes and obtain approval before any upstream submission.

Limits: host JVM tests do not establish Android networking behavior. Content-Length checks completeness, not authenticity; unknown-length responses cannot be validated this way. Read timeouts bound inactivity, not total transfer duration. No new APK, enforced offline mode, upstream PR, release, or merge is included.
