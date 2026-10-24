# Continue Offline Translator work

Updated 2026-10-24. This document is the continuation entry point; the long chat is not required.

## Ownership and approval boundaries

- Work only in RioPlay's forks. Never submit, comment, open issues/PRs, push, merge or otherwise write to an upstream-maintained repository without RioPlay's explicit approval. Upstream reading is allowed.
- Keep the generic launcher name **Translator**. No personal branding.
- Device-test builds use dev.davidv.translator.devicetest, separate from the regular app. Preserve that separation.
- All PRs remain drafts. No merge, official release or upstream submission has been approved. Device testing precedes upstream decisions.
- Prefer small, validated changes. Do not force unrelated dependency major upgrades simply to clear advisory matches.
- No agent delegation unless explicitly requested.

## Where to start

Clone https://github.com/RioPlay/offline-translator and check out **test/settings-clarity**.
The tested APK's immutable source is **ff7ef7a5a6edd9c6f49515ca49225e9f44e97c8e** (tree 13515e19c89c310207a9bca9dfbb98ee873ed7ab). Later documentation commits on the branch do not change that APK's source.
The default master branch is still 7a662629cb267483ddf3889c6fe42e8fb9462e0d; it does not contain these unmerged changes.

## Published work

- PR #1: Remove explicit translation and dictionary payloads from logs — https://github.com/RioPlay/offline-translator/pull/1 (draft, unmerged; branch fix/private-payload-logging).
- PR #2: Cache catalog row construction while filtering languages — https://github.com/RioPlay/offline-translator/pull/2 (draft, unmerged; branch perf/cache-language-filter-rows).
- PR #3: Document privacy, dependency risks and performance/CI priorities — https://github.com/RioPlay/offline-translator/pull/3 (draft, unmerged; branch docs/privacy-performance-review).
- PR #4: Keep dictionary lookup off the UI thread and reuse catalog search data — https://github.com/RioPlay/offline-translator/pull/4 (draft, unmerged; branch perf/dictionary-and-catalog).
- PR #5: Make language swap reachable just above the keyboard — https://github.com/RioPlay/offline-translator/pull/5 (draft, unmerged; branch ui/keyboard-language-swap).
- PR #6: Update affected build tools and document the pending native PDF fix — https://github.com/RioPlay/offline-translator/pull/6 (draft, unmerged; branch security/build-tools-and-pdf-review).
- PR #7: Make local processing and network behavior explicit in Settings — https://github.com/RioPlay/offline-translator/pull/7 (draft, unmerged; branch ui/settings-processing-clarity).

PRs #1–3 target master; #4–6 target test/device-privacy-performance; #7 targets test/keyboard-performance-security. Do not assume all can be merged directly into master without review/rebasing.
The combined test branch includes the logging, performance, keyboard, build-tool and Settings clarity implementations. PR #3 separately contains the broader audit/CI planning documents.

## Latest device candidate

- Translator **0.8.6-test3**, version code **332**, ARM64, non-debuggable, development-signed.
- SHA-256: **d6f755762ebef2fa01fb4473be79482b2e243020f23ddc2e3046acacc52429c7**.
- Saved APK: https://chatgpt.com/library?file_id=libfile_47d7af16608481919a75f9ee58d57ebd&account_id=personal
- Test3 uses test2's signing certificate and can update that copy in place. Test1's CI signing key differs; replacing test1 requires uninstalling only the test copy, removing its private app data. Keep the regular app installed.
- Passed: compilation, all 90 JVM tests, app ktlint, fresh release NewApi lint at minSdk 23, minified ARM64 assembly, signature/identity/ABI checks.
- Remaining: actual device visual, large-text, landscape and TalkBack checks, plus normal translation/download/TTS/dictionary regressions.
- RioPlay reported that the initial performance candidate was much more responsive. This is qualitative feedback, not measured frame-time or battery evidence.
- Verification record: https://github.com/RioPlay/offline-translator/blob/ui/settings-processing-clarity/docs/reviews/settings-test3-validation-2026-10-24.json

## Implemented behavior and limits

Settings starts with Processing and internet use. It distinguishes local translation/dictionaries/downloaded voices, network downloads/websites, external microphone recognition, and the HTTP API served by this phone. HTTP configuration is not presented as proof of a running server. Server control is visible outside Advanced; scopes and browser/catalog/cleanup behavior are explained. New explanations use English fallback until translations are reviewed.

There is **no enforced local-only/network-blocking mode yet**. Microphone input delegates to an Android provider and does not enforce offline recognition. Keyboard dictation belongs to the keyboard's own privacy settings. No cloud translation backend was found in the reviewed build; this does not mean every feature avoids the network.

The existing language swap changes the language pair and retranslates the same input. A reachable duplicate of that action appears above the keyboard in normal text mode. The proposed separate focus/swap-sides/back-translation actions are **not implemented**.

Dictionary lookups run off the UI thread, serialize native work, and suppress obsolete results. Catalog filtering reuses localized search data.

## Security work still pending

PR #6 records selected dependency scans and conditional exposure. Remaining build-classpath advisory matches are documented; the tree is not claimed CVE-free. ZIP extraction advisories have no established extraction call path in reviewed callers. Legacy CLD2 dependency findings are build-time scoped. Review the actual reports before further upgrades.

The lopdf fix is **not in the APK**. App native dependency remains pinned to DavidVentura/translator-rs at 94f47bff9d80bcd4e2779bceb037a5ed2c330fab.
The complete prepared patch is preserved at docs/patches/translator-rs-lopdf-0.42.patch, with evidence at docs/reviews/pdf-security-validation-2026-10-24.json (both in the combined branch and PR #6).
The old parser's stack-overflow abort was reproduced; the fixed parser and exact standalone regression passed. Full default app native bindings compiled/linked for ARM64/API23 using the isolated local patch. This is not the full native test suite or a device PDF pipeline test.
The local native commit 4391b6c85bbfc021ff54c459e239394803c13eda was not pushed, but its full three-file patch is archived on GitHub.
RioPlay/translator-rs was inaccessible at the last check. Enable/create that owned fork, publish a patch branch based on the pinned revision (not unrelated latest native API changes), repin app manifests/locks, and rebuild/device-test. The GitHub connector used here cannot create forks.

## Suggested next work (proposed, not implemented)

1. Collect test3 device feedback.
2. Implement an enforceable app-local networking policy and explicit handling of external speech; do not promise control over a third-party keyboard.
3. Add per-feature processing indicators grounded in real execution paths; avoid fake global offline status.
4. Separate writing focus, pane movement and explicit reverse translation. Preserve drafts and Undo.
5. Finish native PDF integration after owned-fork access is available.

## Rebuilding / workspace-only items

The repository Dockerfile, Gradle wrapper and device-test workflow are on GitHub. Build with assembleDeviceTest -PtargetAbi=arm64-v8a. Tooling used: JDK17, Gradle8.14.6, Kotlin2.4.21, AGP8.13.2 with R8 override9.1.57, Build Tools35, platform/target34, NDK28.0.13004108, Rust1.88, cargo-ndk4.1.2. Keep minSdk23; API desugaring and iterator-based install queues preserve compatibility.

The previous chat's installed SDK/JDK/Rust, caches, local helper scripts, raw logs/probes and private debug keystore are temporary workspace items, not GitHub assets. Do not assume they exist in a new chat. The source, prepared native patch and meaningful validation evidence are recoverable from GitHub; APK bytes are saved separately above. Never upload the private signing key to a public repository. Losing that key means future test builds may require replacing the test app with a new key.

One local lint intermediate reused removed calls and stale line positions. Fresh analysis after clearing the generated lint partial results passed; compiled queue bytecode used iterators. Do not paper over NewApi errors with a blanket suppression or baseline.
