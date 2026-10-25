# Coordinated upstream submission plan

Status: automated fork integration passed on ARM64 and ARMv7; signed-device acceptance remains pending; upstream submission requires RioPlay's explicit approval. No default-branch merge or production release is requested.

## Review order and exact proposed targets

| Order | Proposed target | Fork draft | Scope |
| --- | --- | --- | --- |
| 1a | DavidVentura/mupdf, master | [RioPlay/mupdf#1](https://github.com/RioPlay/mupdf/pull/1) | Vendor backports for CVE-2026-7233 and remaining CVE-2026-3308 hardening; preserve valid CFF/image inputs. |
| 1b | DavidVentura/rust-cld2, master | [RioPlay/rust-cld2#1](https://github.com/RioPlay/rust-cld2/pull/1) | Remove obsolete build dependency chain, preserving C++ source selection and runtime API. |
| 2 | DavidVentura/mupdf-rs, main | [RioPlay/mupdf-rs#1](https://github.com/RioPlay/mupdf-rs/pull/1) | Consume the reviewed MuPDF commit; no new Rust API/features. Depends on 1a. |
| 3 | DavidVentura/translator-rs, master | [RioPlay/translator-rs#1](https://github.com/RioPlay/translator-rs/pull/1) | lopdf 0.42 and nested-catalog regression; reviewed wrapper/CLD2 pins and lock updates. Depends on 1b and 2. |
| 4 | DavidVentura/offline-translator, master | [RioPlay/offline-translator#12](https://github.com/RioPlay/offline-translator/pull/12) | Consolidated privacy, performance, download/update checks and reviewed native fixes. Depends on 3. |

The MuPDF changes already originate from vendor fixes; this plan backports them to the app maintainer's pinned fork, not a duplicate proposal to Artifex. Targets are the direct parent repositories verified through GitHub metadata.

After approval, submit in dependency order and link the actual related PRs. Keep downstream proposals draft while prerequisite revisions are unsettled. Once maintainers accept dependency changes, replace temporary RioPlay URLs with the agreed maintainer repositories and full immutable commit pins, regenerate the locks and security-pin record, and rerun affected host tests plus both Android builds. Current fork build results cannot validate future repins.

## Reviewed source

- Android app: d77b0062a440e7abc1036ef8b5e7ac39f76afa79
- Engine: 904c8661f1bb9ffa4a5ef93623aa20707af4f50e
- MuPDF wrapper: 285b012811563c412052df685d2b872c9a95c82c
- MuPDF: ba2c748657a4a4c5bd418b684b077b142a61d433
- CLD2: 7c9be0fcd97a77d65a47a14894078a6110b115ad

Upstream defaults rechecked on 2026-10-25 and unchanged from the reviewed bases:
app 2281d8d65fc5c195257c0b47fc6d768440f4c416;
engine bd32a4f272e239efb6867795c8b7d0c47c619b49;
wrapper 6f695fae38dcd1d4ed8f39f56b177042f3ff11a5;
MuPDF ef017f6ad65ea02efd006f387131bfeed1b18886;
CLD2 2bca970e413453717e000a1e878bedf2d3d45e75.

## Regression evidence

- 72 locked host PDF tests passed against published dependencies, with image translation enabled.
- 244 additional locked host library tests passed: core 144, documents 20, renderer 25, translation 55. EPUB, ODT and HTML features were enabled; no test-harness ignores or failures.
- The tested engine working-tree files exactly match published commit 904c8661f1bb9ffa4a5ef93623aa20707af4f50e. No dependency path overrides.
- 26 earlier isolated CLD2 workspace/unit/integration/doc tests passed; its 24 C++ files plus wrapper selection is unchanged.
- 18 Python guards passed. These exercise backup/update/native compatibility checks and reject stale/wrong native security sources.
- Bounded old/fixed native probes preserve valid controls while rejecting malformed CFF/image arguments.
- Host library tests are not Android runtime tests or model-backed translation acceptance. Environment-dependent smoke tests were not counted as executed evidence when their model assets were unavailable.

## Remaining acceptance gates

A successful CI build is an automated proof of integration, not a claim of zero regressions on every device. Before calling this release-ready, exercise fresh install/cold launch, PDF import/render/translate/export with real models, normal text/image/document translation, download cancel/retry, keyboard swaps, permission-denied behavior and offline use. Validate retained-data updates with the same signer, plus representative minimum-API, ARMv7 and 16 KB devices. Use docs/device-testing.md for the matrix.

The previous test6 private signing key is unavailable. CI outputs are unsigned. A differently signed replacement cannot update test6 in place; do not uninstall or clear data to disguise that failure. Test6's successful phone testing predates these native patches. Recover the owner-controlled signer or agree on a separate test identity before distributing a new signed phone candidate.

The 480-query app-lock scan removed the targeted lopdf/regex/rustc-serialize/thread_local/anyhow/crossbeam findings. ZIP 2.4.2 extraction advisories remain; maintenance warnings and incomplete native/C++ review remain explicit. No all-CVEs-cleared claim.

## Approval boundary

RioPlay must approve the concrete upstream targets and reviewed changes before any upstream PR, comment or branch write. Fork-local testing and review may continue. Approval to submit is not approval to merge or release.
