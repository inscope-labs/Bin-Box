# Issue: File Size (> 300 Lines)

- **File Path**: `app/src/main/java/com/inscopelabs/abx/binbox/ui/components/SettingsScreen.kt`
- **Issue Type**: `FILE-SIZE`
- **Reason**: File is 619 lines (exceeds 300-line threshold). Per AGENTS.md Section 4.1 it may not be included in any task's scope except a task explicitly designated a restructuring task. This blocks wiring `BetaEnrollmentCard.kt` (the beta opt-in switch for `FeatureGate`/`BinBoxFeature`, restored on 2026-09-01) into Settings — that wiring is deferred until this file is restructured into Orchestrator + Module pieces.
- **Date Flagged**: 2026-09-01
- **Source Report**: `agent-reports/2026-09-01T190000Z-recovery-restore-and-beta-gating-salvage.md`
