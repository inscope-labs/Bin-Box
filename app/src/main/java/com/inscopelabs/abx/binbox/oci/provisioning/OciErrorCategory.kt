package com.inscopelabs.abx.binbox.oci.provisioning

/**
 * Categorized provisioning errors, per OCI provisioning doc §33.
 *
 * Kept as its own enum rather than folded into [com.inscopelabs.abx.binbox.core.error.AppError]
 * because these categories map to *user-facing wizard recovery actions*
 * (§35: retry, reconfigure, switch region) rather than to the general
 * app-wide error taxonomy — a QUOTA_ERROR and a CAPACITY_ERROR are both
 * "the request was rejected" at the AppError level, but need different
 * copy and different retry affordances in the wizard.
 */
enum class OciErrorCategory {
    ACCOUNT_ERROR,
    AUTHENTICATION_ERROR,
    PERMISSION_ERROR,
    REGION_ERROR,
    COMPARTMENT_ERROR,
    NETWORK_ERROR,
    QUOTA_ERROR,
    CAPACITY_ERROR,
    COMPUTE_ERROR,
    SSH_ERROR,
    TRANSPORT_ERROR,
    TIMEOUT_ERROR,
    USER_CANCELLED,
    UNKNOWN_ERROR
}

/**
 * A categorized provisioning failure with the four pieces of context §33
 * requires every error to carry: what happened, why, what the user can do,
 * and whether retry applies.
 */
data class OciProvisioningError(
    val category: OciErrorCategory,
    val whatHappened: String,
    val whyItHappened: String? = null,
    val whatUserCanDo: String? = null,
    val retryable: Boolean = false,
    // Excluded from persistence: this is nested inside OciProvisioningSession,
    // which OciProvisioningRepository serializes with Moshi's reflection
    // adapter — Throwable has no Moshi adapter and would fail at save time.
    @Transient val cause: Throwable? = null
)
