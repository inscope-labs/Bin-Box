package com.inscopelabs.abx.binbox.oci.api.networking

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Not in the doc's §5 package listing (which stops at
 * `InternetGatewayApi.kt`), but §18's networking dependency graph
 * (VCN → Internet Gateway → Route Table → Subnet) requires it: every VCN
 * already has a default route table (`Vcn.defaultRouteTableId`), so this
 * doesn't need CreateRouteTable — [updateDefaultRouteTable] points the
 * VCN's existing default route table at the internet gateway rather than
 * creating a second one, which is simpler and keeps §19's idempotency
 * property (nothing to discover-or-create; the default table always
 * exists once the VCN does).
 */
interface RouteTableApi {

    @GET("20160918/routeTables/{routeTableId}")
    suspend fun getRouteTable(@Path("routeTableId") routeTableId: String): Response<RouteTable>

    /** UpdateRouteTable replaces the whole rule set — see [UpdateRouteTableRequest]'s kdoc. */
    @PUT("20160918/routeTables/{routeTableId}")
    suspend fun updateRouteTable(
        @Path("routeTableId") routeTableId: String,
        @Body request: UpdateRouteTableRequest
    ): Response<RouteTable>
}
