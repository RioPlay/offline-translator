# Native PDF security review — current status

**Start with [the maintainer handoff](maintainer-handoff.md).** It explains the problem, implementation, dependency order, reproduction commands and remaining limits without requiring the chat history.

The reviewed fixes are published in four RioPlay native forks and integrated into [Android draft PR #12](https://github.com/RioPlay/offline-translator/pull/12). Both ARM64 and ARMv7 jobs passed in [run 40240420210](https://github.com/RioPlay/offline-translator/actions/runs/40240420210) at app commit d77b0062a440e7abc1036ef8b5e7ac39f76afa79. Current build outputs are unsigned. Device PDF workflows and retained-data updates remain unverified for this candidate.

No upstream write, default-branch merge or release has occurred. This evidence branch is an archive, not an app merge candidate.

## AI disclosure and rationale

Substantial AI assistance used **Sol 6.1 and Astra** through OpenAI ChatGPT/Codex. [Read the change-by-change rationale, research/testing provenance, limitations and proposed smaller contribution scope](ai-transparency-and-scope.md). The broad integration draft is not an all-or-nothing upstream proposal.

## Current implementation

| Component | Implemented change | Review |
| --- | --- | --- |
| lopdf | 0.42.0 plus nested-catalog regression for RUSTSEC-2026-0187 | [engine PR](https://github.com/RioPlay/translator-rs/pull/1) |
| MuPDF | Vendor backports for CVE-2026-7233 and remaining CVE-2026-3308 hardening | [MuPDF PR](https://github.com/RioPlay/mupdf/pull/1) |
| MuPDF wrapper | Immutable patched submodule and matching fetch URL | [wrapper PR](https://github.com/RioPlay/mupdf-rs/pull/1) |
| CLD2 | TOML 0.9 and suffix matching remove obsolete build dependency chain | [CLD2 PR](https://github.com/RioPlay/rust-cld2/pull/1) |
| Engine root lock | Updated anyhow/crossbeam-epoch alongside reviewed native sources | [engine PR](https://github.com/RioPlay/translator-rs/pull/1) |
| Android | Both native consumers use the same reviewed engine; locked builds, native pin/source and packaged compatibility checks | [app PR](https://github.com/RioPlay/offline-translator/pull/12) |

MuPDF's version header alone was insufficient: the old pinned source predates vendor fixes despite reporting 1.28.0. See mupdf-source-review.json for exact ancestry. CFF app entry-point reachability is not established.

## Current evidence

- [Submission plan](submission-plan.md): all five proposed upstream targets, exact fork commits and reviewed upstream bases.
- [Android validation](android-ci-validation.json): both ABIs, unsigned artifacts, checks and the earlier unexplained packaging failure.
- [Published-source validation](published-source-validation.json): 72 locked PDF host tests using published dependencies.
- [Additional regression validation](regression-validation.json): 244 host library tests, all passing.
- [Integrated app OSV review](integrated-app-osv-review.json): 480 unique queries across the three current app Cargo locks.
- [Compiled-feature review](compiled-feature-review.md): limited native-feature reachability findings and exclusions.
- Native probe reports and C/Python harnesses retain baseline/fixed behavior and valid controls; isolated CLD2 validation records 26 passing tests.

## Historical evidence — not installation instructions

osv-lock-review.json (607 queries), candidate-osv-review.json, tested-engine.lock and the patches in ../../patches describe earlier preparation stages. Their source/lock identities differ from the integrated app. Do not apply those preparation patches on top of the current fork commits, use tested-engine.lock as an app lock, or treat historical package findings as the latest scan.

pdf-test-results.json and the native probe reports preserve local old/fixed comparisons. published-source-validation.json, regression-validation.json and android-ci-validation.json document the later published-source/integration checks. Earlier failed MuPDF staging results were discarded; the retained corrected checks verified the patched C source hashes.

## Still open

ZIP 2.4.2 extraction advisories RUSTSEC-2026-0367/0368/0369 remain. Reviewed EPUB/ODT and Kokoro/ndarray-npy callers read entries instead of extracting files; this does not clear the affected package. Maintenance warnings and broader native/C++ review remain open. Earlier Maven checks were scoped to selected configurations and were not freshly resolved by the 480-query Cargo scan.

Test6 predates these native fixes. Its positive phone feedback does not validate this candidate. The private test6 signer is unavailable; a replacement signer cannot perform an in-place update. Follow the [device matrix and signing requirements](https://github.com/RioPlay/offline-translator/blob/upstream/consolidated-fixes/docs/device-testing.md). No all-vulnerabilities-cleared or all-devices-tested claim is made.
