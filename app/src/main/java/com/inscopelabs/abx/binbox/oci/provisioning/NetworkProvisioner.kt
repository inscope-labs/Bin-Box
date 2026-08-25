package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.networking.CreateInternetGatewayRequest
import com.inscopelabs.abx.binbox.oci.api.networking.CreateSubnetRequest
import com.inscopelabs.abx.binbox.oci.api.networking.CreateVcnRequest
import com.inscopelabs.abx.binbox.oci.api.networking.InternetGateway
import com.inscopelabs.abx.binbox.oci.api.networking.RouteRule
import com.inscopelabs.abx.binbox.oci.api.networking.Subnet
import com.inscopelabs.abx.binbox.oci.api.networking.UpdateRouteTableRequest
import com.inscopelabs.abx.binbox.oci.api.networking.Vcn
import retrofit2.Response

/** VCN/IGW/subnet already provisioned and reachable — the output §19's discover-or-create logic produces. */
data class NetworkResult(
    val vcnId: String,
    val internetGatewayId: String,
    val subnetId: String
)

/**
 * Idempotent networking discover-or-create (§18-19).
 *
 * Discoverable by a STABLE tag, not a per-session id: [DISPLAY_NAME_TAG] is
 * a fixed name, not `"...-$sessionId"`. This is deliberate — re-running the
 * wizard (a new [OciProvisioningSession]) should reuse the same VCN in a
 * compartment rather than create a new one every time, since §19 requires
 * "discover existing infrastructure before creating." The `opc-retry-token`
 * passed to each create call is session-scoped instead (see
 * [retryTokenFor]) — that guards against a single request being duplicated
 * by a crash-and-retry within one attempt, a different idempotency concern
 * than "does the resource already exist at all."
 */
class NetworkProvisioner(private val client: OciClient) {

    suspend fun ensureNetwork(compartmentId: String, sessionId: String): OciResult<NetworkResult> {
        val vcnResult = ensureVcn(compartmentId, sessionId)
        val vcn = (vcnResult as? OciResult.Success)?.data ?: return vcnResult as OciResult.Error

        val igwResult = ensureInternetGateway(compartmentId, vcn, sessionId)
        val igw = (igwResult as? OciResult.Success)?.data ?: return igwResult as OciResult.Error

        val routeUpdate = client.routeTableApi.updateRouteTable(
            routeTableId = vcn.defaultRouteTableId,
            request = UpdateRouteTableRequest(
                routeRules = listOf(RouteRule(cidrBlock = "0.0.0.0/0", networkEntityId = igw.id))
            )
        )
        if (!routeUpdate.isSuccessful) {
            return OciResult.Error(OciApiErrorMapper.fromErrorResponse(routeUpdate))
        }

        val subnetResult = ensureSubnet(compartmentId, vcn, sessionId)
        val subnet = (subnetResult as? OciResult.Success)?.data ?: return subnetResult as OciResult.Error

        return OciResult.Success(
            NetworkResult(vcnId = vcn.id, internetGatewayId = igw.id, subnetId = subnet.id)
        )
    }

    private suspend fun ensureVcn(compartmentId: String, sessionId: String): OciResult<Vcn> {
        val list = client.vcnApi.listVcns(compartmentId, displayName = DISPLAY_NAME_TAG)
        if (!list.isSuccessful) return OciResult.Error(OciApiErrorMapper.fromErrorResponse(list))
        list.body()?.firstOrNull()?.let { return OciResult.Success(it) }

        val created = client.vcnApi.createVcn(
            request = CreateVcnRequest(compartmentId = compartmentId, displayName = DISPLAY_NAME_TAG, cidrBlock = VCN_CIDR),
            opcRetryToken = retryTokenFor("vcn", sessionId)
        )
        return created.toOciResult()
    }

    /** [InternetGatewayApi] has no `displayName` query filter — filtered client-side instead. */
    private suspend fun ensureInternetGateway(compartmentId: String, vcn: Vcn, sessionId: String): OciResult<InternetGateway> {
        val list = client.internetGatewayApi.listInternetGateways(compartmentId, vcn.id)
        if (!list.isSuccessful) return OciResult.Error(OciApiErrorMapper.fromErrorResponse(list))
        list.body()?.firstOrNull { it.displayName == DISPLAY_NAME_TAG }?.let { return OciResult.Success(it) }

        val created = client.internetGatewayApi.createInternetGateway(
            request = CreateInternetGatewayRequest(compartmentId = compartmentId, vcnId = vcn.id, displayName = DISPLAY_NAME_TAG),
            opcRetryToken = retryTokenFor("igw", sessionId)
        )
        return created.toOciResult()
    }

    private suspend fun ensureSubnet(compartmentId: String, vcn: Vcn, sessionId: String): OciResult<Subnet> {
        val list = client.subnetApi.listSubnets(compartmentId, vcn.id)
        if (!list.isSuccessful) return OciResult.Error(OciApiErrorMapper.fromErrorResponse(list))
        list.body()?.firstOrNull { it.displayName == DISPLAY_NAME_TAG }?.let { return OciResult.Success(it) }

        val created = client.subnetApi.createSubnet(
            request = CreateSubnetRequest(
                compartmentId = compartmentId,
                vcnId = vcn.id,
                displayName = DISPLAY_NAME_TAG,
                cidrBlock = SUBNET_CIDR,
                routeTableId = vcn.defaultRouteTableId
            ),
            opcRetryToken = retryTokenFor("subnet", sessionId)
        )
        return created.toOciResult()
    }

    private fun retryTokenFor(resource: String, sessionId: String) = "$resource-$sessionId"

    private fun <T> Response<T>.toOciResult(): OciResult<T> =
        if (isSuccessful && body() != null) OciResult.Success(body()!!)
        else OciResult.Error(OciApiErrorMapper.fromErrorResponse(this))

    companion object {
        const val DISPLAY_NAME_TAG = "bin-box-managed"
        const val VCN_CIDR = "10.0.0.0/16"
        const val SUBNET_CIDR = "10.0.0.0/24"
    }
}
