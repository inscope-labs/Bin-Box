# FILE-SIZE Issue

- **Full file path**: `app/src/main/java/com/inscopelabs/abx/binbox/ui/components/SettingsScreen.kt`
- **Issue type**: FILE-SIZE
- **Line count at time of flagging**: 560 lines (limit: 300)
- **Line count after explicit override**: 671 lines — John explicitly directed wiring the Diagnostics package into this file before the restructuring pass (see agent-reports/2026-08-26T150000Z-wire-diagnostics-package.md). This is a deliberate, human-authorized exception to Section 4.1, not a violation slipping through — the restructuring task below still needs to happen, and now has more to split.
- **Reason**: Beta Testing enrollment toggle + gated Diagnostics & Telemetry section were added directly to this file per explicit instruction. Still must be split along Orchestrator/Module lines (Section 4.2) — nothing about the override changes that.
- **Date flagged**: 2026-08-26 (updated same day after override)
- **Source agent-report**: agent-reports/2026-08-26T150000Z-wire-diagnostics-package.md

---

## Resolution (2026-08-26)

Split into an Orchestrator + 10 Module files under
`ui/components/settings/`. `SettingsScreen.kt` is now 155 lines (was
671). All 10 new Module files are ≤ 170 lines. See
`agent-reports/2026-08-26T160000Z-restructure-settings-screen.md` for
the full restructuring audit.
