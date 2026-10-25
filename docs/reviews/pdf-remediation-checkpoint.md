# Native PDF remediation checkpoint

The fixes are now published in RioPlay's native forks and integrated into [Android draft PR #12](https://github.com/RioPlay/offline-translator/pull/12). The previous "local only / not integrated" checkpoint is superseded.

**[Start with the maintainer handoff](native-security-2026-10-25/maintainer-handoff.md).** It contains implementation guidance, test commands, evidence, outstanding limits and the coordinated upstream review plan.

At app commit d77b0062a440e7abc1036ef8b5e7ac39f76afa79, both ARM64 and ARMv7 CI builds pass, including JVM tests, formatting, release packaging, native compatibility and patched-source checks. Published native source passes 72 PDF host tests plus 244 other host library tests. Earlier isolated CLD2 validation passed 26 tests.

CI artifacts are unsigned. Phone PDF workflows and retained-data upgrades remain pending; test6 predates these fixes and its private signing key is unavailable. ZIP advisories and broader native security coverage remain open.

This branch preserves evidence only; do not merge its older app snapshot. Implementation lives in the five linked fork drafts. No upstream repository has been modified, and upstream submission requires RioPlay's explicit approval.
