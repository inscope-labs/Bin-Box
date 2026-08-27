package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningContext
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState

sealed class OciOnboardingEvent {
    data object GetStarted : OciOnboardingEvent()
    data class SubmitAccountInfo(val tenancyOcid: String, val userOcid: String, val region: String) : OciOnboardingEvent()
    data class ImportConfig(val rawConfig: String) : OciOnboardingEvent()
    data object GenerateApiKey : OciOnboardingEvent()
    data class SubmitFingerprint(val fingerprint: String) : OciOnboardingEvent()
    data object VerifyConnection : OciOnboardingEvent()
    data object GenerateVmSshKey : OciOnboardingEvent()
    data object DiscoverContext : OciOnboardingEvent()
    data class SelectCompartment(val compartmentOcid: String) : OciOnboardingEvent()
    data class SelectAvailabilityDomain(val availabilityDomain: String) : OciOnboardingEvent()
    data class SelectShape(val shape: String) : OciOnboardingEvent()
    data class SelectImage(val imageOcid: String) : OciOnboardingEvent()
    data object StartProvisioning : OciOnboardingEvent()
    data object GoBack : OciOnboardingEvent()
    data object StartOver : OciOnboardingEvent()
    data object EditAccountInfo : OciOnboardingEvent()
    data object Cancel : OciOnboardingEvent()
}

data class OciOnboardingUiState(
    val tenancyOcid: String? = null,
    val userOcid: String? = null,
    val region: String? = null,
    val pendingFingerprint: String? = null,
    val pendingKeyAlias: String? = null,
    val publicKeyPem: String? = null,
    val credentials: OciCredentials? = null,
    val isVerifying: Boolean = false,
    val isGeneratingVmSshKey: Boolean = false,
    val vmSshPublicKey: String? = null,
    val context: OciProvisioningContext = OciProvisioningContext(),
    val isDiscovering: Boolean = false,
    val discoveredCompartments: List<Compartment> = emptyList(),
    val discoveredAvailabilityDomains: List<String> = emptyList(),
    val discoveredShapes: List<String> = emptyList(),
    val discoveredImages: List<Image> = emptyList(),
    val isProvisioning: Boolean = false,
    val provisioningState: OciProvisioningState? = null,
    val provisionedPublicIp: String? = null,
    val error: String? = null,
    val diagnostics: OciVerificationDiagnostics? = null
)
