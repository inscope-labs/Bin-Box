package com.inscopelabs.abx.binbox.oci.api.compute

data class Shape(
    val shape: String,
    val ocpus: Double?,
    val memoryInGBs: Double?,
    val processorDescription: String?
)

data class Image(
    val id: String,
    val displayName: String,
    val operatingSystem: String,
    val operatingSystemVersion: String,
    val lifecycleState: String
)

/**
 * Only the flex-shape config fields (§17 — the Always Free Ampere A1 shape
 * is `VM.Standard.A1.Flex`, which requires explicit ocpus/memory rather
 * than a fixed shape spec).
 */
data class LaunchInstanceShapeConfig(
    val ocpus: Double,
    val memoryInGBs: Double
)

/** `sourceType` is `"image"` for a fresh boot volume from an image (the only path this package builds). */
data class InstanceSourceViaImageDetails(
    val sourceType: String = "image",
    val imageId: String
)

data class CreateVnicDetails(
    val subnetId: String,
    val assignPublicIp: Boolean = true
)

data class LaunchInstanceRequest(
    val availabilityDomain: String,
    val compartmentId: String,
    val shape: String,
    val shapeConfig: LaunchInstanceShapeConfig?,
    val sourceDetails: InstanceSourceViaImageDetails,
    val createVnicDetails: CreateVnicDetails,
    val displayName: String,
    val metadata: Map<String, String>
)

data class Instance(
    val id: String,
    val compartmentId: String,
    val availabilityDomain: String,
    val displayName: String,
    val shape: String,
    val lifecycleState: String,
    val timeCreated: String
)

data class VnicAttachment(
    val id: String,
    val instanceId: String,
    val vnicId: String?,
    val lifecycleState: String
)

data class Vnic(
    val id: String,
    val privateIp: String,
    val publicIp: String?,
    val isPrimary: Boolean
)

/**
 * CreateComputeCapacityReport — confirmed as the correct §23 mechanism
 * (not parsing "Out of host capacity." out of a generic 500 InternalError,
 * which is undocumented and not a stable contract). Call this BEFORE
 * LaunchInstance.
 */
data class CapacityReportShapeAvailabilityRequest(
    val instanceShape: String,
    val instanceShapeConfig: LaunchInstanceShapeConfig? = null
)

data class CreateComputeCapacityReportRequest(
    val compartmentId: String,
    val availabilityDomain: String,
    val shapeAvailabilities: List<CapacityReportShapeAvailabilityRequest>
)

/** [availabilityStatus] is one of "AVAILABLE", "OUT_OF_HOST_CAPACITY", "HARDWARE_NOT_SUPPORTED". */
data class CapacityReportShapeAvailability(
    val instanceShape: String,
    val availabilityStatus: String,
    val availableCount: Long? = null,
    val faultDomain: String? = null
)

data class ComputeCapacityReport(
    val id: String? = null,
    val compartmentId: String? = null,
    val availabilityDomain: String? = null,
    val shapeAvailabilities: List<CapacityReportShapeAvailability> = emptyList(),
    val timeCreated: String? = null
)
