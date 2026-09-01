# Agent Task Report: Revise Size-Triggered Compliance Threshold (AGENTS.md §4.1)

**Timestamp:** `2026-09-01T19:30:00Z`
**Task:** Revisit whether the 180L-check/300L-block file-size rule is too
restrictive and simplify it. Requested outcome: single threshold per file
role, 500 lines for logic files, 1000 lines for UI (Compose) files.

---

## 1. What Was Asked
John asked whether the max-lines-per-file threshold was too restrictive and
whether it could be simplified, then specified the replacement numbers.

## 2. Rationale (why the old rule was worth revisiting)
The old two-tier rule (mandatory compliance check at 180L, hard block at
300L except for restructuring tasks) wasn't preventing the thing it was
meant to prevent: `HostsScreen.kt` (1185L), `TerminalScreen.kt` (1326L),
`BinBoxViewModel.kt` (1031L), and `SettingsScreen.kt` (619L, now resolved —
see below) all sailed past 300 lines without the rule stopping anything.
The "block edits except restructuring tasks" carve-out let agents flag and
defer indefinitely rather than fix, and the two-tier bookkeeping (issue
files, COMPLIANCE-CHECK report lines) added process overhead without
reducing the actual debt. Separately, 300 lines is an especially tight
ceiling for Compose UI screen files specifically, where verbosity
(modifiers, nested layout scopes, previews) is largely intrinsic to
declarative UI rather than a sign of tangled responsibility the way it is
in a ViewModel or repository.

## 3. Change Made
Rewrote AGENTS.md Section 4.1 from a two-severity-tier single threshold
(180L compliance-check / 300L hard block) to a single hard-block threshold
split by file role:
- **UI files** (primary content is `@Composable` screen/component
  functions, typically under `ui/components/` or `ui/screens/`): **1000
  lines**.
- **Logic files** (Orchestrators and Modules per Section 4 — ViewModels,
  Activities, Fragments, Services, UseCases, repositories, provisioners,
  managers): **500 lines**.

Removed the standalone 180L mandatory-compliance-check step entirely (no
replacement — Section 3's logging-gap-on-touch rule and Section 4's
Orchestrator/Module role rule still apply regardless of file size, they
just no longer get a separate numeric trigger). Removed `COMPLIANCE-CHECK`
from the Section 1.1 issue-type example list — with the 180L tier gone,
nothing generates that issue type anymore. Updated Section 4.2's
restructuring target from "≤ 180 lines where reasonably achievable" to
"comfortably under the applicable Section 4.1 threshold for their role...
the goal is genuine single-responsibility separation, not landing just
under the number."

## 4. Immediate Effect on Standing Debt
Checked the four previously-flagged oversized files against the new
thresholds:
- `SettingsScreen.kt` (619L, UI) — now **under** the 1000L UI threshold.
  Resolved the standing `FILE-SIZE` issue for it (moved to
  `issues/resolved/`, fix note added) — no code changed, only the
  threshold changed, and the file is a genuine UI screen file.
- `HostsScreen.kt` (1185L, UI) — still over 1000L, still blocked pending a
  restructuring task.
- `TerminalScreen.kt` (1326L, UI) — still over 1000L, still blocked.
- `BinBoxViewModel.kt` (1031L, logic) — still over its stricter 500L
  threshold, still blocked. This one was never close under either rule and
  is the strongest real restructuring candidate of the four.

## 5. Version Increment Assessment
- **Probability Score (0-100):** `0`
- **Rationale:** Process/documentation change only, no app code touched.
- **Action Taken:** `version.properties` unchanged.

## 6. Compliance & Issue Check
- **PRIOR LOGGING GAPS FOUND:** none (AGENTS.md and issue files are not
  `.kt` source, Section 3 doesn't apply).
- **COMPLIANCE CHECK:** n/a — this task's own change eliminates that
  checkpoint going forward.
- **BLOCKED FILES (over threshold):** `HostsScreen.kt`, `TerminalScreen.kt`
  (UI, still over 1000L), `BinBoxViewModel.kt` (logic, still over 500L) —
  none edited this task, all remain queued for the restructuring task
  John plans to run after AI Studio finishes Local Shell phase 3.

## 7. Files Touched
- Modified: `AGENTS.md` (Section 1.1 issue-type example, Section 4.1
  rewritten, Section 4.2 target line updated)
- Moved: `issues/pending/..._SettingsScreen.kt__FILE-SIZE.md` →
  `issues/resolved/..._SettingsScreen.kt__FILE-SIZE.md` (resolution note
  appended)
- Added: this report
