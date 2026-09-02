# Agent Task Report: Verification of Updated File Size Thresholds in AGENTS.md

**Timestamp:** `2026-09-01T23:50:37Z`
**Task:** Re-verify `AGENTS.md` following the update to maximum line count rules per file.

---

## 1. What Was Asked
The user noted that `AGENTS.md` had been updated regarding maximum line count rules and requested a re-check.

## 2. Updated Rules Confirmed in `AGENTS.md`

The file size rules have been updated in **Section 4.1 (*Size-Triggered Compliance Threshold*)** and **Section 4.2 (*Restructuring Tasks Are Repository-Wide Compliance Audits*)**:

### Key Changes:
1. **Old System Replaced:**
   - The former two-tier system (180L compliance audit check / 300L hard block ceiling) and the `COMPLIANCE-CHECK` issue type have been removed entirely.

2. **New Role-Based Single Thresholds (§4.1):**
   - **UI files** (files whose primary content is `@Composable` screen or component functions, e.g., `ui/components/`, `ui/screens/`): **1,000 lines**.
   - **Logic files** (everything else — Orchestrators and Modules such as ViewModels, Activities, Fragments, Services, UseCases, repositories, provisioners, managers, etc.): **500 lines**.

3. **Hard Scope Exclusion (§4.1):**
   - Files exceeding their respective threshold (1,000L for UI, 500L for Logic) **must NOT** be included in any task's scope (not read into, edited, or extended) except for an explicit **restructuring task**.
   - Non-restructuring tasks hitting an over-threshold file must stop, create a `FILE-SIZE` issue in `issues/pending/`, and report `BLOCKED — FILE OVER THRESHOLD: <file> (<UI/logic>, <line count>/<threshold>)`.

4. **Restructuring Target (§4.2):**
   - Files undergoing restructuring must be split along Orchestrator/Module role lines until all resulting files are **comfortably under their role threshold (500L for logic, 1000L for UI) where reasonably achievable**.

## 3. Version Increment Assessment
- **Probability Score (0-100):** `0`
- **Rationale:** Informational inquiry and rule verification. No application source code or build configuration changed.
- **Action Taken:** `version.properties` left unchanged (`versionCode=26`, `debugCode=0026`).

## 4. Compliance & Issue Check
- **PRIOR LOGGING GAPS FOUND:** none.
- **PENDING ISSUES:** `issues/pending/` is currently clean (empty).
- **RESOLVED ISSUES:** `issues/resolved/app_src_main_java_com_inscopelabs_abx_binbox_ui_components_SettingsScreen.kt__FILE-SIZE.md` resolved under the new 1,000L UI threshold.
