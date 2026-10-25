# Cumulative device candidates

Build candidates from one reviewed integration branch. Integrate accepted changes
before building; separate PRs are not automatically included. Record the exact
commit and artifact hashes with each distributed APK. Keep deferred native
dependency updates separate until their source, bindings and runtime are tested.

## Signing and versioning

The `deviceTest` variant retains the Translator label and release minification,
with a separate `dev.davidv.translator.devicetest` identity and storage. It requires
an existing keystore explicitly supplied through these environment variables:

- `TRANSLATOR_TEST_KEYSTORE`: absolute path to the test keystore
- `TRANSLATOR_TEST_STORE_PASSWORD`
- `TRANSLATOR_TEST_KEY_ALIAS`
- `TRANSLATOR_TEST_KEY_PASSWORD`

Keep this key in owner-controlled secure storage and reuse it for future test
updates. Do not commit the key, passwords, or environment files. The build does
not create a replacement key when one is missing. Supply secrets through the
build environment, avoiding command-line arguments or verbose shell tracing.
Increase the deviceTest version code for each distributed candidate; the current
test5 ARM64 code is 352. Keep ordinary release versioning/signing under the
maintainer's control.

This test identity cannot update the official app. An official in-place release
requires the installed distribution's signing identity or authorized rotation,
the official application ID, and an eligible version code. Keep that installed
app and its data untouched during fork testing.

## Build and record

Use the repository's Dockerfile/toolchain, recursive submodules, and locked native
dependencies. Commit the source before building. From the repository root:

```sh
./gradlew --no-daemon :app:testDebugUnitTest :app:ktlintCheck :app:assembleDeviceTest -PtargetAbi=arm64-v8a
python3 -m unittest discover -s tools -p test_verify_candidate.py
python3 tools/verify_candidate.py app/build/outputs/apk/deviceTest/app-arm64-v8a-deviceTest.apk --previous /path/to/previous.apk --output /path/outside/checkout/BUILD.json
```

Always supply `--previous` for an update candidate. Verification fails on changed
package/signers, non-increasing version codes, debug packaging, unexpected ABI,
or mismatched native/catalog build inputs. It verifies signatures using Android
build tools and records all packaged native hashes. Without `--previous`, the
record explicitly says upgrade preflight was not run. Authorized signing rotation
requires separate review; the preflight conservatively requires equal signers.
The source record assumes verification immediately follows a build from that
clean checkout; it is provenance bookkeeping, not a cryptographic build attestation.

## Device acceptance

Static preflight does not prove launch or data retention. Keep the candidate a
draft until the target device passes both paths:

1. On a disposable profile/device, install the candidate fresh and cold-launch it.
   Exercise local translation with a downloaded model and networking disabled.
2. On the existing test installation, record chosen languages, Settings values and
   downloaded packs. Install the candidate as an update without uninstalling or
   clearing data. Cold-launch it and verify those settings and supported packs
   remain usable. Do not substitute a fresh install for this check.
3. Exercise download cancel/retry, dictionary taps and closing during lookup,
   keyboard language swap, image dictionary/edit-as-text, and Settings at large
   text sizes and with TalkBack. Use fictional text for diagnostic privacy checks.

Record device/OS, old/new APK hashes, results and failures in the candidate review.
An installed version may already be newer; stop on installer rejection rather
than forcing a downgrade. Never uninstall the user's app to make an update pass.
