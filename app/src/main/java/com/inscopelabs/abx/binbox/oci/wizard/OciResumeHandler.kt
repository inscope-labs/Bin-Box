package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningContext
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState

/**
 * Module supporting resuming a persisted, in-progress [com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession]
 * (§32) after the process was killed or the app relaunched mid-wizard. Only the selected OCIDs
 * are persisted on the session — human-readable discovered lists (compartment/AD/shape/image
 * names) are not — so those need a fresh live fetch; existing selections are preserved rather
 * than cleared while that happens.
 */
class OciResumeHandler(
    private val discoveryHandler: OciEnvironmentDiscoveryHandler,
    private val keyRepository: IKeyRepository
) {
    /** Provisioning states meaning [com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioner.provision]
     * started but never reached [OciProvisioningState.PUBLIC_IP_DISCOVERED] — safe to re-invoke on resume. */
    val inProgressStates: Set<OciProvisioningState> = setOf(
        OciProvisioningState.SHAPE_SELECTED,
        OciProvisioningState.SSH_KEY_READY,
        OciProvisioningState.NETWORK_DISCOVERED,
        OciProvisioningState.NETWORK_CREATING,
        OciProvisioningState.NETWORK_READY,
        OciProvisioningState.INSTANCE_CREATING,
        OciProvisioningState.INSTANCE_PROVISIONING,
        OciProvisioningState.INSTANCE_RUNNING
    )

    suspend fun fetchVmSshPublicKey(sshKeyAlias: String?): String? =
        sshKeyAlias?.toLongOrNull()?.let { keyRepository.getKeyById(it)?.publicKey }

    /** Full resume orchestration: restores the VM SSH key, re-fetches the discovered
     * environment lists for whatever [session] already had selected, then continues
     * provisioning/registration if either was left mid-flight. Takes callbacks rather than
     * owning [kotlinx.coroutines.flow.MutableStateFlow]/`viewModelScope` directly so this stays
     * a plain module the orchestrator can unit-test around. */
    suspend fun resume(
        session: OciProvisioningSession,
        credentials: OciCredentials?,
        currentPublicKeyPem: String?,
        targetStage: OciOnboardingStage,
        updateUiState: ((OciOnboardingUiState) -> OciOnboardingUiState) -> Unit,
        onProvisioningInProgress: suspend () -> Unit,
        onProvisioningComplete: suspend () -> Unit
    ) {
        fetchVmSshPublicKey(session.sshKeyAlias)?.let { key -> updateUiState { it.copy(vmSshPublicKey = key) } }

        if (credentials != null && targetStage.ordinal >= OciOnboardingStage.OCI_CONTEXT_DISCOVERY.ordinal) {
            rediscoverEnvironment(
                client = OciClient(credentials.region) { credentials },
                credentials = credentials,
                currentPublicKeyPem = currentPublicKeyPem,
                context = session.context,
                onCompartments = { compartments, ads ->
                    updateUiState {
                        it.copy(
                            discoveredCompartments = compartments,
                            discoveredAvailabilityDomains = ads,
                            context = it.context.copy(availableCompartmentOcids = compartments.map { c -> c.id }, availabilityDomains = ads)
                        )
                    }
                },
                onShapes = { shapes -> updateUiState { it.copy(discoveredShapes = shapes) } },
                onImages = { imgs -> updateUiState { it.copy(discoveredImages = imgs) } },
                onError = { err, diag -> updateUiState { it.copy(error = err, diagnostics = diag) } }
            )
        }

        when {
            session.state in inProgressStates -> onProvisioningInProgress()
            session.state == OciProvisioningState.PUBLIC_IP_DISCOVERED && credentials != null -> onProvisioningComplete()
        }
    }

    /** Re-fetches compartments/ADs, then shapes/images for whatever was already selected in
     * [context]. Read-only — never advances the wizard stage or persists session state, since
     * that's the orchestrator's call once the whole resume has settled. */
    private suspend fun rediscoverEnvironment(
        client: OciClient,
        credentials: OciCredentials,
        currentPublicKeyPem: String?,
        context: OciProvisioningContext,
        onCompartments: (List<Compartment>, List<String>) -> Unit,
        onShapes: (List<String>) -> Unit,
        onImages: (List<Image>) -> Unit,
        onError: (String, OciVerificationDiagnostics) -> Unit
    ) {
        // discoverContext is fully suspend (no internal fire-and-forget launch), so onCompartments
        // has already run by the time this call returns — safe to chain the shape/image fetches
        // sequentially below rather than nesting them inside the callback.
        discoveryHandler.discoverContext(client, credentials, currentPublicKeyPem, onSuccess = onCompartments, onError = onError)

        val compId = context.selectedCompartmentOcid
        val ad = context.selectedAvailabilityDomain
        if (compId != null && ad != null) {
            discoveryHandler.fetchShapes(client, compId, ad, onSuccess = onShapes, onError = { })
            context.selectedShapeName?.let { shape ->
                discoveryHandler.fetchImages(client, compId, shape, onSuccess = onImages, onError = { })
            }
        }
    }
}
