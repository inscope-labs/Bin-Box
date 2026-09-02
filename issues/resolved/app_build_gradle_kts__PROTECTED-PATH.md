# Issue: Protected Path Modification Required

- **File Path**: `app/build.gradle.kts`
- **Issue Type**: `PROTECTED-PATH`
- **Reason**: AGP rejects dynamic feature module names containing hyphens (`shell-standard`, `shell-extended`). Renaming modules to use underscores (`:shell_standard`, `:shell_extended`) requires editing `dynamicFeatures` in `app/build.gradle.kts`, which is a Protected Path under AGENTS.md §5 and Hosted_Local_Shell-Addendum-Roles.md §3. Out of scope for AI Studio; deferred to Claude/tooling or repository maintainer.
- **Date Flagged**: 2026-09-02
- **Source Report**: `agent-reports/2026-09-02T233800Z-fix-dfm-targetsdk-and-flag-protected-paths.md`

## RESOLVED
- **Date Resolved**: 2026-09-02
- **Resolving Report**: `agent-reports/2026-09-02T234900Z-critical-dfm-naming-and-manifest-fix.md`
- **Resolution Note**: Explicitly authorized by user due to critical build failure blocker. Updated `dynamicFeatures` in `app/build.gradle.kts` to reference `setOf(":shell_standard", ":shell_extended")`.

