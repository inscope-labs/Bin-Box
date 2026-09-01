# Agent Task Report: Recovery from Destructive Beta-Gating Commit & Additive Salvage

**Timestamp:** `2026-09-01T19:00:00Z`
**Task:** Recover `main` after commits `fe9157e`..`68d1ca3` deleted ~130 source files
(entire OCI provisioning subsystem, SSH/WebSocket transport, Room database layer,
secure storage, i18n, Keys/Snippets screens, most tests), deleted the CI workflow
(`.github/workflows/build-apk-debug.yml`), and deleted the Gradle wrapper, while
attempting to implement a core-vs-beta feature distribution system. Then preserve
the legitimate work from that same window without reintroducing the deletions.

---

## 1. What Was Asked
- Assess the broken build (done in a separate conversational report, not committed).
- Revert `main` to the last known-good, build-verified commit (`df52cbf`).
- Do NOT lose the beta-gating work in the process — salvage what's genuinely reusable.

## 2. Root Cause (for the record)
`fe9157e` ("refactor: simplify project configuration and core") deleted the entire
existing feature set instead of gating it behind the `FeatureGate` system it
introduced. The final commit `68d1ca3` ("chore: ignore Gradle build cache files")
additionally deleted `.github/workflows/build-apk-debug.yml` and the Gradle wrapper.
`.github/workflows/*.yml`, `app/build.gradle.kts`, `AndroidManifest.xml`,
`proguard-rules.pro`, and `version.properties` are all protected paths per the
(not-yet-committed) `Hosted_Local_Shell-Addendum-Roles.md` role split — this task
is exactly the kind of protected-path recovery that split calls for.

## 3. Actions Taken
1. `git reset --hard df52cbf && git push --force-with-lease origin main` — done by
   John directly from Termux; `origin/main` confirmed back at `df52cbf` with zero
   diff against it.
2. Identified 5 commits between `df52cbf` and `fe9157e` that were purely additive
   (no deletions of existing functionality) and cherry-picked them cleanly onto the
   restored `main`:
   - `fe97d50` feat: add secure host deletion confirmation
   - `99654cd` build: increment version and debug codes to 22
   - `3a4aa0e` feat: implement local shell infrastructure (the real Local Shell v2
     work: 3-tier manifests, `PlayFeatureModuleManager`, `LocalShellProvider`,
     `TierManifestTest`)
   - `1f09dd6` feat: add local shell package management (module-management UI:
     `LocalShellModulesSheet`, `TierModuleCard`)
   - `96cd630` chore: fix unit test runner and update version
3. Manually re-added (as new, additive files — not a merge of the destructive
   commit) the genuinely reusable beta-gating scaffold from `68d1ca3`, verified
   self-contained against the full (non-stub) codebase's existing symbols
   (`BinBoxLogger`, theme colors) before adding:
   - `core/distribution/FeatureGate.kt` (`BinBoxFeature` enum, `FeatureTier`,
     `FeatureGate` object — OCI/remote-backend/extended-modules/Termux features
     default to `BETA` tier; core shell/MCP features default to `PRODUCTION`)
   - `mcp/client/McpClient.kt`, `mcp/client/McpClientStub.kt`, `mcp/model/McpModels.kt`
   - `ui/components/BetaEnrollmentCard.kt`
   - `test/.../FeatureGateTest.kt`, `test/.../McpClientStubTest.kt`
4. Added `buildConfigField("Boolean", "IS_BETA_BUILD", "false")` to
   `app/build.gradle.kts` `defaultConfig` (required by `FeatureGate`, previously
   only existed in the deleted-and-recreated stub `build.gradle.kts`).
5. Wired `FeatureGate.initialize(this)` into `BinBoxApplication.onCreate()`.
6. Did **not** wire `BetaEnrollmentCard` into `SettingsScreen.kt` — that file is
   619 lines, over the AGENTS.md Section 4.1 300-line threshold, and per that
   section may not be edited except as an explicitly-designated restructuring
   task. Filed `issues/pending/..._SettingsScreen.kt__FILE-SIZE.md` instead.
7. Did **not** gate any existing OCI/SSH/WebSocket entry points behind
   `FeatureGate.isEnabled(...)` — those live in `HostsScreen.kt` (1185 lines),
   `TerminalScreen.kt` (1326 lines), and `BinBoxViewModel.kt` (1031 lines), all
   likewise over the restructuring threshold. The `BinBoxFeature` enum values for
   those features exist and are correctly tiered `BETA`, but nothing in the UI
   currently checks them yet — this is scaffolding, not yet enforcement.

## 4. Version Increment Assessment
- **Probability Score (0-100):** `20`
- **Rationale:** Restores and re-adds previously-implemented functionality;
  introduces one small new `BuildConfig` field and one new `Application.onCreate`
  call, no new user-facing behavior yet (gating isn't wired into any screen).
- **Action Taken:** `version.properties` left as restored from `df52cbf`
  (`versionCode`/`debugCode` as of that commit) — not incremented further.

## 5. Compliance & Issue Check
- **PRIOR LOGGING GAPS FOUND:** none.
- **COMPLIANCE CHECK (>180L):** `FeatureGate.kt` (89 lines, PASS), `BetaEnrollmentCard.kt`
  (~150 lines, PASS), `McpClientStub.kt` (~65 lines, PASS). `BinBoxApplication.kt`
  touched but net +2 lines, no new logic beyond the added init call.
- **BLOCKED FILES (>300L):** `SettingsScreen.kt` (619L), `HostsScreen.kt` (1185L),
  `TerminalScreen.kt` (1326L), `BinBoxViewModel.kt` (1031L) — none edited this task;
  `SettingsScreen.kt` issue filed per Section 1.1, the other three are pre-existing
  known oversized files not newly touched here.

## 6. Files Touched
- Modified: `app/build.gradle.kts`, `app/src/main/java/.../BinBoxApplication.kt`
- Added: `core/distribution/FeatureGate.kt`, `mcp/client/McpClient.kt`,
  `mcp/client/McpClientStub.kt`, `mcp/model/McpModels.kt`,
  `ui/components/BetaEnrollmentCard.kt`, `test/.../FeatureGateTest.kt`,
  `test/.../McpClientStubTest.kt`,
  `issues/pending/..._SettingsScreen.kt__FILE-SIZE.md`,
  this report.

## 7. Not Yet Verified
No Android SDK / Gradle available in the recovery environment, so no
`assembleDebug`/`testDebugUnitTest` run was possible here. All added files were
checked by hand against every existing symbol they reference (imports resolved
against the full `df52cbf`-based tree, not the stub tree they were originally
written against). Recommend a real `gradle :app:testDebugUnitTest` run on next
touch to confirm.
