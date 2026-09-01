# Agent Task Report: Line Count Rules Verification in AGENTS.md

**Timestamp:** `2026-09-01T21:55:26Z`
**Task:** Check the rules governing maximum line count per file in `AGENTS.md` and report findings.

---

## 1. What Was Asked
- Check the rules governing maximum line count per file in `AGENTS.md` and confirm whether and how they are defined.

## 2. Findings from `AGENTS.md`

The file size and line count rules in `AGENTS.md` are defined under **Section 4.1 (*Size-Triggered Compliance Tiers*)** and **Section 4.2 (*Restructuring Tasks*)**:

### Key Thresholds:
1. **Tier 1: > 180 Lines (Compliance Audit Threshold)**
   - **Rule:** Before any task proceeds on a file > 180 lines, a mandatory compliance check must be executed against Section 3 (Mandatory Logging Standard) and Section 4 (Single-Responsibility Orchestrator vs. Module discipline).
   - **Reporting & Flagging:** The check result must be stated in the agent report under `COMPLIANCE CHECK (>180L): <file> — <pass / gaps found>`. Any gaps found must have discrete issue files created in `issues/pending/` (`LOGGING-GAP` or `COMPLIANCE-CHECK`). Found gaps do not block non-size-related tasks.

2. **Tier 2: > 300 Lines (Hard Scope Exclusion / Blocker Threshold)**
   - **Rule:** Files exceeding 300 lines **MUST NOT** be included in any task's scope — they cannot be read into, edited, or extended — unless the task is explicitly designated as a **restructuring task**.
   - **Non-Restructuring Task Action:** If a task's scope requires touching a file > 300 lines, the agent must stop, not proceed with edits, create a `FILE-SIZE` issue file in `issues/pending/`, and report `BLOCKED — FILE OVER 300L: <file>`.

3. **Restructuring Target: ≤ 180 Lines**
   - **Rule (Section 4.2):** When a restructuring task is executed, the file must be split along Orchestrator/Module role boundaries until all resulting files are **≤ 180 lines where reasonably achievable**.

## 3. Version Increment Assessment
- **Probability Score (0-100):** `0`
- **Rationale:** Informational inquiry reviewing instructions and repository rules. No source code or build configuration modified.
- **Action Taken:** `version.properties` left unchanged (`versionCode=26`, `debugCode=0026`).

## 4. Compliance & Issue Check
- **PRIOR LOGGING GAPS FOUND:** none in touch-set.
- **COMPLIANCE CHECK (>180L):** N/A (informational check on `AGENTS.md`).
- **BLOCKED FILES (>300L):** `app_src_main_java_com_inscopelabs_abx_binbox_ui_components_SettingsScreen.kt__FILE-SIZE.md` remains pending.
