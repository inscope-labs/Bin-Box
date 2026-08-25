package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compute.CapacityReportShapeAvailabilityRequest
import com.inscopelabs.abx.binbox.oci.api.compute.CreateComputeCapacityReportRequest
import com.inscopelabs.abx.binbox.oci.api.compute.CreateVnicDetails
import com.inscopelabs.abx.binbox.oci.api.compute.Instance
import com.inscopelabs.abx.binbox.oci.api.compute.InstanceSourceViaImageDetails
import com.inscopelabs.abx.binbox.oci.api.compute.LaunchInstanceRequest
import com.inscopelabs.abx.binbox.oci.api.compute.LaunchInstanceShapeConfig
import retrofit2.Response
import kotlin.time.Duration.Companion.minutes

data class ComputeResult(
    val instanceId: String,
    val publicIp: String
)

/**
 * Instance launch + capacity check + poll-to-running + public IP discovery
 * (§21-24).
 *
 * Deliberately does NOT attempt discover-before-create for the instance
 * itself the way [NetworkProvisioner] does for networking — an existing
 * "bin-box-managed" instance found via search would still need its
 * lifecycle state, shape, and reachability re-verified before reuse is
 * actually safe, and that verification is equivalent in cost to just
 * re-running this provisioner idempotently via `opc-retry-token`. Retry
 * safety here comes from the retry token, not from a pre-launch discovery
 * pass.
 */
class ComputeProvisioner(private val client: OciClient) {

    /**
     * Checks capacity BEFORE attempting to launch — see ComputeModels.kt's
     * kdoc for why this is the correct §23 mechanism instead of catching a
     * launch failure. Returns an error immediately if the shape is
     * unavailable in this AD, without ever calling [launchAndWait].
     */
    suspend fun checkCapacity(
        compartmentId: String,
        availabilityDomain: String,
        shape: String,
        shapeConfig: LaunchInstanceShapeConfig?
    ): OciResult<Boolean> {
        val response = client.computeApi.createComputeCapacityReport(
            CreateComputeCapacityReportRequest(
                compartmentId = compartmentId,
                availabilityDomain = availabilityDomain,
                shapeAvailabilities = listOf(
                    CapacityReportShapeAvailabilityRequest(instanceShape = shape, instanceShapeConfig = shapeConfig)
                )
            )
        )
        if (!response.isSuccessful) return OciResult.Error(OciApiErrorMapper.fromErrorResponse(response))
        val available = response.body()?.shapeAvailabilities?.firstOrNull()?.availabilityStatus == "AVAILABLE"
        if (!available) {
            return OciResult.Error(
                OciProvisioningError(
                    category = OciErrorCategory.CAPACITY_ERROR,
                    whatHappened = "No capacity available for $shape in $availabilityDomain right now.",
                    whatUserCanDo = "Try a different availability domain, or try again later — Always Free capacity fluctuates.",
                    retryable = true
                )
            )
        }
        return OciResult.Success(true)
    }

    /**
     * Launches the instance (with a session-stable `opcRetryToken` — see
     * [NetworkProvisioner]'s kdoc on why retry tokens are session-scoped
     * rather than random per call), polls until RUNNING, then resolves the
     * public IP via ListVnicAttachments -> GetVnic (§24).
     *
     * Callers MUST call [checkCapacity] first — this does not check
     * capacity itself, to keep the "check" and "act" steps independently
     * retryable without re-triggering a launch attempt on a capacity
     * re-check.
     */
    suspend fun launchAndWait(
        compartmentId: String,
        availabilityDomain: String,
        shape: String,
        shapeConfig: LaunchInstanceShapeConfig?,
        imageId: String,
        subnetId: String,
        displayName: String,
        sshPublicKey: String,
        sessionId: String
    ): OciResult<ComputeResult> {
        val launchResponse = client.computeApi.launchInstance(
            request = LaunchInstanceRequest(
                availabilityDomain = availabilityDomain,
                compartmentId = compartmentId,
                shape = shape,
                shapeConfig = shapeConfig,
                sourceDetails = InstanceSourceViaImageDetails(imageId = imageId),
                createVnicDetails = CreateVnicDetails(subnetId = subnetId, assignPublicIp = true),
                displayName = displayName,
                metadata = mapOf("ssh_authorized_keys" to sshPublicKey)
            ),
            opcRetryToken = "instance-$sessionId"
        )
        if (!launchResponse.isSuccessful) return OciResult.Error(OciApiErrorMapper.fromErrorResponse(launchResponse))
        val instanceId = launchResponse.body()?.id
            ?: return OciResult.Error(OciApiErrorMapper.fromErrorResponse(launchResponse))

        val runningResult = ProvisioningPoller.poll(
            isDone = { it.lifecycleState == "RUNNING" },
            isFailed = { it.lifecycleState in FAILURE_LIFECYCLE_STATES },
            fetch = { getInstance(instanceId) }
        )
        if (runningResult is OciResult.Error) return runningResult

        val publicIpResult = discoverPublicIp(compartmentId, instanceId)
        val publicIp = (publicIpResult as? OciResult.Success)?.data ?: return publicIpResult as OciResult.Error

        return OciResult.Success(ComputeResult(instanceId = instanceId, publicIp = publicIp))
    }

    private suspend fun getInstance(instanceId: String): OciResult<Instance> {
        val response = client.computeApi.getInstance(instanceId)
        return response.toOciResult()
    }

    /** §24: instance -> VNIC attachment -> VNIC -> public IP. Polls briefly since the public IP can lag instance RUNNING slightly. */
    private suspend fun discoverPublicIp(compartmentId: String, instanceId: String): OciResult<String> {
        val result = ProvisioningPoller.poll(
            timeout = 2.minutes,
            isDone = { it != null },
            isFailed = { false },
            fetch = {
                val attachments = client.computeApi.listVnicAttachments(compartmentId, instanceId)
                if (!attachments.isSuccessful) return@poll OciResult.Error(OciApiErrorMapper.fromErrorResponse(attachments))
                val vnicId = attachments.body()
                    ?.firstOrNull { it.lifecycleState == "ATTACHED" }?.vnicId
                    ?: return@poll OciResult.Success(null)

                val vnic = client.vnicApi.getVnic(vnicId)
                if (!vnic.isSuccessful) return@poll OciResult.Error(OciApiErrorMapper.fromErrorResponse(vnic))
                OciResult.Success(vnic.body()?.publicIp)
            }
        )
        return when (result) {
            is OciResult.Success -> result.data?.let { OciResult.Success(it) }
                ?: OciResult.Error(
                    OciProvisioningError(
                        category = OciErrorCategory.NETWORK_ERROR,
                        whatHappened = "Instance has no public IP assigned.",
                        retryable = false
                    )
                )
            is OciResult.Error -> result
        }
    }

    private fun <T> Response<T>.toOciResult(): OciResult<T> =
        if (isSuccessful && body() != null) OciResult.Success(body()!!)
        else OciResult.Error(OciApiErrorMapper.fromErrorResponse(this))

    companion object {
        private val FAILURE_LIFECYCLE_STATES = setOf("TERMINATING", "TERMINATED")
    }
}
