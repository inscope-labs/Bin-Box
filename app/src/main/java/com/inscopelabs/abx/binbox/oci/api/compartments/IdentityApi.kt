package com.inscopelabs.abx.binbox.oci.api.compartments

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Identity service API (host: identity.<region>.oraclecloud.com).
 *
 * Named `IdentityApi` rather than the doc's literal `CompartmentApi.kt`
 * (§5) since §14's connection-verification check needs GetUser from the
 * same service — kept together rather than splitting one Identity-service
 * file into two for a single extra endpoint.
 */
interface IdentityApi {

    /**
     * ListCompartments. Confirmed request shape: GET with `compartmentId`
     * required; `compartmentIdInSubtree=true` only valid when
     * `compartmentId` is the tenancy (root). Paginated — [page] is the
     * `opc-next-page` cursor from a previous response's headers.
     */
    @GET("20160918/compartments")
    suspend fun listCompartments(
        @Query("compartmentId") compartmentId: String,
        @Query("compartmentIdInSubtree") compartmentIdInSubtree: Boolean = false,
        @Query("accessLevel") accessLevel: String = "ACCESSIBLE",
        @Query("lifecycleState") lifecycleState: String = "ACTIVE",
        @Query("page") page: String? = null
    ): Response<List<Compartment>>

    /**
     * GetUser. Used as the §14 "harmless authenticated OCI API request" —
     * a read-only GET keyed on the user's own OCID, so it succeeds only if
     * signing and account info are both correct, with no side effects.
     */
    @GET("20160918/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): Response<OciUser>
}
