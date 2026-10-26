# Test7: signed cumulative device candidate

The owner selected a separate test installation because the test6 private signer was unavailable. Signed ARM64 and ARMv7 APKs have been verified and saved for owner testing. Phone launch and runtime results are still pending.

## Source and behavior

- Cumulative reviewed baseline: d77b0062a440e7abc1036ef8b5e7ac39f76afa79.
- Test candidate: 43f9de3255df1f6ce82bfc3c4230b3ee76bea56c, tree e851996c5fbd42da7f3f0d0c2480942e6d43f2a0.
- [Fork-only test PR #13](https://github.com/RioPlay/offline-translator/pull/13), based on the integration branch; do not merge it or submit its test infrastructure upstream.
- [Passing CI run 40254214351](https://github.com/RioPlay/offline-translator/actions/runs/40254214351). The checked-out PR merge commit f791b480eca8a9a403782be84810a61a94c04c7a has the exact candidate tree.
- Application behavior code and native manifests/locks are unchanged from the cumulative baseline. Differences cover the isolated test identity, shortcut target, test version, verification expectations and packaging/documentation.
- Package: dev.davidv.translator.devicetest7. Label: Translator. Version: 0.8.6-test7.
- It is a fresh installation alongside test6/the official app. Settings and app-private models are separate. No same-signer update or data-retention test is claimed.

## Final artifacts

| ABI | Filename | Version code | SHA-256 |
| --- | --- | --- | --- |
| ARM64 | translator-test7-arm64.apk | 372 | d2b359d0c8b407172491b19023ea0f346157da281d5e7b3827b9715fa88b8040 |
| ARMv7 | translator-test7-armv7.apk | 371 | 044ab0450823aed453785823f695ab62da000e9428132cc1f907665985c61d56 |

Final signer certificate SHA-256:
abed57535cf0804794e0c1712128b0aebf6fec316a36d448331f95593cdf7f7d.

The final APKs and private signing backup were saved to the owner's private file collection. The private key and password are not in this repository or GitHub Actions artifacts. Future updates to this test identity must reuse that signer and increase the version code.

## What was verified

Both jobs passed the 18 Python guards, immutable dependency-pin checks, JVM unit tests, ktlint, DeviceTest R8 minification, fatal/API lint, APK packaging, native ABI/alignment checks and exact patched MuPDF build-source checks.

CI used a disposable packaging signer. Final APKs were signed outside GitHub with the persistent test7 signer. For each final APK, verification checked:

- Successful v1/v2 signature verification and the expected signer certificate.
- The exact package, version/code, generic label, non-debuggable packaging, minimum API 23 and single expected ABI.
- Unique FileProvider authority and the optimized shortcut resource targeting the isolated package.
- All 758 non-signature APK entries byte-for-byte unchanged from the CI APK; bundled catalog matches the source.
- All six native libraries pass the packaged compatibility guard, with 16 KB ELF/ZIP checks on ARM64.
- Archive and intermediate APK hashes match the GitHub artifact metadata and native build report; lock and native-pin identities match the source.

Detailed records: [ARM64](test7-arm64-verification.json), [ARMv7](test7-armv7-verification.json).
CI artifacts named intermediate-test7-* use disposable signers; they are not the final distributed APKs.

## Remaining acceptance

The owner should install the appropriate final APK, open the new installation, obtain needed models and test offline text translation, PDF import/translation/export, images, language swapping, Settings disclosures and download cancellation/retry. Keep prior installations and data intact.

This packages all implemented cumulative changes. Remaining ZIP advisories, native-review coverage limits and representative-device testing are unchanged. No all-vulnerabilities-cleared or all-devices-tested claim is made. No upstream submission or merge has occurred.
