package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession

/**
 * Pure state transitions for the Host Configuration stage's compartment/AD/shape/image
 * selections. Each function clears the fields that stop being valid once its own selection
 * changes (e.g. picking a new AD invalidates any already-picked shape/image), then defers to
 * [OciProvisioningInvalidation] to also clear any provisioning progress made under the old
 * selection. Module role (see [OciEnvironmentDiscoveryHandler]'s kdoc for the same pattern) —
 * kept out of [OciOnboardingViewModel] so it stays unit-testable without an Application.
 */
class OciHostConfigSelectionHandler {

    /** Returns null if the compartment didn't actually change (no-op). */
    fun selectCompartment(compartmentOcid: String, uiState: OciOnboardingUiState, session: OciProvisioningSession): Pair<OciOnboardingUiState, OciProvisioningSession>? {
        if (compartmentOcid == uiState.context.selectedCompartmentOcid) return null
        val cleared = uiState.copy(
            context = uiState.context.copy(
                selectedCompartmentOcid = compartmentOcid,
                selectedAvailabilityDomain = null,
                selectedShapeName = null,
                selectedImageOcid = null
            ),
            discoveredShapes = emptyList(),
            discoveredImages = emptyList()
        )
        return OciProvisioningInvalidation.clearProvisioningProgress(cleared, session)
    }

    fun selectAvailabilityDomain(ad: String, uiState: OciOnboardingUiState, session: OciProvisioningSession): Pair<OciOnboardingUiState, OciProvisioningSession> {
        val cleared = uiState.copy(
            context = uiState.context.copy(selectedAvailabilityDomain = ad, selectedShapeName = null, selectedImageOcid = null),
            discoveredImages = emptyList()
        )
        if (ad == uiState.context.selectedAvailabilityDomain) return cleared to session
        return OciProvisioningInvalidation.clearProvisioningProgress(cleared, session)
    }

    fun selectShape(shape: String, uiState: OciOnboardingUiState, session: OciProvisioningSession): Pair<OciOnboardingUiState, OciProvisioningSession> {
        val cleared = uiState.copy(context = uiState.context.copy(selectedShapeName = shape, selectedImageOcid = null))
        if (shape == uiState.context.selectedShapeName) return cleared to session
        return OciProvisioningInvalidation.clearProvisioningProgress(cleared, session)
    }

    /** Returns null if the image didn't actually change (no-op). */
    fun selectImage(imageOcid: String, uiState: OciOnboardingUiState, session: OciProvisioningSession): Pair<OciOnboardingUiState, OciProvisioningSession>? {
        if (imageOcid == uiState.context.selectedImageOcid) return null
        val cleared = uiState.copy(context = uiState.context.copy(selectedImageOcid = imageOcid))
        return OciProvisioningInvalidation.clearProvisioningProgress(cleared, session)
    }
}
