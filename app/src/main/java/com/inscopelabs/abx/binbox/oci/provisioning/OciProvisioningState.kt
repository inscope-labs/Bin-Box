package com.inscopelabs.abx.binbox.oci.provisioning

/**
 * Resumable provisioning states, per OCI provisioning doc §31.
 *
 * This is the persisted state machine — distinct from the wizard's UI-facing
 * stage sequence (§9, [com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingStage]).
 * Multiple wizard stages can correspond to work happening within a single
 * state here; the wizard stage is what the user sees, this is what
 * [OciProvisioningSession] persists to survive process death (§32).
 */
enum class OciProvisioningState {
    NOT_STARTED,

    ACCOUNT_REQUIRED,
    API_KEY_REQUIRED,
    API_KEY_REGISTERED,
    AUTHENTICATION_VERIFIED,

    CONTEXT_DISCOVERED,

    NETWORK_DISCOVERED,
    NETWORK_CREATING,
    NETWORK_READY,

    SHAPE_SELECTED,

    SSH_KEY_READY,

    INSTANCE_CREATING,
    INSTANCE_PROVISIONING,
    INSTANCE_RUNNING,

    PUBLIC_IP_DISCOVERED,
    SSH_VERIFYING,
    SSH_READY,

    HOST_REGISTERED,
    SHELL_READY,
    COMPLETED,

    // --- Failure states ---
    AUTH_FAILED,
    NETWORK_FAILED,
    QUOTA_FAILED,
    CAPACITY_UNAVAILABLE,
    INSTANCE_FAILED,
    SSH_FAILED,
    TIMEOUT,
    CANCELLED;

    val isFailure: Boolean
        get() = this in FAILURE_STATES

    val isTerminal: Boolean
        get() = this == COMPLETED || isFailure

    companion object {
        private val FAILURE_STATES = setOf(
            AUTH_FAILED, NETWORK_FAILED, QUOTA_FAILED, CAPACITY_UNAVAILABLE,
            INSTANCE_FAILED, SSH_FAILED, TIMEOUT, CANCELLED
        )
    }
}
