package com.inscopelabs.abx.binbox.oci.api.networking

/** Confirmed exactly against docs.oracle.com/en-us/iaas/Content/API/Concepts/usingapi.htm's CreateVcn example. */
data class CreateVcnRequest(
    val compartmentId: String,
    val displayName: String,
    val cidrBlock: String
)

data class Vcn(
    val id: String,
    val compartmentId: String,
    val displayName: String,
    val cidrBlock: String,
    val defaultRouteTableId: String,
    val defaultSecurityListId: String,
    val defaultDhcpOptionsId: String,
    val lifecycleState: String,
    val timeCreated: String
)

data class CreateSubnetRequest(
    val compartmentId: String,
    val vcnId: String,
    val displayName: String,
    val cidrBlock: String,
    val routeTableId: String? = null
)

data class Subnet(
    val id: String,
    val compartmentId: String,
    val vcnId: String,
    val displayName: String,
    val cidrBlock: String,
    val routeTableId: String,
    val lifecycleState: String,
    val timeCreated: String
)

data class CreateInternetGatewayRequest(
    val compartmentId: String,
    val vcnId: String,
    val displayName: String,
    val isEnabled: Boolean = true
)

data class InternetGateway(
    val id: String,
    val compartmentId: String,
    val vcnId: String,
    val displayName: String,
    val isEnabled: Boolean,
    val lifecycleState: String,
    val timeCreated: String
)

/**
 * One route table rule. Confirmed shape:
 * `[{"cidrBlock":"0.0.0.0/0","networkEntityId":"ocid1.internetgateway..."}]`
 * — for internet-bound default routing, [cidrBlock] is "0.0.0.0/0" and
 * [networkEntityId] is the internet gateway's OCID (§18).
 */
data class RouteRule(
    val cidrBlock: String,
    val networkEntityId: String
)

data class CreateRouteTableRequest(
    val compartmentId: String,
    val vcnId: String,
    val displayName: String,
    val routeRules: List<RouteRule>
)

data class RouteTable(
    val id: String,
    val compartmentId: String,
    val vcnId: String,
    val displayName: String,
    val routeRules: List<RouteRule>,
    val lifecycleState: String,
    val timeCreated: String
)

/**
 * UpdateRouteTable's `routeRules` REPLACES the entire existing rule set —
 * confirmed from the OCI CLI docs' explicit note on this. Callers must
 * pass the full desired rule list, not just the delta.
 */
data class UpdateRouteTableRequest(
    val routeRules: List<RouteRule>
)
