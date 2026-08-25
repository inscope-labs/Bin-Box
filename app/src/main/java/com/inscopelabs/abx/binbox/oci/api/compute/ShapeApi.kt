package com.inscopelabs.abx.binbox.oci.api.compute

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ShapeApi {
    @GET("20160918/shapes")
    suspend fun listShapes(
        @Query("compartmentId") compartmentId: String,
        @Query("availabilityDomain") availabilityDomain: String? = null,
        @Query("imageId") imageId: String? = null
    ): Response<List<Shape>>
}
