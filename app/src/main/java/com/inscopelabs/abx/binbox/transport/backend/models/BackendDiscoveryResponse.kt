package com.inscopelabs.abx.binbox.transport.backend.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Discovery metadata returned by the BinBox / ABX Backend Gateway.
 */
@JsonClass(generateAdapter = true)
data class BackendDiscoveryResponse(
    @Json(name = "serverVersion") val serverVersion: String = "1.0.0",
    @Json(name = "gatewayName") val gatewayName: String = "ABX-Gateway",
    @Json(name = "capabilities") val capabilities: List<String> = listOf("ws_pty", "resize", "heartbeat", "token_auth"),
    @Json(name = "supportedProtocols") val supportedProtocols: List<String> = listOf("ssh", "websocket", "raw_tcp"),
    @Json(name = "availableRegions") val availableRegions: List<String> = listOf("us-ashburn-1", "us-phoenix-1", "eu-frankfurt-1"),
    @Json(name = "activeSessionsCount") val activeSessionsCount: Int = 0,
    @Json(name = "status") val status: String = "HEALTHY"
)
