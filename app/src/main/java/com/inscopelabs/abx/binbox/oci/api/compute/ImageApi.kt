package com.inscopelabs.abx.binbox.oci.api.compute

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ImageApi {
    @GET("20160918/images")
    suspend fun listImages(
        @Query("compartmentId") compartmentId: String,
        @Query("operatingSystem") operatingSystem: String? = null,
        @Query("shape") shape: String? = null,
        @Query("lifecycleState") lifecycleState: String = "AVAILABLE"
    ): Response<List<Image>>
}
