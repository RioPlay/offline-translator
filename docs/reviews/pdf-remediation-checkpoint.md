# Native PDF remediation checkpoint

Prepared against DavidVentura/translator-rs `bd32a4f272e239efb6867795c8b7d0c47c619b49` on 2026-10-25. The accompanying patch updates both lopdf manifests, the tracked root Cargo.lock and a parser regression test. `cargo update -p lopdf --precise 0.42.0` succeeded; git revisions stayed pinned.

The standalone host probe in `pdf-probe/` compares exact 0.40.0 and 0.42.0 packages with default features disabled. Both parse the shallow control. A 21,000-byte, 10,380-level synthetic PDF aborts the old version with stack overflow (exit -6); the new version succeeds. Runs used a 30-second timeout, 1 GiB address-space bound and disabled core dumps. See `pdf-probe-results.json`.

This is not a complete translator-pdf/native workspace build, Android build, or real-document regression pass. No app native pin has changed. The app remains affected until patched native source is published and the app bindings lock is updated and built/tested on both ARM targets.

The available connector cannot create the required RioPlay/translator-rs fork; the browser fork route currently requires sign-in. The patch is preserved here for review, not applied to any upstream repository. Do not claim it ships in the app candidate.

Reference: https://rustsec.org/advisories/RUSTSEC-2026-0187.html
