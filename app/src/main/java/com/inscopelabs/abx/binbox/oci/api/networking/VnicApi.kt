package com.inscopelabs.abx.binbox.oci.api.networking

import com.inscopelabs.abx.binbox.oci.api.compute.Vnic
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GetVnic — the second half of §24's public-IP discovery
 * (ListVnicAttachments in [com.inscopelabs.abx.binbox.oci.api.compute.ComputeApi]
 * gives the VNIC id; this resolves it to an actual IP). Vnic itself is a
 * Networking-service resource even though it's reached via a compute
 * instance, which is why this lives here rather than in `compute/`.
 */
interface VnicApi {
    @GET("20160918/vnics/{vnicId}")
    suspend fun getVnic(@Path("vnicId") vnicId: String): Response<Vnic>
}
