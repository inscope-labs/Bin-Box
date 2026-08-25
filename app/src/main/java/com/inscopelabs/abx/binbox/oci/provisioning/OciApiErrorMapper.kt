package com.inscopelabs.abx.binbox.oci.provisioning

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response

/**
 * Maps an OCI API HTTP response to an [OciProvisioningError], using
 * Oracle's confirmed common error table
 * (docs.oracle.com/en-us/iaas/Content/API/References/apierrors.htm) —
 * every `httpCode`/`code` pairing below is taken directly from that page,
 * not inferred.
 *
 * `QuotaExceeded` and `LimitExceeded` are real, distinct 400 error codes —
 * directly usable for §23's quota distinction. "No capacity for this
 * shape" has NO distinct error code of its own; it's an undocumented
 * message string (`"Out of host capacity."`) inside a generic 500
 * `InternalError`. Don't build capacity detection on that string — call
 * `ComputeApi.createComputeCapacityReport` before `launchInstance` instead
 * (see `ComputeModels.kt`'s kdoc) and treat any 500 that reaches this
 * mapper as a genuine unknown/internal error, not a capacity signal.
 */
object OciApiErrorMapper {

    private data class OciErrorBody(val code: String?, val message: String?)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(OciErrorBody::class.java)

    fun <T> fromErrorResponse(response: Response<T>, cause: Throwable? = null): OciProvisioningError {
        val httpCode = response.code()
        val bodyJson = runCatching { response.errorBody()?.string() }.getOrNull()
        val parsed = bodyJson?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
        val code = parsed?.code
        val message = parsed?.message ?: response.message()

        val category = when (httpCode) {
            400 -> when (code) {
                "QuotaExceeded" -> OciErrorCategory.QUOTA_ERROR
                "LimitExceeded" -> OciErrorCategory.QUOTA_ERROR
                else -> OciErrorCategory.ACCOUNT_ERROR // CannotParseRequest / InvalidParameter / MissingParameter / RelatedResourceNotAuthorizedOrNotFound
            }
            401 -> OciErrorCategory.AUTHENTICATION_ERROR
            403 -> OciErrorCategory.PERMISSION_ERROR
            404 -> OciErrorCategory.COMPARTMENT_ERROR // NotAuthorizedOrNotFound most commonly means "wrong/inaccessible compartment" in this flow
            409 -> OciErrorCategory.COMPUTE_ERROR // Conflict / IncorrectState — resource lifecycle conflict
            429 -> OciErrorCategory.TIMEOUT_ERROR // TooManyRequests — caller should back off and retry
            in 500..599 -> OciErrorCategory.COMPUTE_ERROR // see kdoc: NOT capacity-specific, that's CreateComputeCapacityReport's job
            else -> OciErrorCategory.UNKNOWN_ERROR
        }

        val retryable = httpCode == 429 || httpCode in 500..599

        return OciProvisioningError(
            category = category,
            whatHappened = message ?: "OCI API request failed (HTTP $httpCode)",
            whyItHappened = code,
            whatUserCanDo = when (category) {
                OciErrorCategory.AUTHENTICATION_ERROR -> "Check that your API key and fingerprint are registered correctly in OCI."
                OciErrorCategory.PERMISSION_ERROR -> "Your OCI account may not have permission for this action in this compartment."
                OciErrorCategory.QUOTA_ERROR -> "This would exceed your tenancy's quota or service limit for this resource."
                else -> null
            },
            retryable = retryable,
            cause = cause
        )
    }
}
