# Native PDF remediation checkpoint

Updated 2026-10-25. See [the expanded review](native-security-2026-10-25/README.md)
for exact sources, patches, remaining findings and reproducible regression evidence.

The local candidate upgrades lopdf to 0.42.0, backports two missing MuPDF vendor
security fixes, modernizes CLD2 build dependencies and updates anyhow/crossbeam
in the engine development lock. The corrected combined host build passes 72
translator-pdf tests with image translation enabled; isolated CLD2 tests pass 26.
CFF ASan/UBSan and image-stream probes compare the old and patched native code.
Compiled MuPDF source hashes were checked; an earlier failed staging copy's
results were discarded.

**Not integrated into the app.** The app native pin and test6 APK remain unchanged.
Publishing the dependency forks, repinning/relocking, both Android ARM builds,
packaged-source verification and real PDF device workflows remain required.
ZIP advisories and broader native coverage remain open; do not claim all CVEs fixed.

The connector cannot create repositories. Native source publication needs
accessible user-owned translator-rs, mupdf-rs, mupdf and rust-cld2 repositories.
No upstream repository was modified. Do not merge this older app branch; app
consolidation remains in PR #12.
