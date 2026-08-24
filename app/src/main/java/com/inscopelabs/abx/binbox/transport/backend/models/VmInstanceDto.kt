package com.inscopelabs.abx.binbox.transport.backend.models

import com.inscopelabs.abx.binbox.domain.model.VmState
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for VM instances queried from the backend / cloud management API.
 */
@JsonClass(generateAdapter = true)
data class VmInstanceDto(
    @Json(name = "instanceId") val instanceId: String,
    @Json(name = "displayName") val displayName: String,
    @Json(name = "state") val state: String = "RUNNING",
    @Json(name = "publicIp") val publicIp: String? = null,
    @Json(name = "privateIp") val privateIp: String? = null,
    @Json(name = "region") val region: String = "us-ashburn-1",
    @Json(name = "shape") val shape: String = "VM.Standard.A1.Flex",
    @Json(name = "ocpus") val ocpus: Float = 4.0f,
    @Json(name = "memoryGbs") val memoryGbs: Float = 24.0f,
    @Json(name = "uptimeSeconds") val uptimeSeconds: Long = 0L,
    @Json(name = "cpuUtilizationPercent") val cpuUtilizationPercent: Float = 0.0f,
    @Json(name = "memoryUtilizationPercent") val memoryUtilizationPercent: Float = 0.0f,
    @Json(name = "diskUtilizationPercent") val diskUtilizationPercent: Float = 0.0f
) {
    fun toDomain(): VmStatus {
        val vmState = try {
            VmState.valueOf(state.uppercase())
        } catch (_: Exception) {
            VmState.RUNNING
        }
        return VmStatus(
            instanceId = instanceId,
            displayName = displayName,
            state = vmState,
            publicIp = publicIp,
            privateIp = privateIp,
            region = region,
            shape = shape,
            ocpus = ocpus,
            memoryGbs = memoryGbs,
            uptimeSeconds = uptimeSeconds,
            cpuUtilizationPercent = cpuUtilizationPercent,
            memoryUtilizationPercent = memoryUtilizationPercent,
            diskUtilizationPercent = diskUtilizationPercent,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
