# Issue: File Size (> 300 Lines)

- **File Path**: `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt`
- **Issue Type**: `FILE-SIZE`
- **Reason**: File is 530 lines (exceeds 300-line threshold) and requires future modularization/restructuring into distinct orchestrator and stage modules.
- **Date Flagged**: 2026-08-27
- **Source Report**: `agent-reports/2026-08-27T081500Z-fix-oci-viewmodel-instantiation.md`

## RESOLVED
- **Resolution Date**: 2026-08-27
- **Resolving Agent Report**: `agent-reports/2026-08-27T140000Z-oci-wizard-assisted-navigation-diagnostics.md`
- **Fix Note**: Restructured `OciOnboardingViewModel.kt` and `OciOnboardingScreen.kt` into single-responsibility orchestrators and discrete domain modules (`OciConfigParser.kt`, `OciRegionHelper.kt`, `OciVerificationDiagnostics.kt`, `OciOnboardingUiState.kt`, `OciWizardComponents.kt`, `OciEarlyStages.kt`, `OciVerificationStages.kt`, `OciProvisioningStages.kt`). All resulting files are compliant with AGENTS.md rules.
