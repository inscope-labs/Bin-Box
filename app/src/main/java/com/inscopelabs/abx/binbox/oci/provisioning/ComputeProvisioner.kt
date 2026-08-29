package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compute.CapacityReportShapeAvailabilityRequest
import com.inscopelabs.abx.binbox.oci.api.compute.CreateComputeCapacityReportRequest
import com.inscopelabs.abx.binbox.oci.api.compute.CreateVnicDetails
import com.inscopelabs.abx.binbox.oci.api.compute.Instance
import com.inscopelabs.abx.binbox.oci.api.compute.InstanceSourceViaImageDetails
import com.inscopelabs.abx.binbox.oci.api.compute.LaunchInstanceRequest
import com.inscopelabs.abx.binbox.oci.api.compute.LaunchInstanceShapeConfig
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext
import retrofit2.Response
import kotlin.time.Duration.Companion.minutes

data class ComputeResult(
    val instanceId: String,
    val publicIp: String
)

/**
 * Instance launch + capacity check + poll-to-running + public IP discovery (§21-24).
 */
class ComputeProvisioner(private val client: OciClient) {

    suspend fun checkCapacity(
        compartmentId: String,
        availabilityDomain: String,
        shape: String,
        shapeConfig: LaunchInstanceShapeConfig?
    ): OciResult<Boolean> {
        val response = OciStepContext.withStep(STAGE_ID, "check_capacity") {
            client.computeApi.createComputeCapacityReport(
                CreateComputeCapacityReportRequest(
                    compartmentId = compartmentId,
                    availabilityDomain = availabilityDomain,
                    shapeAvailabilities = listOf(
                        CapacityReportShapeAvailabilityRequest(instanceShape = shape, instanceShapeConfig = shapeConfig)
                    )
                )
            )
        }
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
        val launchResponse = OciStepContext.withStep(STAGE_ID, "launch_instance") {
            client.computeApi.launchInstance(
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
        }
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
        val response = OciStepContext.withStep(STAGE_ID, "poll_instance_running") {
            client.computeApi.getInstance(instanceId)
        }
        return response.toOciResult()
    }

    /** §24: instance -> VNIC attachment -> VNIC -> public IP. Polls briefly since the public IP can lag instance RUNNING slightly. */
    private suspend fun discoverPublicIp(compartmentId: String, instanceId: String): OciResult<String> {
        val result = ProvisioningPoller.poll(
            timeout = 2.minutes,
            isDone = { it != null },
            isFailed = { false },
            fetch = {
                val attachments = OciStepContext.withStep(STAGE_ID, "discover_vnic_attachments") {
                    client.computeApi.listVnicAttachments(compartmentId, instanceId)
                }
                if (!attachments.isSuccessful) return@poll OciResult.Error(OciApiErrorMapper.fromErrorResponse(attachments))
                val vnicId = attachments.body()
                    ?.firstOrNull { it.lifecycleState == "ATTACHED" }?.vnicId
                    ?: return@poll OciResult.Success(null)

                val vnic = OciStepContext.withStep(STAGE_ID, "get_vnic") {
                    client.vnicApi.getVnic(vnicId)
                }
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
        const val STAGE_ID = "COMPUTE_PROVISIONING"
        private val FAILURE_LIFECYCLE_STATES = setOf("TERMINATING", "TERMINATED")
    }
}
