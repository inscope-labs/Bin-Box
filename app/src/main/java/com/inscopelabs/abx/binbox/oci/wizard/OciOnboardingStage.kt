package com.inscopelabs.abx.binbox.oci.wizard

/**
 * UI-facing wizard stages, per OCI provisioning doc §9. What the user sees
 * and steps through — distinct from the persisted
 * [com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState] machine
 * underneath it (see that file's kdoc for the split rationale).
 */
enum class OciOnboardingStage {
    WELCOME,
    ACCOUNT_INFORMATION,
    API_KEY_GENERATION,
    API_KEY_REGISTRATION,
    CONNECTION_VERIFICATION,
    OCI_CONTEXT_DISCOVERY,
    HOST_CONFIGURATION,
    NETWORK_PROVISIONING,
    SSH_KEY_GENERATION,
    INSTANCE_PROVISIONING,
    SSH_VERIFICATION,
    HOST_REGISTRATION,
    SHELL_READY
}
