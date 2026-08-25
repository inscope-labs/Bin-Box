package com.inscopelabs.abx.binbox.oci.api.compartments

/** Identity service Compartment resource. */
data class Compartment(
    val id: String,
    val compartmentId: String,
    val name: String,
    val description: String?,
    val lifecycleState: String,
    val timeCreated: String
)

/**
 * Identity service User resource — only the fields this package needs.
 * GetUser is used for §14's "harmless authenticated request" connection
 * check: a GET with no side effects, that only succeeds if signing and
 * account info are both correct.
 */
data class OciUser(
    val id: String,
    val compartmentId: String,
    val name: String,
    val lifecycleState: String
)

/** Confirmed field-for-field against Oracle's ListAvailabilityDomains schema. */
data class AvailabilityDomain(
    val compartmentId: String,
    val id: String,
    val name: String
)
