# Native security remediation, 2026-10-25

**Review checkpoint, not a fixed APK or an all-vulnerabilities-cleared claim.**
The app's consolidated branch remains at `5fe6634bd1666dcb15cabce9b19371e25c525a19`.
No upstream repository was modified. Do not merge this older app branch; app changes belong in PR #12.

## Source-level findings

| Component | Evidence and proposed correction | Shipping status |
| --- | --- | --- |
| lopdf 0.40.0 | RUSTSEC-2026-0187; upgrade both engine manifests and root lock to 0.42.0. The saved bounded old/new probe reproduces the old stack overflow on a 21 KB nested PDF. | Prepared locally; current app still uses 0.40.0. |
| MuPDF CFF indexing | CVE-2026-7233. Pinned source lacks vendor fix `611f75f0`. Backport absolute-offset bounds checks. Actual parser regression reproduces a heap-buffer overflow under ASan; patched code rejects both invalid cases and accepts the control. | Local backport; app entry-point reachability of CFF subsetting is not established. |
| MuPDF image arithmetic | CVE-2026-3308. Pin contains `a26f0142` but lacks subsequent fix `a08c5ee1`. Backport checked size arithmetic and input bounds. This includes PDF image code, even when standalone image-document support is disabled. | Local backport; Android integration pending. |
| CLD2 build dependencies | Replace obsolete TOML parser with 0.9 and filename regex with `ends_with(".cc")`; removes regex 0.1, rustc-serialize and thread_local 0.2 from the candidate graph. Preserve the 24 selected C++ files plus wrapper. | 26 isolated host tests pass; Android integration pending. |
| Engine root lock | anyhow 1.0.102 and crossbeam-epoch 0.9.18 have separate findings. Use 1.0.104 and 0.9.20. These old versions appeared in the standalone engine lock, not the reviewed app bindings lock. | Candidate update; distinguish engine development graph from APK contents. |

MuPDF identifies itself as 1.28.0, but the exact submodule revision is an earlier
snapshot. The vendor's release version table alone is insufficient. The source
comparison also confirms that fixes `0f17d789` (CVE-2026-40505), `d4743b60`
(CVE-2026-25556), and the first CVE-2026-3308 fix are already ancestors of the pin.
See `mupdf-source-review.json` for exact commits and comparisons.

## Three distinct verification layers

1. **Dependency inventory:** OSV queried 607 unique registry-version/Git-commit
   requests across the three app locks and the lopdf-patched engine lock. Inputs
   and hashes are recorded in `osv-lock-review.json`. The separately patched
   CLD2 workspace's 20 queries returned no matches. These are lock inventories,
   not selected-feature or APK inventories; maintenance advisories are not CVEs.
2. **Exact source review:** compare the nested MuPDF commit with vendor fixes,
   inspect affected C code and disabled features, and trace ZIP callers. Git
   queries alone returned no warning about the missing MuPDF fixes.
3. **Regression/build verification:** retain old/fixed CFF results, valid controls,
   CLD2 tests, and native PDF test results. Android builds, APK contents and phone
   PDF workflows must still be checked after publishing and repinning sources.

Earlier selected Maven/runtime/desugaring/plugin scans are documented in
`../../security-status.md` on the consolidated app branch. They were not freshly
resolved by this native review. Gradle distribution internals and other native
C/C++ submodules remain outside this pass. No scanner proves absence of unknown
vulnerabilities.

The corrected combined host build passes **72 translator-pdf tests**, including
image translation and the nested-parser regression. The compiled C source hashes
match both MuPDF backports. Four image-stream cases pass after the patch; the
old library accepts three invalid argument cases. CFF runs keep AddressSanitizer
and UBSan enabled; LeakSanitizer is disabled because the host denies its required
`/proc` task access. See `pdf-test-results.json` and both MuPDF probe reports.

Prepared patches: `../../patches/translator-rs-security.patch`,
`../../patches/mupdf-security-backports.patch`, and
`../../patches/cld2-build-dependencies.patch`. The engine patch preserves the old
Git source pins until the dependency forks exist; it does not pretend those
backports are already incorporated. `tested-engine.lock` records the separate
local-override test graph, and is not an installable app lock.

## Remaining findings

- ZIP 2.4.2 remains flagged by RUSTSEC-2026-0367/0368/0369 (fixed in 9.0.0).
  Inspected EPUB/ODT readers and piper-rs Kokoro -> ndarray-npy 0.9.1 use entry
  reads (`by_name`/`by_index`), not `extract`. These filesystem extraction paths
  were not observed in those callers. The dependency is not fixed or suppressed;
  upgrading it requires coordinated translator-doc and ndarray-npy/piper changes.
- Maintenance warnings remain for buf_redux, multipart, paste, rustybuzz, safemem,
  ttf-parser, twoway and host bindgen basic-toml; the standalone engine also has
  instant. Keep these separate from confirmed vulnerability findings.
- Native submodule coverage is incomplete. The targeted MuPDF fixes do not
  constitute an audit of all bundled image/font/model libraries.
- No newly patched Android APK exists. The current test6 app still contains the
  old native revisions. Existing test6 launch/download feedback does not validate
  these proposed PDF fixes, all supported devices, or official-app upgrades.

## Integration gate

Publish reviewed changes in user-owned native repositories, update the MuPDF
wrapper's submodule, repin the engine's MuPDF/CLD2 dependencies, then repin and
relock the app. This involves translator-rs, mupdf-rs, mupdf and rust-cld2.
The connector cannot create repositories; browser sign-in was declined. No
upstream write is authorized. Until accessible forks exist, these patches and
evidence are preserved in the user's app fork without claiming app integration.

After integration, build ARM64 and ARMv7, run native/JVM/format/API/alignment
checks, inspect packaged dependency identities, and test actual PDF import,
translation, image handling, export and malformed-file rejection on devices.
The previous test signing private key is unavailable; a new key cannot silently
replace test6 in place. Official-app replacement additionally requires its
distribution signing key.

## References

- https://rustsec.org/advisories/RUSTSEC-2026-0187.html
- https://mupdf.com/releases/cve
- https://github.com/DavidVentura/mupdf/commit/611f75f0c8657b92460554009196c5ac4b68d909
- https://github.com/DavidVentura/mupdf/commit/a08c5ee1637580658184ab8b1860779ed2e9f73e
- https://rustsec.org/advisories/RUSTSEC-2026-0204.html
- https://rustsec.org/advisories/RUSTSEC-2026-0190.html
- https://rustsec.org/advisories/RUSTSEC-2026-0367.html
- https://rustsec.org/advisories/RUSTSEC-2026-0368.html
- https://rustsec.org/advisories/RUSTSEC-2026-0369.html
