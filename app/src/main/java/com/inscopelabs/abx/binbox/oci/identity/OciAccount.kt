package com.inscopelabs.abx.binbox.oci.identity

/**
 * The user's OCI account context — tenancy, user, home region, and the
 * compartment provisioning will target.
 *
 * [compartmentOcid] is deliberately never defaulted silently to the tenancy
 * root elsewhere in this package (see OCI provisioning doc §16). Callers
 * that want the tenancy-root default must set it explicitly here.
 */
data class OciAccount(
    val tenancyOcid: String,
    val userOcid: String,
    val homeRegion: String,
    val compartmentOcid: String
) {
    init {
        require(tenancyOcid.startsWith("ocid1.tenancy.")) {
            "tenancyOcid does not look like a valid OCI tenancy OCID"
        }
        require(userOcid.startsWith("ocid1.user.")) {
            "userOcid does not look like a valid OCI user OCID"
        }
        require(compartmentOcid.startsWith("ocid1.tenancy.") || compartmentOcid.startsWith("ocid1.compartment.")) {
            "compartmentOcid does not look like a valid OCI tenancy/compartment OCID"
        }
    }
}
