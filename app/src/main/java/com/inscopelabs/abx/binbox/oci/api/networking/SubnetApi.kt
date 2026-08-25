package com.inscopelabs.abx.binbox.oci.api.networking

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface SubnetApi {

    @GET("20160918/subnets")
    suspend fun listSubnets(
        @Query("compartmentId") compartmentId: String,
        @Query("vcnId") vcnId: String,
        @Query("lifecycleState") lifecycleState: String = "AVAILABLE"
    ): Response<List<Subnet>>

    @POST("20160918/subnets")
    suspend fun createSubnet(
        @Body request: CreateSubnetRequest,
        @Header("opc-retry-token") opcRetryToken: String
    ): Response<Subnet>
}
