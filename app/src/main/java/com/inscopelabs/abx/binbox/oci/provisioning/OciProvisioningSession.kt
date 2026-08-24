package com.inscopelabs.abx.binbox.oci.provisioning

/**
 * What §15 calls "OciProvisioningContext" — the discovered OCI environment,
 * populated once authentication succeeds. Identifiers only; the actual
 * shape/image/network objects they refer to are fetched live from the OCI
 * API layer (deferred — see api/ package note) rather than cached here.
 */
data class OciProvisioningContext(
    val availableCompartmentOcids: List<String> = emptyList(),
    val selectedCompartmentOcid: String? = null, // null = tenancy root (§16 default)
    val availabilityDomains: List<String> = emptyList(),
    val eligibleShapeNames: List<String> = emptyList(),
    val selectedShapeName: String? = null,
    val eligibleImageOcids: List<String> = emptyList(),
    val selectedImageOcid: String? = null,
    val existingVcnOcid: String? = null,
    val existingSubnetOcid: String? = null
)

/**
 * A resumable provisioning session (§31, §32). Persisted via
 * [OciProvisioningRepository] so the wizard can recover after the process
 * is killed, backgrounded, or loses network — by re-querying actual OCI
 * state using the stored OCIDs rather than assuming local state is still
 * accurate (§32: "It should inspect actual OCI state before deciding what
 * operation remains").
 *
 * This class only carries identifiers and state, deliberately — it must
 * never create duplicate infrastructure merely because the session was
 * reloaded (§32), so every provisioning step that consumes this session
 * must treat a non-null id field as "verify this still exists" rather
 * than "this is known good."
 */
data class OciProvisioningSession(
    val sessionId: String,
    val state: OciProvisioningState,
    val context: OciProvisioningContext = OciProvisioningContext(),

    // SSH identity for the target VM (§20) — key alias only, never raw key material.
    val sshKeyAlias: String? = null,

    // Networking (§18-19) — set once discovered-or-created, idempotently.
    val vcnOcid: String? = null,
    val internetGatewayOcid: String? = null,
    val routeTableOcid: String? = null,
    val subnetOcid: String? = null,

    // Compute (§21-22)
    val instanceOcid: String? = null,

    // Reachability (§24-25)
    val publicIp: String? = null,

    // Terminal registration (§26)
    val registeredShellHostId: String? = null,

    val lastError: OciProvisioningError? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun advance(newState: OciProvisioningState): OciProvisioningSession =
        copy(state = newState, lastError = null, updatedAtMillis = System.currentTimeMillis())

    fun fail(error: OciProvisioningError, failureState: OciProvisioningState): OciProvisioningSession {
        require(failureState.isFailure) { "$failureState is not a failure state" }
        return copy(state = failureState, lastError = error, updatedAtMillis = System.currentTimeMillis())
    }
}
