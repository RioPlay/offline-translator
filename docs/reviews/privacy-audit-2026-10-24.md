# Offline Translator privacy and service audit

Date: 2026-10-24. Official Android source: `7a662629cb267483ddf3889c6fe42e8fb9462e0d`. Native translator-rs revision selected by Cargo.lock: `94f47bff9d80bcd4e2779bceb037a5ed2c330fab`.

## Conclusion and scope

No cloud-translation fallback, analytics SDK declaration or deliberate translation-content upload was found in the reviewed paths. Text translation, OCR and TTS invoke local native engines and installed model files. This is a static source audit, not proof that a distributed APK has no network activity or that every transitive native dependency is harmless.

Reviewed 291 selected Android production/config/resource files, verified against their Git blob IDs, plus 28 files covering the native translation, speech and HTTP-server paths. Inspected the bundled model catalog for URL hosts. Did not audit all transitive Rust/C/C++ dependencies, reproduce a release build, match official source to the supplied older debug APK, or capture device traffic. The local audit checkout is a selected source snapshot, not a complete build checkout.

## Confirmed privacy concerns

### External microphone provider can use the cloud

`rememberSpeechInput` in [ActionButtons.kt](https://github.com/DavidVentura/offline-translator/blob/7a662629cb267483ddf3889c6fe42e8fb9462e0d/app/src/main/java/dev/davidv/translator/ui/components/ActionButtons.kt) starts `ACTION_RECOGNIZE_SPEECH`, supplies language/model extras and does not set `EXTRA_PREFER_OFFLINE` or enforce an on-device recognizer. This is user-triggered dictation, not a hidden background microphone service. Its selected provider may send audio for network recognition. The app's own TTS synthesis is local and separate.

Android documents that the offline preference defaults to false and can be ignored by an implementation: https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_PREFER_OFFLINE . Merely adding that flag would not establish an offline guarantee. A strict offline mode should fail closed when verified on-device recognition is unavailable, with clear UI and provider/device tests. Blocking this app's network alone does not establish that an external provider cannot communicate.

### User content appears in Android diagnostics

Raw input/output is logged by LanguageDetector, AidlTranslationService and ProcessTextActivity. DictionarySession logs requested words and results. TranslatorViewModel serializes non-TextInput messages, which can contain dictionary, OCR or other user data. BrowserActivity forwards web console payloads and logs shared text. Local logs can later appear in privileged debugging or bug reports; this is distinct from confirmed remote telemetry.

A prepared patch removes the explicit input/output/word/shared-text logs, logs only the UI message class, and consumes browser console messages without copying their contents. It preserves translation callbacks, displayed results and dictionary behavior. This is not a complete redaction of filenames, browsing URLs, error objects or all native diagnostics.

Related existing upstream work: [Datawav PR #350](https://github.com/DavidVentura/offline-translator/pull/350), open and unmerged when checked. It addresses overlapping logging paths on an older layout. Reconcile with that contribution before proposing a duplicate upstream PR. Native trace logging contains a text dump, but Android's configured native logger caps at Info in bindings/src/lib.rs; do not mistake that trace statement for an enabled Android upload path.

### Optional HTTP server is unauthenticated and unencrypted

[AppSettings.kt](https://github.com/DavidVentura/offline-translator/blob/7a662629cb267483ddf3889c6fe42e8fb9462e0d/app/src/main/java/dev/davidv/translator/AppSettings.kt) defaults the server to disabled, port 5000, localhost-only. TranslationApiService starts a visible foreground service only when enabled through settings. It can also be configured to bind all interfaces.

Native [HTTP handling](https://github.com/DavidVentura/translator-rs/blob/94f47bff9d80bcd4e2779bceb037a5ed2c330fab/src/http/api.rs) has no authentication check and returns wildcard CORS headers. The server uses plain HTTP. In all-interfaces mode, reachable network peers can submit translation work; traffic is not protected by TLS. Even localhost is not an application identity boundary. Browser access also depends on browser local-network policies.

This does not automatically expose the main screen's drafts or past translations: endpoints process client-supplied content. Uploaded translated files are retrieved using random 128-bit IDs; knowledge of a download URL is a capability. File expiry is swept on subsequent uploads, not an exact one-hour deletion timer. Keep the server disabled when unused; require authentication, bounded requests and an explicit LAN warning before treating network mode as hardened.

### Android backup is permitted

The manifest sets `allowBackup=true`; backup_rules.xml and data_extraction_rules.xml contain template rules without active exclusions. Eligible application files and preferences can therefore participate in the device's configured backup system. This is not evidence that translation requests are sent to a translation cloud. Android excludes cache/noBackup directories by default. Actual transfers depend on device settings, provider, storage locations and backup limits: https://developer.android.com/identity/data/autobackup . A privacy-focused fork should explicitly decide and test backup exclusions rather than imply everything is protected by offline inference.

## Background behavior and outbound connections

- DownloadService is a foreground notification wrapper for queued downloads and stops when work finishes; it is not a perpetual translation uploader.
- DocumentTranslationService and screen projection perform user-requested local work. Projection requires a valid Android projection result/token; invalid starts stop. Active live OCR can consume CPU/GPU/battery, which needs measurement.
- Accessibility and assistant integrations can capture screens when enabled/invoked. They are sensitive capabilities, not evidence of exfiltration. Accessibility scroll events invalidate overlays; the reviewed callback does not independently upload screenshots.
- TapToTranslateBootReceiver is registered unconditionally. Receiving boot starts the app process and its shared initialization even if optional features are off. AppEffects restores enabled notification/API features. This is a concrete startup-cost candidate, not evidence of malware or measured persistent idle drain.
- AIDL translation is intentionally exported for other apps. Requests return to their supplied callbacks; the service does not grant a caller access to current UI drafts. Resource limits/caller policy deserve separate review.
- The bundled catalog contains 946 HTTPS URL occurrences: 945 on offline-translator.davidv.dev and one on storage.googleapis.com. Downloads fetch catalogs/model/support files; voice sample requests occur on playback. An IP address and requested pack are necessarily visible to the relevant host. Configurable catalogs/redirects can introduce other destinations; this host inventory is not a runtime allowlist.
- The built-in browser loads selected websites and their resources with JavaScript enabled. Translation invokes native engines, but normal website connections still occur. The default home is about:blank. Do not describe web browsing as network-free.
- No Firebase, Crashlytics, Sentry or analytics SDK declaration was found in the inspected Gradle configuration or reviewed first-party source. Occurrences of "telemetry" in the live OCR code refer to local processing statistics, not an observed reporting endpoint.

## Prepared performance change

LanguageAssetManagerScreen rebuilds and sorts catalog rows, including dictionary/TTS/size lookups, whenever the normalized search query changes. The prepared change caches construction by catalog snapshot and filters that cached list by query. It should eliminate repeat row construction while typing with an unchanged catalog. It has not been benchmarked and does not establish a fix for initial opening, drawer animation, download progress jank or battery use. No refresh, repair, delete or download policy is changed.

## Validation and release status

Original selected-source Git blob verification: 291/291 matched. Draft performance patch applies to the official snapshot. Whitespace checks and clean-baseline patch replay are the appropriate completed source checks. No Android compilation, JVM app tests, instrumentation, device profiling or network capture was performed: the workspace lacks a complete checkout, Android SDK, Gradle wrapper and emulator. No optimized or hardened APK was produced.

The supplied bilingual debug APK and the earlier recovery checkpoint remain separate evidence. Bilingual modifications have not been reconstructed into this official-source draft. Historical 93/99-test reports do not validate these changes.

## Fork and contribution constraints

Forking and modification are permitted by the GPLv3 license. Distributed derivatives must retain notices, identify modifications and provide corresponding source/build material under applicable GPL terms; dependencies/models need their own license review. No repository-specific CONTRIBUTING, CLA, DCO or AI-contribution policy was found in the current tree. Upstream controls acceptance; PR #312's maintainer comment describes cherry-picking a contributor's UI fix, so a closed PR need not mean rejection. Our fork will need its own APK signing identity and an installation/data-migration decision.

RioPlay authorized creation of a fork and work on fixes. The available GitHub connector exposes no fork-creation operation, and RioPlay/offline-translator was not accessible when checked. No fork, commit, branch or PR for this project has been published by this session. Browser fallback requires approval before use. Once available, base publication on the real upstream commit/tree, not the synthetic local selected-source snapshot history.

Next proof: create/access the fork, obtain a complete source checkout, reconcile the existing privacy PR, build an unchanged release/profileable baseline, apply and test the bounded fixes, then measure the same screen interactions and network scenarios on a target device. Speech, HTTP and backup policy changes remain deferred pending explicit behavior and recovery tests.
