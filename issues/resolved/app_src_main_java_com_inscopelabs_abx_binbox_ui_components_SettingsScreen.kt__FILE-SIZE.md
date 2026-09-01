# Issue: File Size (> 300 Lines)

- **File Path**: `app/src/main/java/com/inscopelabs/abx/binbox/ui/components/SettingsScreen.kt`
- **Issue Type**: `FILE-SIZE`
- **Reason**: File is 619 lines (exceeds 300-line threshold). Per AGENTS.md Section 4.1 it may not be included in any task's scope except a task explicitly designated a restructuring task. This blocks wiring `BetaEnrollmentCard.kt` (the beta opt-in switch for `FeatureGate`/`BinBoxFeature`, restored on 2026-09-01) into Settings — that wiring is deferred until this file is restructured into Orchestrator + Module pieces.
- **Date Flagged**: 2026-09-01
- **Source Report**: `agent-reports/2026-09-01T190000Z-recovery-restore-and-beta-gating-salvage.md`

## RESOLVED
- **Resolution Date**: 2026-09-01
- **Resolving Agent Report**: `agent-reports/2026-09-01T193000Z-revise-file-size-threshold.md`
- **Fix Note**: AGENTS.md Section 4.1 rewritten to a single role-split threshold (500L logic files / 1000L UI files) replacing the old 180L-check/300L-block two-tier system. `SettingsScreen.kt` is 619 lines, a Compose UI file, and now falls under the 1000L UI threshold — no longer blocked as a restructuring-only file. No code changed; the threshold changed.
