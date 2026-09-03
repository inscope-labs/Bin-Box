# Agent Report — Remove Orphaned shell-standard/shell-extended Directories

**Timestamp:** 2026-09-03T113017Z
**Task:** Housekeeping — the hyphenated `shell-standard/`, `shell-extended/`
directories were left behind after the DFM rename fix (`19823c3`,
`459cf48`). `settings.gradle.kts` references `:shell_standard`/
`:shell_extended` (underscore) only, so these old directories are dead
weight — not part of the Gradle project graph, just clutter.

## Change
`git rm -r shell-standard shell-extended`. No functional change; nothing
references these paths.

## Verification
Confirmed via `git grep` that no build file, workflow, or doc under
version control references the hyphenated paths after removal.
