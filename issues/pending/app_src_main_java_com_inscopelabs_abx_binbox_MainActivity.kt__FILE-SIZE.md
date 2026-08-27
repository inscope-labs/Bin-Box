# FILE-SIZE Issue

- **Full file path**: `app/src/main/java/com/inscopelabs/abx/binbox/MainActivity.kt`
- **Issue type**: FILE-SIZE
- **Line count at time of flagging**: 372 lines (limit: 300)
- **Reason**: This file is a required touch-point for the production/beta
  release-model integration (Feature-Flag gating and/or Beta Testing
  entry-point UI must be added here), but AGENTS.md Section 4.1 blocks any
  non-restructuring task from editing a file already over 300 lines. This
  file must be split along Orchestrator/Module lines (Section 4.2) before
  the release-model gating can be wired into it.
- **Date flagged**: 2026-08-26
- **Source agent-report**: agent-reports/2026-08-26T140000Z-release-model-foundation.md
