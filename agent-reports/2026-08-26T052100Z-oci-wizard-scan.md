# Agent Process Report: OCI Provisioning Wizard Scan

- **Timestamp:** 2026-08-26T05:21:00Z
- **Task Slug:** `oci-wizard-scan`
- **Task Query:** Scan the codebase to see why the OCI provision wizard is not user facing

---

## 1. Assessed Probability Score & Version Increment Action
- **Probability Score:** 0 / 100 (Informational scan / discovery query without code changes).
- **Version Action:** No change to `version.properties`.

---

## 2. Prior Logging Gaps & Issue Files
- **PRIOR LOGGING GAPS FOUND:** none in pending issues directory.

---

## 3. Scope & Findings Summary

### Summary of What Was Asked
The user requested a scan of the codebase to determine why the Oracle Cloud Infrastructure (OCI) provisioning wizard is not user facing.

### Investigation & Root Causes Identified
The OCI Provisioning Wizard (`OciOnboardingScreen` and `OciOnboardingViewModel`) is fully implemented in `com.inscopelabs.abx.binbox.oci`, but its discoverability and user-facing integration in the UI have the following architectural and visual limitations:

1. **Only One Obscure Trigger on the Entire Application:**
   - In `HostsScreen.kt` (lines 81–103), the wizard is triggered exclusively by a `SmallFloatingActionButton` stacked directly above the primary Add FAB.
   - It only displays a generic `Icons.Default.Cloud` icon with no textual label or tooltip, making it easy to miss or mistake for a filter or secondary icon.

2. **Absent from the Standard "Add Host" Dialog Flow:**
   - When users tap the prominent `+` (`add_host_fab`) button on the Hosts screen, `AddEditHostDialog` opens.
   - In this dialog, the protocol selectors are restricted to `SSH`, `LOCAL_SHELL`, `WEBSOCKET`, and `DEMO_HOST`. There is no "Oracle Cloud Always Free" option or action button to launch the wizard from within the standard host creation flow.

3. **Absent from Empty State Screens:**
   - When the user has no hosts configured or filter matches zero hosts in `HostsScreen.kt`, the empty state simply displays a static icon and "No host connections match your filter". There is no banner or Call-To-Action (CTA) card to launch the OCI provisioning wizard.

4. **Absent from the Terminal Tab's New Session Flows:**
   - In `TerminalScreen.kt`, tapping the `+` session dropdown (`add_session_button`) presents only:
     - `Cloud Sandbox (Demo SSH)`
     - `Local Device Shell`
     - `Saved Hosts...`
   - In the Terminal's empty state (when no sessions are active), the CTAs are `Launch Demo Host` and `Local Shell`. No OCI provisioning option is presented.

5. **Modal Dialog Embedding instead of Dedicated Navigation/Entry Point:**
   - In `HostsScreen.kt`, the wizard is embedded in a conditional `Dialog` (`if (showOciWizard)`). It is not listed as a route, tab, settings entry, or prominent drawer item.

---

## 4. Files Touched / Modified
- None (Informational scan).
