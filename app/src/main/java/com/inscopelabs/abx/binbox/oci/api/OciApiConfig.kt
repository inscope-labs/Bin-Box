package com.inscopelabs.abx.binbox.oci.api

/**
 * Per-region OCI API hosts and API version. Confirmed directly against
 * Oracle's REST API documentation (docs.oracle.com/en-us/iaas/Content/API/Concepts/usingapi.htm,
 * signingrequests.htm) rather than assumed:
 *
 *   Identity:      https://identity.<region>.oraclecloud.com/20160918/...
 *   Core Services: https://iaas.<region>.oraclecloud.com/20160918/...
 *
 * (Core Services covers Networking and Compute both — Oracle does not split
 * them into separate hosts, only separate path prefixes.)
 */
object OciApiConfig {
    const val API_VERSION = "20160918"

    fun identityBaseUrl(region: String): String = "https://identity.$region.oraclecloud.com/"
    fun coreServicesBaseUrl(region: String): String = "https://iaas.$region.oraclecloud.com/"
}
