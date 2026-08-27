# Agent Process Report — Fix OCI Onboarding ViewModel Instantiation Crash

- **Timestamp**: `2026-08-27T08:15:00Z`
- **Task Slug**: `fix-oci-viewmodel-instantiation`
- **Assessed Probability Score**: 95 (Crash fix directly impacting user launch of the OCI onboarding wizard, requiring a new debug build)
- **Version Action**: Incremented `versionCode` (3 -> 4) and `debugCode` (`0003` -> `0004`) in `version.properties`.
- **Prior Logging Gaps**: `PRIOR LOGGING GAPS FOUND: none`

---

## 1. What Was Asked
Investigate and resolve the global crash report:
```
EXCEPTION
Type           : java.lang.RuntimeException
Message        : Cannot create an instance of class com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingViewModel
STACK TRACE
Caused by: java.lang.NoSuchMethodException: com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingViewModel.<init> [class android.app.Application]
```
The crash occurred when users triggered the Oracle Cloud Infrastructure (OCI) Onboarding Wizard from the Hosts Screen (or promo cards/dialogs), where Compose's default `viewModel()` factory attempted reflective instantiation via `AndroidViewModelFactory` which looks specifically for a 1-parameter constructor taking `Application`.

---

## 2. Root Cause Analysis
`OciOnboardingViewModel` extended `AndroidViewModel(application)` with default parameters in its primary constructor:
```kotlin
class OciOnboardingViewModel(
    application: Application,
    private val secureStorage: SecureStorageService = SecureStorageService(application)
) : AndroidViewModel(application)
```
In Kotlin JVM bytecode generation, default constructor arguments do not automatically emit an overloaded single-parameter constructor `(Application)` on the class unless `@JvmOverloads` is explicitly declared. Consequently, `AndroidViewModelFactory`'s reflective lookup `Class.getConstructor(Application::class.java)` threw `NoSuchMethodException`.

---

## 3. Changes Made

### A. ViewModel Constructor Overloads
- **`app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt`**
  - Added `@JvmOverloads constructor(...)` to the primary constructor.
  - This ensures Kotlin generates the necessary public single-argument `(Application)` constructor required by `AndroidViewModelFactory` / `SavedStateViewModelFactory`.

### B. Unit & Robolectric Regression Test Suite
- **`app/src/test/java/com/inscopelabs/abx/binbox/oci/OciOnboardingViewModelTest.kt`**
  - Added `testConstructorWithSingleApplicationParameterExists` to verify reflection-based `(Application)` constructor availability.
  - Added `testViewModelProviderFactoryInstantiation` to verify end-to-end `AndroidViewModelFactory.getInstance(application).create(...)` instantiation without crashing.
  - Added `testGetStartedAdvancesStage` to verify state transitions.

### C. Issue Tracking
- **`issues/pending/app_src_main_java_com_inscopelabs_abx_binbox_oci_wizard_OciOnboardingViewModel.kt__FILE-SIZE.md`**
  - Created issue record documenting that `OciOnboardingViewModel.kt` (523 lines) exceeds the 300-line threshold for future modularization.

---

## 4. Compliance Check (>180L)
- **`COMPLIANCE CHECK (>180L): app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt — gaps found (file size 523L, flagged in issues/pending)`**

---

## 5. Verification & Commands Executed
1. `compile_applet`: **BUILD SUCCESSFUL**.
2. `gradle :app:testDebugUnitTest --tests "com.inscopelabs.abx.binbox.oci.OciOnboardingViewModelTest"`: **BUILD SUCCESSFUL** (100% tests passed).
3. Full suite `gradle :app:testDebugUnitTest`: **BUILD SUCCESSFUL** (all unit and Robolectric tests green).

---

## 6. Assumptions & Limitations
- Assumed standard Compose `viewModel()` factory usage without requiring manual Dagger/Hilt injection for `OciOnboardingViewModel`. Adding `@JvmOverloads` resolves reflective instantiation cleanly across all UI call sites.
