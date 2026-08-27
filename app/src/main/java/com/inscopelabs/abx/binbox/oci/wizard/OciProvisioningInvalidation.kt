package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningContext
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState

/**
 * Pure state transitions for "the user changed something earlier in the wizard, so
 * everything downstream of that change is no longer trustworthy." Kept separate from
 * [OciOnboardingViewModel] (module role, not orchestrator) so it stays unit-testable without
 * an Application/ViewModelScope, and so the orchestrator doesn't have to carry this logic
 * inline on top of everything else it already coordinates.
 *
 * Network/compute provisioning is idempotent per compartment (discover-or-create + a
 * session-stable retry token — see [com.inscopelabs.abx.binbox.oci.provisioning.NetworkProvisioner]
 * and [com.inscopelabs.abx.binbox.oci.provisioning.ComputeProvisioner]'s kdocs), so clearing
 * progress here and letting the wizard re-provision under a changed selection never creates
 * duplicate infrastructure.
 */
object OciProvisioningInvalidation {

    /** Compartment/AD/shape/image changed. Clears any in-flight or completed provisioning
     * progress tied to the old selection. */
    fun clearProvisioningProgress(uiState: OciOnboardingUiState, session: OciProvisioningSession): Pair<OciOnboardingUiState, OciProvisioningSession> {
        if (uiState.provisioningState == null && uiState.provisionedPublicIp == null && session.vcnOcid == null && session.instanceOcid == null) {
            return uiState to session
        }
        val newUiState = uiState.copy(provisioningState = null, provisionedPublicIp = null, isProvisioning = false, error = null)
        val newSession = session.copy(
            vcnOcid = null, internetGatewayOcid = null, routeTableOcid = null, subnetOcid = null,
            instanceOcid = null, publicIp = null, registeredShellHostId = null
        ).advance(OciProvisioningState.CONTEXT_DISCOVERED)
        return newUiState to newSession
    }

    /** OCI account (tenancy/user/region) changed. Clears the discovered environment and any
     * host-configuration selections made under the old account, plus anything downstream. */
    fun clearFromAccountChange(uiState: OciOnboardingUiState, session: OciProvisioningSession): Pair<OciOnboardingUiState, OciProvisioningSession> {
        val newUiState = uiState.copy(
            diagnostics = null,
            discoveredCompartments = emptyList(),
            discoveredAvailabilityDomains = emptyList(),
            discoveredShapes = emptyList(),
            discoveredImages = emptyList(),
            context = OciProvisioningContext(),
            provisioningState = null,
            provisionedPublicIp = null,
            isProvisioning = false
        )
        val newSession = session.copy(
            vcnOcid = null, internetGatewayOcid = null, routeTableOcid = null, subnetOcid = null,
            instanceOcid = null, publicIp = null, registeredShellHostId = null,
            context = OciProvisioningContext()
        )
        return newUiState to newSession
    }
}
