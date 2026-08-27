# Agent Process Report — Production/Beta Release Model Foundation

- **Timestamp**: `2026-08-26T14:00:00Z`
- **Task Slug**: `release-model-foundation`
- **Assessed Probability Score**: 90 (new source files, new packages, version.properties change — needs a new debug build)
- **Version Action**: `versionCode` 3 -> 4, `debugCode` 0003 -> 0004, `versionName` 1.0.0 -> 0.1.0 (see "What Was Asked" — this was the untouched Android template default, never previously set).
- **Prior Logging Gaps**: `PRIOR LOGGING GAPS FOUND: none` (no existing file with an open LOGGING-GAP issue was in this task's touch-set).

---

## 1. What Was Asked

Establish the foundation for BinBox's production/beta release model: a
fixed 10-stage roadmap (v0.1.0 Core through v1.0.0 Stable), a runtime
feature-flag system gating not-yet-proven stages behind local Beta
Testing enrollment (single AAB, both Google Play testing tracks and
Production track — no build variants), and correct `versionName` to
reflect the actual v0.1.0 starting point.

## 2. Changes Made

### A. New package `core/featureflags/`
- **`ReleaseStage.kt`** (24 lines) — the 10-stage enum, each with its
  target `versionName` and display name.
- **`Feature.kt`** (16 lines) — registry of gate-able features, each
  pinned to the `ReleaseStage` it's introduced at. Populated so far:
  `DIAGNOSTICS_INSPECTOR`, `OCI_EXTENDED_SHELL_HOST`,
  `AI_MCP_SHELL_CLIENT`, `PLUGIN_SCRIPT_INSTALLER`.
- **`BetaEnrollment.kt`** (22 lines) — local SharedPreferences-backed
  opt-in, matching the existing `DiagnosticPreferences` pattern already
  in the codebase (no DataStore dependency introduced).
- **`FeatureFlags.kt`** (29 lines) — `isEnabled(feature, betaEnrolled)`;
  `CURRENT_PRODUCTION_STAGE` is the single constant a future task bumps
  per promotion.

### B. `version.properties`
`versionName` was `1.0.0` — confirmed to be the untouched default the
Android Studio project template ships with, never actually set by any
prior task. Corrected to `0.1.0` per the confirmed release roadmap.
`versionCode`/`debugCode` incremented per Section 2's probability-score
rule.

### C. `AGENTS.md`
- Fixed Section 3's Logger facade reference — it named
  `com.inscopelabs.abx.clipinbox.diagnostics.Logger`, a leftover from
  ClipInBox's AGENTS.md never updated for this repo. Corrected to the
  actual `com.inscopelabs.abx.binbox.core.logging.BinBoxLogger`.
- Added new Section 5 documenting the release model (roadmap, gating
  mechanism, and the rule that `CURRENT_PRODUCTION_STAGE`/`versionName`
  are release decisions, never a side effect of shipping a feature).

### D. `issues/pending/` and `issues/resolved/`
Neither directory existed despite AGENTS.md Section 1.1 mandating them
since this repo's AGENTS.md was created. Created both (with a
`.gitkeep` in `resolved/`, currently empty) and filed 5 `FILE-SIZE`
issues — see Section 3 below.

## 3. COMPLIANCE CHECK (>180L)
All 4 new files are well under 180 lines; no compliance check triggered
for anything added this task.

## 4. BLOCKED — FILE OVER 300L (5 files)

The actual UI wiring — a Beta Testing signup entry point, and gating the
built OCI wizard's entry points behind `Feature.OCI_EXTENDED_SHELL_HOST`
— requires editing:

- `ui/components/SettingsScreen.kt` (560L)
- `ui/components/HostsScreen.kt` (1174L)
- `ui/components/TerminalScreen.kt` (1313L)
- `MainActivity.kt` (372L)
- `oci/wizard/OciOnboardingScreen.kt` (925L)

All five are already over the Section 4.1 300-line hard limit — none of
this is new bloat, all five were already this size before this task.
Per Section 4.1, a non-restructuring task must not edit them. `FILE-SIZE`
issues filed for all five in `issues/pending/`; this task does not
attempt the edits. A dedicated restructuring task (Section 4.2) is a
prerequisite before the gating UI can actually be wired in.

## 5. Assumption Made

The new crash-reporting infrastructure (`GlobalExceptionHandler`,
`UserFacingErrorActivity`, `CrashActivity`, routed by `BuildConfig.DEBUG`
— confirmed by reading `GlobalExceptionHandler.kt` directly, not
guessed) is treated as baseline app robustness, not part of the
`DIAGNOSTICS_INSPECTOR` gated feature — every build needs a safety net
regardless of release stage. `Feature.DIAGNOSTICS_INSPECTOR` is scoped
to user-facing observability surfaces (a Telemetry Inspector screen, any
future remote-reporting opt-in toggle) once those exist in an editable
file. `CrashReporterManager`/`DiagnosticPreferences` currently default
remote reporting to off with no UI toggle yet exposed (none was added by
the `crash-diagnostics-integration` commit), so no gating gap exists
there today.

## 6. Not Yet Done
- Beta Testing signup entry point UI — blocked, see Section 4.
- Gating OCI wizard entry points (FAB, promo banners, quick-action tile,
  Settings section, Terminal dropdown item) behind
  `Feature.OCI_EXTENDED_SHELL_HOST` — blocked, see Section 4.
- `AndroidManifest.xml`/CI workflow implications of the Play testing
  track pipeline — not addressed this task.
