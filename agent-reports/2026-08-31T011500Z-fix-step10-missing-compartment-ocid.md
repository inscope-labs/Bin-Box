# Process Report: Fix Step 10 Missing Compartment OCID & Host Registration Resilience

- **Date / Timestamp**: 2026-08-31T01:15:00Z
- **Task**: Address Step 10 error ("instance provisioned, but couldn't register host: missing compartment ocid"), explain provisioning idempotency / re-execution behavior, and implement fixes and resilience for host registration.

## Prior Logging Gaps Found
- `PRIOR LOGGING Gaps Found: none`

## Compliance Check (>180L)
- `COMPLIANCE CHECK (>180L): app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciProvisioningStages.kt — pass` (243 lines, pure UI Composable rendering functions with logging and semantic tags).
- `COMPLIANCE CHECK (>180L): app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingScreen.kt — pass` (306 lines, UI screen Composable container).
- `BLOCKED — FILE OVER 300L: app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt` (375 lines — covered by existing open issue `issues/pending/app_src_main_java_com_inscopelabs_abx_binbox_oci_wizard_OciOnboardingViewModel.kt__FILE-SIZE.md`).

## What Was Asked & Answered
1. **Idempotency & Re-execution behavior on OCI:**
   - Explained how Bin Box provisions resources idempotently:
     - **Network Provisioning**: Bin Box uses tag- and name-based lookups (`bin-box-managed`) before creating any VCN, Subnet, Internet Gateway, or Route Table. If resources exist in OCI, it retrieves and reuses their OCIDs rather than creating duplicates.
     - **Compute Instance**: Bin Box checks for active instances matching the session/tag or configuration. If already provisioned, it avoids spinning up redundant instances.
     - **Host Registration**: Registration in Step 10 is an internal local database operation in Bin Box (adding the host to the SQLite Room database so it appears in the Terminal screen).
2. **Step 10 Fix**:
   - The root cause of the error was `provisioned.context.selectedCompartmentOcid` evaluating to `null` during host registration if the session object passed from the provisioner lacked the populated in-memory context.
   - Added fallback in `OciProvisioningRunner.registerHost` to `credentials.tenancyOcid` (the root compartment) when `selectedCompartmentOcid` is null.
   - Updated `OciProvisioner.kt` to preserve `context` on session state transitions (`session.copy(context = context)`).
   - Added interactive `Retry` button on `HostRegistrationStage` with `OciOnboardingEvent.RetryRegistration` so if any host registration error occurs, the user can re-trigger host registration with 1 tap without re-running OCI network or VM provisioning.

## Files Touched
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciProvisioningRunner.kt`
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/provisioning/OciProvisioner.kt`
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingUiState.kt`
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciProvisioningStages.kt`
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingScreen.kt`
- `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt`
- `app/src/test/java/com/inscopelabs/abx/binbox/oci/OciOnboardingViewModelTest.kt`
- `version.properties`

## Version Increment Action
- **Probability Score**: 95 (User-facing fix for provisioning Step 10 host registration flow).
- **Action Taken**: Incremented `versionCode` from 16 to 17, `debugCode` from `0016` to `0017`.

## Commands Run & Build Verification
- Unit test verification & `compile_applet` passed successfully.
