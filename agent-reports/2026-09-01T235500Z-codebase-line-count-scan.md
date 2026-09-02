# Agent Task Report: Codebase Line Count Scan Against AGENTS.md Thresholds

**Timestamp:** `2026-09-01T23:55:00Z`
**Task:** Scan the codebase and identify files that are over the maximum or close to being over the maximum per the updated AGENTS.md §4.1 rules.

---

## 1. What Was Asked
Scan the codebase and report:
- Which files exceed the maximum line count threshold (UI > 1000L, Logic > 500L).
- Which files are close to exceeding their respective threshold.

## 2. Scan Results & Analysis

### A. Files Over the Maximum Threshold (Hard-Blocked from Standard Scope)

#### **UI Files (Threshold: 1,000 Lines)**
1. **`app/src/main/java/com/inscopelabs/abx/binbox/ui/components/TerminalScreen.kt`** — **1,326 lines** (133% of limit)
2. **`app/src/main/java/com/inscopelabs/abx/binbox/ui/components/HostsScreen.kt`** — **1,185 lines** (119% of limit)

#### **Logic / Module / Orchestrator Files (Threshold: 500 Lines)**
1. **`app/src/main/java/com/inscopelabs/abx/binbox/ui/i18n/AppStrings.kt`** — **1,064 lines** (213% of limit)
   - *Role:* i18n data class and multilingual translation map module.
2. **`app/src/main/java/com/inscopelabs/abx/binbox/ui/viewmodel/BinBoxViewModel.kt`** — **1,031 lines** (206% of limit)
   - *Role:* Main application ViewModel / Orchestrator.
3. **`app/src/main/java/com/inscopelabs/abx/binbox/terminal/engine/ShellSession.kt`** — **577 lines** (115% of limit)
   - *Role:* Terminal engine session state machine & buffer module.
4. **`app/src/main/java/com/inscopelabs/abx/binbox/terminal/engine/AnsiParser.kt`** — **524 lines** (105% of limit)
   - *Role:* ANSI escape sequence parsing module.

---

### B. Files Close / Approaching the Maximum Threshold

#### **Approaching UI Limit (1,000 Lines)**
1. **`app/src/main/java/com/inscopelabs/abx/binbox/ui/components/Modals.kt`** — **783 lines** (78% of limit)

#### **Approaching Logic Limit (500 Lines)**
1. **`app/src/main/java/com/inscopelabs/abx/binbox/MainActivity.kt`** — **419 lines** (84% of limit)
   - *Role:* Root Activity orchestrating bottom navigation, intent handling, and permission lifecycles.
2. **`app/src/main/java/com/inscopelabs/abx/binbox/terminal/model/TerminalModels.kt`** — **343 lines** (69% of limit)
3. **`app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciProvisioningStages.kt`** — **328 lines** (66% of limit)
4. **`app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciEarlyStages.kt`** — **300 lines** (60% of limit)
5. **`app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt`** — **288 lines** (58% of limit)
6. **`app/src/main/java/com/inscopelabs/abx/binbox/terminal/engine/TerminalSessionManager.kt`** — **281 lines** (56% of limit)

---

## 3. Version Increment Assessment
- **Probability Score (0-100):** `0`
- **Rationale:** Diagnostic analysis and codebase inventory scan only. No application source code or build configuration changed.
- **Action Taken:** `version.properties` remains unchanged (`versionCode=26`, `debugCode=0026`).

## 4. Compliance & Issue Check
- **PRIOR LOGGING GAPS FOUND:** none in touch-set.
- **TOUCHED FILES:** None edited (only scan performed).
