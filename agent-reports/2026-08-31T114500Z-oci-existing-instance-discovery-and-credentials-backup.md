# Process Report: OCI Existing Instance Discovery & Credentials Backup UI

**Date**: 2026-08-31T11:45:00Z  
**Task Slug**: `oci-existing-instance-discovery-and-credentials-backup`  

---

## 1. What Was Asked
1. Check whether the OCI provisioning wizard detects existing Always Free VMs before public API key generation / provisioning, and specifically prompts the user to reuse the existing VM or provision a second VM (to support edge cases where app user data was wiped/reinstalled but the cloud VM is already running).
2. Guide the user on how they can use the "Create New Host" link with saved authentication information for an existing Always Free VM.
3. At the end of the provisioning process (Shell Ready step), prompt the user to save and backup the provisioned VM authentication information (username, IP, private key) in a safe place.

---

## 2. What Was Actually Changed

### API & Engine Layer
- **`ComputeApi.kt`**: Added `listInstances(compartmentId, availabilityDomain, displayName, lifecycleState)` calling GET `20160918/instances`.
- **`OciContextDiscovery.kt`**: Added `fetchExistingInstances(client, tenancyOcid, compartmentOcids)` discovering compute instances across compartments and filtering for running/provisioning Always Free VM instances.
- **`OciProvisioningRunner.kt`**: Exposed `discoverExistingInstances(client, tenancyOcid)` to query and return running/provisioning instances.

### Wizard & State Machine Layer
- **`OciOnboardingStage.kt`**: Added `EXISTING_INSTANCE_PROMPT` enum stage.
- **`OciStageMapper.kt`**: Updated `previousStageFor` to map back from `EXISTING_INSTANCE_PROMPT` to `CONNECTION_VERIFICATION`, and from `HOST_CONFIGURATION` to `EXISTING_INSTANCE_PROMPT` when existing instances exist.
- **`OciOnboardingUiState.kt`**: Added `discoveredExistingInstances: List<Instance>`, `vmSshPrivateKey: String?`, and `vmSshUsername: String?`.
- **`OciEnvironmentDiscoveryHandler.kt`**: Updated `discoverContext` to fetch existing compute instances in parallel during context discovery.
- **`OciProvisioningExecutionHandler.kt`**: Created dedicated execution module handling VM SSH key generation, provisioning execution, and host registration.
- **`OciOnboardingViewModel.kt`**: Integrated existing instance routing (advances to `EXISTING_INSTANCE_PROMPT` if instances found), reduced file size from 387 lines to 285 lines (< 300L compliance threshold).

### UI Layer
- **`OciExistingInstanceStage.kt`**: Created dedicated screen displaying discovered existing instances (name, shape, AD, state), explaining how to use "Create New Host" with saved credentials or proceed to provision a second VM.
- **`OciProvisioningStages.kt`**: Enhanced `ShellReadyStage` with a prominent "Save & Backup Connection Credentials" card showing Host IP, Username, and Private Key with one-tap copy and instructions to save securely.
- **`OciOnboardingScreen.kt`**: Wired `EXISTING_INSTANCE_PROMPT` into the animated wizard navigator and passed username/private key to `ShellReadyStage`.

---

## 3. Compliance and Issue Tracking

- **VERSION INCREMENT RULE (AGENTS.md §2)**:
  - **Probability Score**: 95 (> 75)
  - **Action Taken**: Incremented `versionCode` from 19 to 20, and `debugCode` from `0019` to `0020` in `version.properties`.
- **PRIOR LOGGING GAPS FOUND**: none
- **COMPLIANCE CHECK (>180L)**:
  - `OciOnboardingViewModel.kt` (285 lines): PASS (Orchestrator role, < 300 lines).
  - `OciProvisioningStages.kt` (198 lines): PASS (UI stage definitions).
  - `OciEnvironmentDiscoveryHandler.kt` (185 lines): PASS (Single-responsibility discovery module).
- **ISSUES RESOLVED**:
  - `issues/resolved/app_src_main_java_com_inscopelabs_abx_binbox_oci_wizard_OciOnboardingViewModel.kt__FILE-SIZE.md` (Updated and resolved with extraction of `OciProvisioningExecutionHandler.kt`).

---

## 4. Commands Run and Results
- `compile_applet`: Succeeded.
- `gradle :app:testDebugUnitTest`: Succeeded (BUILD SUCCESSFUL in 1m 24s, 33 actionable tasks executed/up-to-date, all unit tests passed).

---

## 5. Assumptions & Verifications
- Assumed standard Oracle Cloud default compute usernames (`ubuntu` for Ubuntu images, `opc` for Oracle Linux).
- Kept the SSH private key in memory in UI state strictly for the duration of the wizard session so the user can copy/backup it before concluding setup.
