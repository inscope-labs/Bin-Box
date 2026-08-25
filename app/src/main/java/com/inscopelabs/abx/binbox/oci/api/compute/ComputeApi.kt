package com.inscopelabs.abx.binbox.oci.api.compute

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ComputeApi {

    /**
     * Call this BEFORE [launchInstance] — see ComputeModels.kt's kdoc on
     * why this is the correct §23 mechanism instead of catching a launch
     * failure and pattern-matching its message.
     */
    @POST("20160918/computeCapacityReports")
    suspend fun createComputeCapacityReport(
        @Body request: CreateComputeCapacityReportRequest
    ): Response<ComputeCapacityReport>

    /**
     * [opcRetryToken] must be stable per provisioning session (§19/§21) —
     * a retried launch after a crash must reuse the same token so OCI
     * recognizes it as the same request rather than creating a second
     * instance.
     */
    @POST("20160918/instances")
    suspend fun launchInstance(
        @Body request: LaunchInstanceRequest,
        @Header("opc-retry-token") opcRetryToken: String
    ): Response<Instance>

    /** Poll target for §22's PROVISIONING -> RUNNING state machine. */
    @GET("20160918/instances/{instanceId}")
    suspend fun getInstance(@Path("instanceId") instanceId: String): Response<Instance>

    /** First half of §24's public-IP discovery: instance -> VNIC attachment -> VNIC. */
    @GET("20160918/vnicAttachments")
    suspend fun listVnicAttachments(
        @Query("compartmentId") compartmentId: String,
        @Query("instanceId") instanceId: String
    ): Response<List<VnicAttachment>>
}
