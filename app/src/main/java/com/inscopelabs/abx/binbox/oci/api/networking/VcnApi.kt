package com.inscopelabs.abx.binbox.oci.api.networking

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VcnApi {

    /** ListVcns first — §19 requires discovering existing infrastructure before creating. */
    @GET("20160918/vcns")
    suspend fun listVcns(
        @Query("compartmentId") compartmentId: String,
        @Query("displayName") displayName: String? = null,
        @Query("lifecycleState") lifecycleState: String = "AVAILABLE"
    ): Response<List<Vcn>>

    @GET("20160918/vcns/{vcnId}")
    suspend fun getVcn(@Path("vcnId") vcnId: String): Response<Vcn>

    /**
     * [opcRetryToken] should be a stable, session-derived value (not random
     * per call) so a retried CreateVcn after a crash doesn't create a
     * second VCN — see §19. Oracle expires retry tokens after 24 hours.
     */
    @POST("20160918/vcns")
    suspend fun createVcn(
        @Body request: CreateVcnRequest,
        @Header("opc-retry-token") opcRetryToken: String
    ): Response<Vcn>
}
