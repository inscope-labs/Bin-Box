package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compartments.AvailabilityDomain
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image

/**
 * Populates the discoverable parts of [OciProvisioningContext] (§15-17):
 * compartments, availability domains, and Always-Free-eligible
 * shapes/images. Doesn't pick defaults or make selections itself — every
 * `selected*` field in [OciProvisioningContext] stays null/unset here;
 * that's the caller's (wizard UI's) job, same boundary [OciProvisioner]
 * already draws for the resolved context it consumes.
 */
class OciContextDiscovery(private val client: OciClient) {

    /** ListCompartments under the tenancy (root). Includes the tenancy itself as an implicit "root" option — callers add that. */
    suspend fun fetchCompartments(tenancyOcid: String): OciResult<List<Compartment>> {
        val response = client.identityApi.listCompartments(
            compartmentId = tenancyOcid,
            compartmentIdInSubtree = true
        )
        return response.toOciResult()
    }

    suspend fun fetchAvailabilityDomains(compartmentId: String): OciResult<List<AvailabilityDomain>> {
        val response = client.identityApi.listAvailabilityDomains(compartmentId)
        return response.toOciResult()
    }

    /**
     * Shapes filtered to what [OciFreeTierShapes] knows about — the raw
     * ListShapes response includes every paid shape too, which isn't
     * useful noise for an Always-Free-focused wizard. Availability of a
     * *listed* shape here is not the same as *capacity* for it — that's
     * [ComputeProvisioner.checkCapacity]'s job, called later at launch time.
     */
    suspend fun fetchEligibleShapes(compartmentId: String, availabilityDomain: String): OciResult<List<String>> {
        val response = client.shapeApi.listShapes(compartmentId, availabilityDomain)
        val result = response.toOciResult()
        return when (result) {
            is OciResult.Success -> OciResult.Success(
                result.data
                    .map { it.shape }
                    .filter { it == OciFreeTierShapes.AMPERE_A1_FLEX || it == OciFreeTierShapes.E2_MICRO }
                    .distinct()
            )
            is OciResult.Error -> result
        }
    }

    suspend fun fetchImages(compartmentId: String, shape: String): OciResult<List<Image>> {
        val response = client.imageApi.listImages(compartmentId = compartmentId, shape = shape)
        return response.toOciResult()
    }

    private fun <T> retrofit2.Response<T>.toOciResult(): OciResult<T> =
        if (isSuccessful && body() != null) OciResult.Success(body()!!)
        else OciResult.Error(OciApiErrorMapper.fromErrorResponse(this))
}
