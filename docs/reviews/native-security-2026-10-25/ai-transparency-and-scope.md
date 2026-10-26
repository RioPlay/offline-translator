# AI disclosure, change rationale and contribution scope

## Purpose and authorship

This work began with RioPlay's concerns about responsiveness, privacy and security in Offline Translator. The aim is to offer useful, reviewable fixes to the existing maintainer. The fork is an integration/test environment, not a proposed change of project ownership, branding, release authority or roadmap.

**AI involvement was substantial.** OpenAI ChatGPT/Codex was used with **Sol 6.1 and Astra**, as identified by RioPlay. AI assistance covered source/advisory research, implementation and test changes, test/build execution, interpretation of results, GitHub operations in the authorized forks, and documentation. Per-change model attribution was not retained; we do not invent a division of work or claim that two model names establish independent review.

RioPlay supplied the goals, constraints and device feedback, and controls whether anything is submitted upstream. RioPlay reported that an earlier test6 build launched, downloaded packs and felt more responsive on a Pixel 8 Pro. That build predates the latest native PDF changes. No line-by-line human review or independent professional security audit of the complete current candidate is claimed. AI-written code, tests and explanations remain subject to maintainer review; agreement between generated code and generated tests is not sufficient evidence by itself.

## What changed, why, and what it costs

The current Android draft is a 75-file integration candidate. The rows below describe reviewable areas, not a requirement to adopt the whole draft.

| Change | Before and after | Reason and supporting evidence | Tradeoff / boundary |
| --- | --- | --- | --- |
| Catalog reuse and dictionary work | Repeated catalog/filter/voice construction and synchronous dictionary lookups become reused data and asynchronous lookup with stale-result rejection. | Reduce avoidable UI-thread work. DictionarySessionTest covers lookup behavior; the user observed better responsiveness in test6. | Cache invalidation and request ordering need care. No controlled frame-time, memory or battery benchmark was recorded. |
| Diagnostic privacy | Reviewed Kotlin logs containing payloads, paths/URLs, exception details and image/overlay metadata become reduced, non-content diagnostics or are removed. | Reduce disclosure through logs. Source review and FailureDiagnosticsTest support the scoped change. | Less detail for debugging. No complete audit of native/system logs, malware finding or demonstrated cloud exfiltration is claimed. |
| Download handling | Stalled, cancelled or known-length truncated downloads get bounded waits, failure handling and partial-file cleanup. | Avoid leaving incomplete packs and improve cancellation behavior. PackDownloadTest exercises the failure paths. | Timeouts can affect very slow connections. The user's intermittent download problem was work-network blocking, not evidence that this patch fixed that incident. |
| lopdf parser | 0.40 is replaced with 0.42 in both engine manifests/locks, with a bounded nested-catalog regression. | RUSTSEC-2026-0187; the old parser aborts on the retained roughly 21 KB input and the new parser succeeds. Published-source PDF tests pass. | A dependency upgrade can change parsing behavior. Real-model Android PDF import/translation/export remains unverified for this candidate. |
| MuPDF bounds checks | Three C files receive existing vendor CFF/image bounds and arithmetic fixes; wrapper and engine pins propagate the patched source. | Exact source ancestry showed missing fixes despite the 1.28.0 header. Old/fixed CFF ASan/UBSan and image probes preserve valid controls and demonstrate malformed-input rejection. | These are vendor backports, not original vulnerability discoveries. CFF app entry-point reachability is unestablished; other native libraries are not comprehensively cleared. |
| CLD2 build dependencies | A fixed filename regex becomes suffix matching; TOML 0.1 becomes 0.9; an existing test import is corrected. | Remove obsolete build dependency chains. The selected 24 C++ files plus wrapper stay unchanged; 26 isolated tests pass. | This concerns build-time parsing, not a demonstrated attack through translation text. Build-script maintenance still changes. |
| Build dependencies and native packaging | Reviewed dependency constraints, API desugaring, stable UniFFI library identity, locked Rust builds and ABI/alignment/source checks are applied. | Address scoped dependency/API/loader concerns and prevent testing an unintended native revision. Both ARM CI jobs and packaged-library/source checks pass. | Additional maintenance and review are required when pins/toolchains change. Passing build checks is not minimum-API or 16 KB device-runtime proof. |
| Backup policy | App data is excluded from cloud backup; legacy Android 6–11 backup/restore is disabled, while Android 12+ device migration remains subject to OS policy. | A privacy preference implemented through documented Android backup rules and configuration tests. | **Optional product-policy change.** It reduces restore convenience and needs explicit maintainer agreement. It is not necessary to accept PDF, logging or performance fixes. Ordinary updates and public exports are separate. |
| Settings and keyboard controls | Settings describes local translation, downloads, websites, external speech providers and local HTTP behavior; language swap is placed above the keyboard. | Help users understand network use and reduce reaching during input. Earlier user feedback informed the UI work. | **Optional UX changes.** Network disclosures and control placement should be reviewed separately; usability/accessibility evidence is limited. |
| Test identity and evidence tooling | A separate device-test variant and source/signature/upgrade checks support fork testing. | Preserve the official app identity while making candidate contents and upgrade assumptions explicit. | Primarily contributor infrastructure. Maintainers can decline it. Current outputs are unsigned and the former test6 private signer is unavailable. |

For file-level entry points and copyable commands, see [maintainer-handoff.md](maintainer-handoff.md). Each eventual narrow PR should repeat its own problem, before/after example, reasoning, exact files, test evidence and compatibility costs; a link to a large report should not replace that explanation.

## How the scope grew

1. User experience concerns led to UI-work and dictionary investigation, plus Settings/network-clarity changes.
2. Privacy/security review expanded into logging, download failure handling, backup policy and dependency inventories.
3. PDF investigation crossed the engine, Rust wrapper and nested MuPDF source. Missing vendor fixes required linked fork revisions to test the actual compiled source.
4. Prior changes were consolidated for cumulative builds. This produced a broad integration candidate spanning five repositories, including optional choices and contributor infrastructure.

This expansion explains the current branch; it does not justify requiring the maintainer to adopt everything. Further feature expansion should stop while contribution scope is agreed.

## What was researched and how conclusions were checked

| Research | Findings and limits | Inspectable evidence |
| --- | --- | --- |
| Rust dependency advisories: OSV/RustSec | The integrated app Cargo locks were queried in 480 unique requests. Targeted findings were removed; ZIP advisories and maintenance warnings remain. A scanner match does not establish runtime reachability, and no match does not prove safety. | integrated-app-osv-review.json; advisory links and exact source/lock identities in the handoff |
| MuPDF vendor fixes and pinned source | Source ancestry, rather than the version label alone, identified missing vendor patches. Existing vendor credit/licensing is preserved. | mupdf-source-review.json; linked vendor commits; CFF/image harnesses and results |
| Android documentation and app configuration | Backup, signing/update, API and page-size constraints informed configuration and guards. Configuration checks do not prove every Android/OEM implementation. | App docs/security-status.md and docs/device-testing.md; XML/native/upgrade guards |
| Call-site and compiled-feature inspection | ZIP callers were inspected; selected font/image-library findings were examined against enabled build features. These are scoped reachability assessments, not blanket package clearance. | compiled-feature-review.md; android-mupdf-features.txt; remaining findings |
| Regression and build execution | 72 PDF and 244 additional host tests passed against published dependencies; earlier isolated CLD2 validation passed 26 tests. Both ARM Android builds passed JVM tests, formatting, release packaging and native checks; 18 Python guards passed. | published-source-validation.json; regression-validation.json; android-ci-validation.json; linked GitHub run |

Some tests and native probes were AI-created or modified; others are existing project tests. Test counts are different suites, not independent audits. Vendor patches, reproducible old/fixed behavior, actual build logs and user device feedback provide different kinds of evidence, with their limits kept separate.

## Problems and unknowns retained in the record

- An earlier MuPDF staging attempt did not contain the intended patches. Those results were discarded; the corrected build checked exact patched source hashes and later published-source tests were run.
- The first ARM64 integration job failed during packaging without an explanatory exception. A clean build with added diagnostics passed without changing application/native source. The original cause remains unknown.
- The previous local test signing private key became unavailable after workspace maintenance; it had not been durably backed up. A new signer cannot silently update test6 in place.
- No measured battery improvement, complete native/log audit, all-CVEs-cleared result or all-devices compatibility result is claimed.
- The current native candidate still needs signed-device workflow and retained-data update testing.

Preserve failures and superseded results with clear labels. Do not present an earlier test run as validation of a later source revision.

## A smaller upstream offer

**Proposed decomposition; the Android draft has not yet been split.**

Offer focused changes in areas the maintainer wants: (1) performance/dictionary behavior, (2) log privacy, (3) download failure handling, and (4) documented dependency/security fixes with their necessary native dependency order. Discuss backup policy and UX changes separately. Keep fork-specific signing/testing convenience out of a minimal upstream patch unless requested. Evaluate the ongoing maintenance cost of new guards with the maintainer.

The [submission plan](submission-plan.md) describes dependencies, not an obligation to accept the full set. Each proposed PR should be independently understandable and independently acceptable where technically possible. Linked native changes need coordinated pins and retesting; they cannot be called independent when they are not.

Before submission, ask whether the maintainer accepts AI-assisted contributions and which narrow fixes they want. The app README/root files checked did not expose a contribution-specific AI policy; that is not evidence of consent or of policies in every related repository. RioPlay's approval is required before contacting or writing to upstream. Maintainer ownership of design, dependencies, signing and releases remains explicit.

A concise opening disclosure for each PR:

> Developed with substantial AI assistance using OpenAI ChatGPT/Codex, including Sol 6.1 and Astra. AI assisted with research, implementation, tests and documentation. The linked record distinguishes executed checks, user device feedback and unverified claims. This is a focused contribution proposal for maintainer review; no independent security audit or complete human code review is claimed.

No raw chat export is necessary. Publish the relevant decisions, source citations, commands, exact revisions and results; omit unrelated conversation, private data and credentials.
