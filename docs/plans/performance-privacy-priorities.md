# Performance and privacy delivery queue

Base reviewed: DavidVentura/offline-translator `7a662629cb267483ddf3889c6fe42e8fb9462e0d`. Source and issue status checked 2026-10-24. Code changes remain in draft. The combined fork-only device candidate `3424e6974174850deab4e193246699e7d7ecd129` passed Android assembly and APK packaging/signature checks in run 40110607784, with independent local APK verification. Individual code PR-head builds, device regressions, network captures and performance measurements remain pending.

Dependency findings and bounded upgrade gates are in `../reviews/dependency-review-2026-10-24.md`. Measured CI timing and the proposed first cache experiment are in `ci-build-efficiency.md`. Neither a dependency upgrade nor a warm-cache speedup is claimed complete. RioPlay's device test comes before upstream submission, and every upstream mutation requires their explicit approval.

## Immediate reviewable changes

1. **Remove explicit private payload logging.** Separate branch `fix/private-payload-logging`, six Kotlin files. Preserve results, callbacks and static diagnostics. Reconcile overlap with [upstream PR #350](https://github.com/DavidVentura/offline-translator/pull/350), open and unmerged when checked. This first pass does not establish comprehensive exception/URL/filename redaction. Merge gate: app build and canary tests across local text, dictionary, share, Binder, OCR and browser success/failure paths.
2. **Cache language rows while filtering.** Separate branch `perf/cache-language-filter-rows`, one Kotlin file. Build/sort catalog rows when the catalog snapshot changes; search filters that cache. Merge gate: build, equivalent search results and install/delete/update invalidation, locale/rotation checks, then a same-device comparison of typing/frame timings. No speedup or battery reduction has been measured. This patch does not resolve the original cold-open/drawer lag report.
3. **Record evidence and the risk-ranked plan.** Separate branch `docs/privacy-performance-review`. Documentation carries the audit, limitations and acceptance criteria. It adds no Android behavior or new runtime dependencies.

The first two code branches are independent. Do not combine their scope or claim an entire UI/security overhaul is complete. PR descriptions must distinguish passed source checks from Android/device tests that remain outstanding. Publish as drafts until their gates pass.

## Highest-value next performance work

Profile the reported language-management screen opening, scrolling, expansion and progress updates, plus dictionary drawers, on an unchanged release/profileable baseline. Compare the same device, ABI, packs and build mode. Capture UI-thread/native-call timing and frame statistics, then change the demonstrated bottleneck. DictionarySession.lookup currently calls native lookup synchronously; moving it off the caller thread needs cancellation/request-identity and back-navigation tests so a late result cannot reopen or overwrite a newer drawer. Do not disable animations/features or reduce display refresh to manufacture an improvement.

No directly matching upstream issue was identified for catalog-row recomputation during search. The current performance PR is driven by the local user report and source finding. It must not close an unrelated issue to obtain a reference.

## Verified upstream references and boundaries

| Reference | State when checked | Appropriate relationship |
| --- | --- | --- |
| [PR #350 — Stop logging private translation text](https://github.com/DavidVentura/offline-translator/pull/350) | Open, unmerged | Direct overlap for the logging PR. Review/rebase against its changed older architecture before proposing another upstream contribution. |
| [#139 — STT - Speech To Text Feature](https://github.com/DavidVentura/offline-translator/issues/139) | Open | Related to a future genuine offline speech-input path. The current logging/cache PRs do not implement it. |
| [#298 — Optional local HTTP API](https://github.com/DavidVentura/offline-translator/issues/298) | Closed/completed | Historical design/compatibility context for keeping the API off/local by default. Future authentication/transport changes must preserve legitimate local clients. No reopening/closure claim. |
| [#158 — URL translation privacy/security overhaul](https://github.com/DavidVentura/offline-translator/issues/158) | Closed/completed | Historical browser privacy requirements. Bridge/iframe tests and sensitive-diagnostic work are narrower than that feature request. |
| [#44 — Do not ask for access to all files](https://github.com/DavidVentura/offline-translator/issues/44) | Open | Related to future storage permission/import work. No permission or storage migration change is implemented in these PRs. |
| [#339 — UI / UX Improvement](https://github.com/DavidVentura/offline-translator/issues/339) and [#300 — System & UI feedback](https://github.com/DavidVentura/offline-translator/issues/300) | Open | Broader bilingual editing, direction, keyboard and layout work. Our earlier bilingual APK is separate evidence; those modifications have not been integrated into this official-source draft. The filter-cache PR does not resolve them. |
| [#145 — Downloaded languages disappear](https://github.com/DavidVentura/offline-translator/issues/145), [#332 — App crashes on download](https://github.com/DavidVentura/offline-translator/issues/332), [#356 — Download Persian language](https://github.com/DavidVentura/offline-translator/issues/356) | Closed/completed | Regression context only. Preserve installed packs, legacy API support and repair behavior; do not claim these are newly fixed by caching or logging changes. |
| [#177 — Missing WebView failure diagnostics](https://github.com/DavidVentura/offline-translator/issues/177) | Closed | Historical reminder to retain useful failure events while excluding page/user payloads from logs. Current patch does not implement that request. |

Use full upstream URLs or owner/repository references from a fork. Use related/context links for partial or historical relationships. Reserve automatic issue-closing language for an implemented, tested complete resolution.

## Deferred security work, with conditions

- **Speech privacy:** high priority when microphone input is used. Disclose the external provider boundary; design/test strict on-device behavior and unsupported-language/provider recovery. An offline preference alone is not an enforced boundary. Related: #139.
- **API:** highest conditional security priority before LAN use. Keep off/local defaults; validate authentication, encrypted transport or another explicitly trusted deployment boundary, request budgets and shutdown. Wildcard CORS is not authentication. Related historical context: #298.
- **WebView/IPC:** investigate untrusted frames, navigation races, unauthorized data access and request limits; implement a demonstrated correction without breaking website translation and independently signed clients. Related historical browser context: #158.
- **Backup/storage:** enumerate real eligible data and test backup/restore before selective exclusions or permission changes. Cache documents are already excluded by Android defaults. Related storage request: #44.
- **Native/model supply chain:** release gate for dependency/provenance, malformed-input and actual release-artifact checks; no specific exploit is established by native-code use alone.

No upstream issue/PR should be submitted automatically as part of this queue. Prepare and validate in the RioPlay fork first, credit related work, and reconcile the already open logging contribution. This file is a plan, not evidence that those follow-ups have been implemented.
