# Targeted native feature review after publication

This supplements the dependency scan; it does not clear all C/C++ advisories.
App candidate: `fdd72fd0b955a212edb7a7654f1949411d10ddef`.
MuPDF wrapper: `285b012811563c412052df685d2b872c9a95c82c`.
MuPDF: `ba2c748657a4a4c5bd418b684b077b142a61d433`.

## FreeType CVE-2026-23865

The submodule is FreeType 2.13.3, an affected package version. This specific
finding concerns variable-font item-variation parsing. MuPDF's `Makelists`
selects `FT_CONFIG_OPTIONS_H="slimftoptions.h"`; that file explicitly undefines
`TT_CONFIG_OPTION_GX_VAR_SUPPORT`. The affected code is under that feature guard.
This is a configuration-based exclusion for the reviewed bundled MuPDF build,
not a FreeType package upgrade or an exemption for every FreeType CVE. Reassess
if system FreeType or different build features are enabled.

Primary announcement: https://www.openwall.com/lists/oss-security/2026/03/03/8

## HarfBuzz

The MuPDF submodule contains HarfBuzz 6.0.0, but the wrapper's selected features
disable its HTML engine. `mupdf-sys/build.rs` sets the `html` make option from its
own HTML/EPUB features; MuPDF's `Makerules` sets `USE_HARFBUZZ := no` when that
option is disabled, and `Makethird` includes HarfBuzz only when it is enabled.
The engine's separately named HTML feature is not the MuPDF wrapper HTML feature.

Thus a version-only match for GHSA-q4gc-p4hh-3765 or GHSA-p965-5rr7-9mhq does not
establish an affected compiled MuPDF component in this configuration. The app's
Rust shaping libraries are distinct packages and retain their own advisory
review. This does not validate enabling the bundled C HarfBuzz later.

Primary advisories:
- https://github.com/harfbuzz/harfbuzz/security/advisories/GHSA-q4gc-p4hh-3765
- https://github.com/harfbuzz/harfbuzz/security/advisories/GHSA-p965-5rr7-9mhq

## zlib CVE-2026-22184

The submodule reports zlib 1.3.1. This finding concerns the standalone
`contrib/untgz` demonstration utility. MuPDF's explicit `ZLIB_SRC` list compiles
the core compression/decompression files, not that utility. This specific
utility is not introduced into the APK by that source list. Other zlib findings
and the rest of the native dependency tree require their own review.

Primary advisory: https://www.vulncheck.com/advisories/zlib-untgz-global-buffer-overflow-in-tgzfname

## Limits

These conclusions are restricted to the specified findings and selected build
configuration. They do not replace Android artifact inspection, runtime PDF
testing, or a full native submodule audit. ZIP 2.4.2 and maintenance warnings
remain open in the current Cargo scan; no finding was hidden from the reports.
