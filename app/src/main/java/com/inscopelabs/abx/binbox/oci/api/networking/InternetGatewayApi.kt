package com.inscopelabs.abx.binbox.oci.api.networking

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface InternetGatewayApi {

    @GET("20160918/internetGateways")
    suspend fun listInternetGateways(
        @Query("compartmentId") compartmentId: String,
        @Query("vcnId") vcnId: String,
        @Query("lifecycleState") lifecycleState: String = "AVAILABLE"
    ): Response<List<InternetGateway>>

    @POST("20160918/internetGateways")
    suspend fun createInternetGateway(
        @Body request: CreateInternetGatewayRequest,
        @Header("opc-retry-token") opcRetryToken: String
    ): Response<InternetGateway>
}
