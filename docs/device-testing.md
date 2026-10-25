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
test6 ARM64 code is 362. Keep ordinary release versioning/signing under the
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
python3 -m unittest discover -s tools -p 'test_*.py'
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
draft until representative supported devices pass both installation paths.
The app declares Android 6/API 23 as its minimum. Published CI builds cover
ARM64 and ARMv7; x86_64 is a development target, not a validated release here.
One phone passing does not establish compatibility across that range.

Use this minimum coverage matrix before calling the contribution release-ready:

| Environment | Acceptance focus | Current runtime evidence |
| --- | --- | --- |
| API 23, ARMv7 or ARM64 | Minimum-API launch, translation, storage and permission paths | Pending |
| Android 10/11 | Scoped storage, legacy backup exclusions, downloads and upgrades | Pending |
| Android 12–14 | Device migration, foreground services, notification/overlay permission denial | Pending |
| Recent Android, ARM64, 4 KB pages | Fresh install and retained-data update; offline core workflows | Pending |
| Android 15+, ARM64, 16 KB pages | Native load and translation without page-size compatibility mode | Pending |
| Small/low-memory phone and tablet/foldable | Memory pressure/recreation, keyboard, rotation, large text, TalkBack and RTL | Pending |

These are representative environments, not a promise that every OS/OEM/device
has been tested. Models need sufficient storage and memory; measure failures on
constrained hardware instead of assuming a modern flagship represents it.
CI checks both published ABIs and the API compatibility lint guard. Run the
packaged-native guard on each APK, including every newly supported ABI:

```sh
python3 tools/check_apk_native.py /path/to/candidate.apk --abi arm64-v8a
```

This checks native ELF architecture, required libraries, and 16 KB ELF/ZIP
alignment on 64-bit targets. It does not prove native runtime correctness.
See [Android's page-size testing guidance](https://developer.android.com/guide/practices/page-sizes).

For each representative device:

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
Also record ABI, page size, available memory/storage, and distribution/signing
identity. Denying optional camera, speech or overlay permissions must leave
ordinary text translation usable. Check downloaded-model translation in airplane
mode and verify that enabling an external speech provider is clearly disclosed.
An installed version may already be newer; stop on installer rejection rather
than forcing a downgrade. Never uninstall the user's app to make an update pass.
