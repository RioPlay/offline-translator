# Cumulative test7 candidate

This is a fork-only device candidate based on reviewed app commit
`d77b0062a440e7abc1036ef8b5e7ac39f76afa79` and all four native security pins in
`tools/native-security-pins.json`. It includes the cumulative performance,
privacy, download, Settings, backup and reviewed PDF/dependency changes.
Remaining ZIP advisories and the security review's coverage limits still apply.

The user selected a separate test installation after being informed that the
previous test6 signer was unavailable. Package: `dev.davidv.translator.devicetest7`;
launcher label: **Translator**; version: **0.8.6-test7**; ARM64 code: **372**;
ARMv7 code: **371**. It neither updates nor replaces test6 or the official app.
Its settings and downloaded models are separate. Keep existing installations.

Only test identity, shortcut routing, test version, candidate verification,
packaging workflow and these instructions differ from the reviewed baseline.
The native dependency locks and application behavior code are unchanged.
DeviceTest retains release minification and non-debuggable packaging.

CI rebuilds and validates both ABIs using the same pinned sources. Its temporary
packaging signer is disposable and is never the distributed phone-test signer.
The CI artifacts are named `intermediate-test7-*`; do not install them as the
final candidate. Final APKs are re-signed with the persistent private test7
signer outside GitHub. Verify the final signature, package/version/label,
ZIP/ELF alignment, and unchanged APK entry contents, and record final APK and
certificate hashes. Never upload the persistent private key to this repository
or to public Actions artifacts.

For first installation, no same-package upgrade check is claimed. Future test7
updates must reuse its signer and increase the version code. A new package is
not evidence that retained-data upgrades work. Fresh-device launch, offline
translation, PDF import/translation/export and optional-permission paths still
need actual device testing. No upstream write, merge or release is authorized.
