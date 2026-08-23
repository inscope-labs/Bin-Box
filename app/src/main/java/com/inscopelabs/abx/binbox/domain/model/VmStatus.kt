package com.inscopelabs.abx.binbox.domain.model

/**
 * Cloud or virtual machine instance telemetry & operational status.
 */
data class VmStatus(
    val instanceId: String,
    val displayName: String,
    val state: VmState = VmState.RUNNING,
    val publicIp: String? = null,
    val privateIp: String? = null,
    val region: String = "us-ashburn-1",
    val shape: String = "VM.Standard.A1.Flex",
    val ocpus: Float = 4.0f,
    val memoryGbs: Float = 24.0f,
    val uptimeSeconds: Long = 0L,
    val cpuUtilizationPercent: Float = 0.0f,
    val memoryUtilizationPercent: Float = 0.0f,
    val diskUtilizationPercent: Float = 0.0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
