package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compute.LaunchInstanceShapeConfig

/**
 * Top-level orchestrator: network, then capacity check, then launch+wait
 * (§15-24's actual sequencing). Advances [OciProvisioningSession] through
 * the state machine and persists after every step via [onSessionUpdate],
 * so a crash mid-provision leaves the session at the last completed step
 * rather than back at the start — §32's resumability requirement.
 *
 * Deliberately does not choose compartment/AD/shape/image itself — those
 * are selections that belong in front of this class (wizard UI /
 * discovery step), not decided implicitly here. Callers pass a fully
 * resolved [OciProvisioningContext].
 *
 * Flex-shape allocation ([flexOcpus]/[flexMemoryInGBs]) is a [provision]
 * parameter, not a hardcoded constant — [OciFreeTierShapes.DEFAULT_A1_OCPUS]/
 * `DEFAULT_A1_MEMORY_GB` are only the default when the caller doesn't
 * specify a split. To provision two VMs sharing the Ampere A1 pool
 * (e.g. 1 OCPU/6GB each, or any split totaling <= 4 OCPUs/24GB), call
 * [provision] twice with two different [OciProvisioningSession]s (one
 * instance per session — this class doesn't model multiple instances in
 * one session) and a different split each time. [NetworkProvisioner]'s
 * discover-or-create already makes the second call reuse the same VCN/
 * subnet rather than duplicating it, so no networking change is needed for
 * this — only the split needs to vary per call.
 */
class OciProvisioner(private val client: OciClient) {

    private val networkProvisioner = NetworkProvisioner(client)
    private val computeProvisioner = ComputeProvisioner(client)

    suspend fun provision(
        session: OciProvisioningSession,
        context: OciProvisioningContext,
        sshPublicKey: String,
        flexOcpus: Double = OciFreeTierShapes.DEFAULT_A1_OCPUS,
        flexMemoryInGBs: Double = OciFreeTierShapes.DEFAULT_A1_MEMORY_GB,
        onSessionUpdate: suspend (OciProvisioningSession) -> Unit
    ): OciResult<OciProvisioningSession> {
        val compartmentId = context.selectedCompartmentOcid
            ?: return OciResult.Error(missingSelection("compartment"))
        val availabilityDomain = context.availabilityDomains.firstOrNull()
            ?: return OciResult.Error(missingSelection("availability domain"))
        val shape = context.selectedShapeName
            ?: return OciResult.Error(missingSelection("shape"))
        val imageId = context.selectedImageOcid
            ?: return OciResult.Error(missingSelection("image"))

        var current = session.advance(OciProvisioningState.NETWORK_CREATING)
        onSessionUpdate(current)

        val networkResult = networkProvisioner.ensureNetwork(compartmentId, session.sessionId)
        val network = when (networkResult) {
            is OciResult.Success -> networkResult.data
            is OciResult.Error -> {
                current = current.fail(networkResult.error, OciProvisioningState.NETWORK_FAILED)
                onSessionUpdate(current)
                return OciResult.Error(networkResult.error)
            }
        }

        current = current.copy(
            vcnId = network.vcnId,
            internetGatewayOcid = network.internetGatewayId,
            subnetOcid = network.subnetId
        ).advance(OciProvisioningState.NETWORK_READY)
        onSessionUpdate(current)

        val shapeConfig = if (OciFreeTierShapes.isFlexShape(shape)) {
            LaunchInstanceShapeConfig(
                ocpus = flexOcpus,
                memoryInGBs = flexMemoryInGBs
            )
        } else null

        current = current.advance(OciProvisioningState.INSTANCE_CREATING)
        onSessionUpdate(current)

        val capacityResult = computeProvisioner.checkCapacity(compartmentId, availabilityDomain, shape, shapeConfig)
        if (capacityResult is OciResult.Error) {
            current = current.fail(capacityResult.error, OciProvisioningState.CAPACITY_UNAVAILABLE)
            onSessionUpdate(current)
            return OciResult.Error(capacityResult.error)
        }

        current = current.advance(OciProvisioningState.INSTANCE_PROVISIONING)
        onSessionUpdate(current)

        val computeResult = computeProvisioner.launchAndWait(
            compartmentId = compartmentId,
            availabilityDomain = availabilityDomain,
            shape = shape,
            shapeConfig = shapeConfig,
            imageId = imageId,
            subnetId = network.subnetId,
            displayName = "bin-box-${session.sessionId.take(8)}",
            sshPublicKey = sshPublicKey,
            sessionId = session.sessionId
        )
        val compute = when (computeResult) {
            is OciResult.Success -> computeResult.data
            is OciResult.Error -> {
                current = current.fail(computeResult.error, OciProvisioningState.INSTANCE_FAILED)
                onSessionUpdate(current)
                return OciResult.Error(computeResult.error)
            }
        }

        current = current.copy(
            instanceOcid = compute.instanceId,
            publicIp = compute.publicIp
        ).advance(OciProvisioningState.PUBLIC_IP_DISCOVERED)
        onSessionUpdate(current)

        return OciResult.Success(current)
    }

    private fun missingSelection(what: String) = OciProvisioningError(
        category = OciErrorCategory.ACCOUNT_ERROR,
        whatHappened = "No $what selected — can't start provisioning yet.",
        retryable = false
    )
}
