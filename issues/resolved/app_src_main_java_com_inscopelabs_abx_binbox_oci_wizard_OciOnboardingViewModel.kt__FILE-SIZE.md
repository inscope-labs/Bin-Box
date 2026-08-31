# Issue: File Size (> 300 Lines)

- **File Path**: `app/src/main/java/com/inscopelabs/abx/binbox/oci/wizard/OciOnboardingViewModel.kt`
- **Issue Type**: `FILE-SIZE`
- **Reason**: File is 530 lines (exceeds 300-line threshold) and requires future modularization/restructuring into distinct orchestrator and stage modules.
- **Date Flagged**: 2026-08-27
- **Source Report**: `agent-reports/2026-08-27T081500Z-fix-oci-viewmodel-instantiation.md`

## RESOLVED
- **Resolution Date**: 2026-08-31
- **Resolving Agent Report**: `agent-reports/2026-08-31T114500Z-oci-existing-instance-discovery-and-credentials-backup.md`
- **Fix Note**: Extracted `OciProvisioningExecutionHandler.kt` and streamlined event orchestration, bringing `OciOnboardingViewModel.kt` down to 285 lines (< 300L threshold) and strictly fulfilling the Orchestrator role.

