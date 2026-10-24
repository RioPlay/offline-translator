# Settings processing and network clarity

The first Settings card explains the processing routes: local translation/dictionaries/downloaded voices, network downloads and websites, and microphone delegation to an Android provider. It explicitly states that offline speech is not enforced and that these controls do not block internet access. It is an explanation of capabilities, not a live network monitor or enforced offline mode.

The HTTP server control is moved out of Advanced. Its description explains that LibreTranslate compatibility is an API format served by this phone's local engine. The setting's enabled/disabled state is labelled as configuration, not proof that a server is running; users are pointed to the existing notification for startup status. Scope options explain this-device-only localhost versus listening on all interfaces. Persisted enum values, default disabled/localhost settings and networking behavior remain unchanged.

The web, catalog, model and voice sections explain their own network use. Browser registration is labelled as offering the app for links. Browser cleanup explicitly cannot undo requests/data already received by websites. New explanation/scope keys use English fallback until translated, avoiding stale translations of old ambiguous labels.

The HTTP switch row has one toggle action and switch semantics, with a passive Switch indicator. Content wraps in the existing scrollable screen; there are no fixed text heights or new color-only status indicators.

## Validation

Run app ktlint, Android compilation, release NewApi lint and minified device-test packaging. On device, inspect ordinary/large text, landscape and TalkBack; toggle HTTP access and check that its configuration/status wording is accurate, verify localhost/LAN explanations, and confirm browser/catalog fields retain their saved values. A screenshot/device visual pass is still required. This change adds no network-blocking mode and no new cloud backend, permissions or native dependency changes.

All changes stay in RioPlay's fork, pending device testing and explicit approval for any upstream submission. Related context: upstream issues https://github.com/DavidVentura/offline-translator/issues/139 (offline speech) and https://github.com/DavidVentura/offline-translator/issues/298 (HTTP API; historical context), not closing claims.
