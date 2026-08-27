package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState

/**
 * Maps provisioning session lifecycle states to wizard UI stages and computes back navigation targets.
 */
object OciStageMapper {

    fun stageFor(state: OciProvisioningState): OciOnboardingStage = when (state) {
        OciProvisioningState.NOT_STARTED -> OciOnboardingStage.WELCOME
        OciProvisioningState.ACCOUNT_REQUIRED -> OciOnboardingStage.API_KEY_GENERATION
        OciProvisioningState.API_KEY_REQUIRED -> OciOnboardingStage.API_KEY_REGISTRATION
        OciProvisioningState.API_KEY_REGISTERED -> OciOnboardingStage.CONNECTION_VERIFICATION
        OciProvisioningState.AUTHENTICATION_VERIFIED -> OciOnboardingStage.OCI_CONTEXT_DISCOVERY
        OciProvisioningState.SSH_KEY_READY -> OciOnboardingStage.SSH_KEY_GENERATION
        OciProvisioningState.CONTEXT_DISCOVERED -> OciOnboardingStage.HOST_CONFIGURATION
        OciProvisioningState.NETWORK_CREATING,
        OciProvisioningState.NETWORK_READY -> OciOnboardingStage.NETWORK_PROVISIONING
        OciProvisioningState.INSTANCE_CREATING,
        OciProvisioningState.INSTANCE_PROVISIONING,
        OciProvisioningState.INSTANCE_RUNNING -> OciOnboardingStage.INSTANCE_PROVISIONING
        OciProvisioningState.PUBLIC_IP_DISCOVERED -> OciOnboardingStage.SSH_VERIFICATION
        OciProvisioningState.HOST_REGISTERED -> OciOnboardingStage.HOST_REGISTRATION
        OciProvisioningState.SHELL_READY -> OciOnboardingStage.SHELL_READY
        else -> OciOnboardingStage.OCI_CONTEXT_DISCOVERY
    }

    fun previousStageFor(current: OciOnboardingStage): OciOnboardingStage = when (current) {
        OciOnboardingStage.WELCOME -> OciOnboardingStage.WELCOME
        OciOnboardingStage.ACCOUNT_INFORMATION -> OciOnboardingStage.WELCOME
        OciOnboardingStage.API_KEY_GENERATION -> OciOnboardingStage.ACCOUNT_INFORMATION
        OciOnboardingStage.API_KEY_REGISTRATION -> OciOnboardingStage.API_KEY_GENERATION
        OciOnboardingStage.CONNECTION_VERIFICATION -> OciOnboardingStage.API_KEY_REGISTRATION
        OciOnboardingStage.OCI_CONTEXT_DISCOVERY,
        OciOnboardingStage.HOST_CONFIGURATION -> OciOnboardingStage.CONNECTION_VERIFICATION
        OciOnboardingStage.NETWORK_PROVISIONING,
        OciOnboardingStage.SSH_KEY_GENERATION,
        OciOnboardingStage.INSTANCE_PROVISIONING,
        OciOnboardingStage.SSH_VERIFICATION,
        OciOnboardingStage.HOST_REGISTRATION -> OciOnboardingStage.HOST_CONFIGURATION
        OciOnboardingStage.SHELL_READY -> OciOnboardingStage.SHELL_READY
    }
}
