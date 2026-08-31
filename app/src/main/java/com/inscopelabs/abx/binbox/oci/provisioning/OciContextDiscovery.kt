package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compartments.AvailabilityDomain
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.api.compute.Instance
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext

/**
 * Populates the discoverable parts of [OciProvisioningContext] (§15-17):
 * compartments, availability domains, and Always-Free-eligible
 * shapes/images.
 */
class OciContextDiscovery(private val client: OciClient) {

    /** ListCompartments under the tenancy (root). Includes the tenancy itself as an implicit "root" option — callers add that. */
    suspend fun fetchCompartments(tenancyOcid: String): OciResult<List<Compartment>> {
        val response = OciStepContext.withStep(STAGE_ID, "discover_compartments") {
            client.identityApi.listCompartments(
                compartmentId = tenancyOcid,
                compartmentIdInSubtree = true
            )
        }
        return response.toOciResult()
    }

    suspend fun fetchAvailabilityDomains(compartmentId: String): OciResult<List<AvailabilityDomain>> {
        val response = OciStepContext.withStep(STAGE_ID, "discover_availability_domains") {
            client.identityApi.listAvailabilityDomains(compartmentId)
        }
        return response.toOciResult()
    }

    /**
     * Shapes filtered to what [OciFreeTierShapes] knows about.
     */
    suspend fun fetchEligibleShapes(compartmentId: String, availabilityDomain: String): OciResult<List<String>> {
        val response = OciStepContext.withStep(STAGE_ID, "list_shapes") {
            client.shapeApi.listShapes(compartmentId, availabilityDomain)
        }
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
        val response = OciStepContext.withStep(STAGE_ID, "list_images") {
            client.imageApi.listImages(compartmentId = compartmentId, shape = shape)
        }
        return response.toOciResult()
    }

    suspend fun fetchExistingInstances(compartmentId: String): OciResult<List<Instance>> {
        val response = OciStepContext.withStep(STAGE_ID, "list_existing_instances") {
            client.computeApi.listInstances(compartmentId = compartmentId)
        }
        val result = response.toOciResult()
        return when (result) {
            is OciResult.Success -> OciResult.Success(
                result.data.filter { it.lifecycleState !in setOf("TERMINATED", "TERMINATING") }
            )
            is OciResult.Error -> result
        }
    }

    private fun <T> retrofit2.Response<T>.toOciResult(): OciResult<T> =
        if (isSuccessful && body() != null) OciResult.Success(body()!!)
        else OciResult.Error(OciApiErrorMapper.fromErrorResponse(this))

    companion object {
        const val STAGE_ID = "CONTEXT_DISCOVERY"
    }
}
