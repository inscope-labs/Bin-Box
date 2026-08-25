package com.inscopelabs.abx.binbox.oci.provisioning

/**
 * Result type for the provisioning engine (NetworkProvisioner,
 * ComputeProvisioner, OciProvisioner, ProvisioningPoller).
 *
 * NOT [com.inscopelabs.abx.binbox.core.result.AppResult] — that type's
 * `Error` case is pinned to `AppError` specifically
 * (`data class Error(val error: AppError)`), and `AppError` is a
 * core/ type that shouldn't depend on this feature package's
 * [OciErrorCategory] taxonomy. [OciErrorCategory]'s own kdoc already
 * explains why it's deliberately separate from `AppError` — this is that
 * same boundary applied to the result type these functions return.
 */
sealed class OciResult<out T> {
    data class Success<out T>(val data: T) : OciResult<T>()
    data class Error(val error: OciProvisioningError) : OciResult<Nothing>()
}
