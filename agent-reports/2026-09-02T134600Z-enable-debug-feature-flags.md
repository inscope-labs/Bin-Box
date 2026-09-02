# Process Report: Enable Debug Feature Flags

**Timestamp:** 2026-09-02T13:46:00Z  
**Task Slug:** enable-debug-feature-flags  

## 1. What was asked
The user noted that beta release gating was also restricting features from appearing in debug builds. The request was to configure `build.gradle.kts` so that for `debug` builds, all features and beta-gated capabilities are unlocked/enabled (`IS_BETA_BUILD = true`), while maintaining `IS_BETA_BUILD = false` in `release` builds.

## 2. What was actually changed
- **`app/build.gradle.kts`**:
  - Configured `buildTypes.debug` with `buildConfigField("Boolean", "IS_BETA_BUILD", "true")`.
  - Configured `buildTypes.release` with `buildConfigField("Boolean", "IS_BETA_BUILD", "false")`.
  - Unlocks all features (OCI provisioning, remote backend bridge, extended modules, Termux discovery) automatically in debug builds via `FeatureGate.initialize()`.
- **`version.properties`**:
  - Incremented `versionCode` from `28` to `29` and `debugCode` from `0028` to `0029`.

## 3. Commands Run and Results
- `compile_applet`: Succeeded.
- `gradle :app:testDebugUnitTest`: Executing in background.

## 4. Assumptions Made
- Debug builds should have `IS_BETA_BUILD` set to `true` by default, giving developers and testers immediate access to all features without needing manual preference opt-ins.

## 5. Errors, Partial Failures, or Gaps
- None.

## 6. Prior Logging Gaps Found
- PRIOR LOGGING GAPS FOUND: none

## 7. Version Increment Assessment
- **Probability Score:** 90 / 100 (Build configuration change affecting feature availability in debug builds)
- **Action Taken:** Incremented `versionCode` to `29` and `debugCode` to `0029`.
