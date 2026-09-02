# Agent Report — Fix DFM TargetSdk and Flag Protected Paths

**Timestamp:** 2026-09-02T233800Z
**Task:** Diagnose build failure (`Unresolved reference 'targetSdk'` in `shell-extended/build.gradle.kts`) and fix it.

## 1. What Was Asked
The user provided a build error log from CI/Gradle:
```
> Configure project :shell-extended
e: file:///home/runner/work/Bin-Box/Bin-Box/shell-extended/build.gradle.kts:24:5: Unresolved reference 'targetSdk'.
```
and asked: "What happened here? Please fix it."

## 2. Root Cause Analysis
Two distinct issues were diagnosed:

1. **Direct Cause of Reported Error (`Unresolved reference 'targetSdk'`):**
   In Android Gradle Plugin (AGP), modules using the `com.android.dynamic-feature` plugin configure a `DynamicFeatureExtension` whose `defaultConfig` block (`DynamicFeatureDefaultConfig`) does NOT support `targetSdk`. Dynamic Feature Modules (DFMs) are packaged as split APKs of the base application module (`:app`), and AGP requires `targetSdk` to be defined solely on the base application. Both `shell-extended/build.gradle.kts` and `shell-standard/build.gradle.kts` incorrectly contained `targetSdk = 36` inside `defaultConfig`.

2. **Downstream Blocker (`invalid characters in feature module names`):**
   Once `targetSdk = 36` was removed, the build proceeded through Kotlin compilation and reached task `:app:generateDebugFeatureMetadata`, where AGP threw:
   ```
   Execution failed for task ':app:generateDebugFeatureMetadata'.
   > A failure occurred while executing com.android.build.gradle.internal.tasks.featuresplit.FeatureSetMetadataWriterTask$FeatureSetRunnable
      > The following feature module names contain invalid characters. Feature module names can only contain letters, digits and underscores.
        	-> shell-standard
        	-> shell-extended
   ```
   AGP strictly forbids hyphens (`-`) in dynamic feature module names. Dynamic feature names must be alphanumeric and underscore only (e.g., `shell_standard` and `shell_extended`).
   Remediating this requires renaming the module references in `settings.gradle.kts` and `app/build.gradle.kts`.

## 3. What Was Actually Changed

- **`shell-extended/build.gradle.kts`**:
  - Removed `targetSdk = 36` from `defaultConfig`.
- **`shell-standard/build.gradle.kts`**:
  - Removed `targetSdk = 36` from `defaultConfig`.
- **`issues/pending/settings_gradle_kts__PROTECTED-PATH.md`**:
  - Created issue flagging `settings.gradle.kts` modification as required but blocked per AGENTS.md §5.
- **`issues/pending/app_build_gradle_kts__PROTECTED-PATH.md`**:
  - Created issue flagging `app/build.gradle.kts` modification as required but blocked per AGENTS.md §5.

## 4. Role Boundaries and Protected Paths Audit (AGENTS.md §5)
Renaming the DFM modules from hyphenated (`:shell-standard`, `:shell-extended`) to underscored (`:shell_standard`, `:shell_extended`) requires editing:
- `settings.gradle.kts` (`include(":shell_standard")`, `include(":shell_extended")`)
- `app/build.gradle.kts` (`dynamicFeatures += setOf(":shell_standard", ":shell_extended")`)

Both files are explicitly designated **Protected Paths** in `AGENTS.md` §5 and `Hosted_Local_Shell-Addendum-Roles.md` §3 ("never edit, even incidentally as part of a larger task's scope... If a task's scope appears to require touching one of these, stop and create a FILE-SIZE-style issue file flagging it as blocked, rather than proceeding").

Per this mandate, AI Studio stopped, did not modify `settings.gradle.kts` or `app/build.gradle.kts`, and flagged discrete issue files in `issues/pending/`. This change must be applied by Claude / tooling or the human maintainer outside AI Studio's sandbox.

## 5. Compliance Checklist

- **VERSION INCREMENT RULE (AGENTS.md §2)**:
  - **Assessed Probability Score**: 0 / 100
  - **Action Taken**: None. The build cannot successfully output a debug APK until the protected path modifications (`settings.gradle.kts` and `app/build.gradle.kts`) are applied externally. Additionally, `version.properties` is designated a Protected Path under AGENTS.md §5.
- **PRIOR LOGGING GAPS FOUND**: none (`issues/pending/` had no prior logging gaps).
- **SINGLE-RESPONSIBILITY & LINE COUNT**:
  - `shell-extended/build.gradle.kts`: 35 lines (< 500L threshold)
  - `shell-standard/build.gradle.kts`: 33 lines (< 500L threshold)

## 6. Commands Run and Results
- `compile_applet`:
  - Initial run: reproduced `Unresolved reference 'targetSdk'` on `:shell-extended`.
  - Second run (post-fix): script evaluation passed; revealed downstream failure in `:app:generateDebugFeatureMetadata` regarding hyphenated module names (`shell-standard`, `shell-extended`).
