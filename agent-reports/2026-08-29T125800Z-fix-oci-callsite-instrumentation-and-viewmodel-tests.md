# Process Report: Fix Failing OciCallSiteInstrumentationTest and OciOnboardingViewModelTest

**Timestamp**: 2026-08-29T12:58:00Z
**Task**: Fix failing unit tests in `OciCallSiteInstrumentationTest` and `OciOnboardingViewModelTest`

## 1. What was asked
The user requested to continue and resolve the unexpected test failures encountered during the test execution of `:app:testDebugUnitTest`.

Specifically:
1. `OciCallSiteInstrumentationTest`:
   - `testContextDiscoveryTracesRecordedWithStageAndStep` (ComparisonFailure: expected `discover_availability_domains` / `discover_compartments` but received `unknown` or stale step contexts across pooled OkHttp threads).
   - `testNetworkProvisionerTracesRecordedWithStageAndStep` (ComparisonFailure on step tracing).
2. `OciOnboardingViewModelTest`:
   - `testResumePromptShownForPersistedInProgressSession` (`AssertionError: expected:<HOST_CONFIGURATION> but was:<null>`).

## 2. Root Cause Analysis
1. **Trace Correlation & Step Context across Asynchronous Threads**:
   `OciStepContext` previously relied on an `InheritableThreadLocal`. In OkHttp's pooled dispatcher threads, the `InheritableThreadLocal` values copied during initial worker thread spawn became stale or out of sync with sequential `withStep` invocations running on coroutine dispatchers. Replacing this with an `AtomicReference` context registry in `OciStepContext` ensures accurate, real-time stage/step attribution for synchronous and asynchronous OkHttp interceptor executions.

2. **Moshi Serialization of Error Objects**:
   In `OciErrorCategory.kt`, `OciProvisioningError` had `@Transient val cause: Throwable? = null` without `@Json(ignore = true)`. Data classes `OciProvisioningSession`, `OciProvisioningContext`, and `OciProvisioningError` were also updated with `@JsonClass(generateAdapter = true)` and `@Json(ignore = true)` to ensure clean compile-time Moshi adapter generation via KSP.

3. **Robolectric Static Factory Leaking Across Tests**:
   In `OciOnboardingViewModelTest.kt`, tests were invoking `ViewModelProvider.AndroidViewModelFactory.getInstance(application)`, which caches a static `sInstance` in AndroidX Lifecycle. When running multiple Robolectric tests, the cached singleton retained the `Application` instance from the first executed test method. When later tests saved new sessions to their local test `Application` SharedPreferences, the ViewModel instantiated with the stale `Application` did not see those preferences. Updating tests to instantiate `ViewModelProvider.AndroidViewModelFactory(application)` directly ensures each test method interacts with its isolated application context.

## 3. Files Touched
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/diagnostics/OciStepContext.kt`: Replaced thread-local context with atomic reference and exposed `@PublishedApi` fields for inline `withStep`.
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/provisioning/OciProvisioningSession.kt`: Added `@JsonClass(generateAdapter = true)` for Moshi code generation.
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/provisioning/OciErrorCategory.kt`: Added `@JsonClass(generateAdapter = true)` and `@Json(ignore = true)` for the `cause` parameter.
- `app/src/test/java/com/inscopelabs/abx/binbox/oci/OciOnboardingViewModelTest.kt`: Replaced static factory singleton with test-isolated factory instances.
- `version.properties`: Incremented `versionCode` and `debugCode`.

## 4. Commands Ran & Results
- `gradle :app:testDebugUnitTest`: Ran full unit test suite (79 tests). All 79 tests completed with 0 failures (`BUILD SUCCESSFUL`).
- `compile_applet`: Compilation completed cleanly.

## 5. Compliance & Issues
- `PRIOR LOGGING GAPS FOUND`: none.
- `COMPLIANCE CHECK (>180L)`:
  - `OciStepContext.kt` (59 lines <= 180 lines, compliant).
  - `OciProvisioningSession.kt` (75 lines <= 180 lines, compliant).
  - `OciErrorCategory.kt` (49 lines <= 180 lines, compliant).
  - `OciOnboardingViewModel.kt` has an existing `FILE-SIZE` issue recorded in `issues/pending/app_src_main_java_com_inscopelabs_abx_binbox_oci_wizard_OciOnboardingViewModel.kt__FILE-SIZE.md`. No new logic was added to this file.
- Version Increment Probability: 80% (> 75%) -> `versionCode` incremented 12 -> 13, `debugCode` 0012 -> 0013.
