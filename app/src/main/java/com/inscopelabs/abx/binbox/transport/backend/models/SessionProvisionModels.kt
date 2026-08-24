package com.inscopelabs.abx.binbox.transport.backend.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for creating/provisioning an interactive terminal session via backend gateway.
 */
@JsonClass(generateAdapter = true)
data class ProvisionSessionRequest(
    @Json(name = "targetHost") val targetHost: String = "localhost",
    @Json(name = "port") val port: Int = 22,
    @Json(name = "protocol") val protocol: String = "SSH",
    @Json(name = "username") val username: String = "root",
    @Json(name = "password") val password: String? = null,
    @Json(name = "privateKey") val privateKey: String? = null,
    @Json(name = "initialCols") val initialCols: Int = 80,
    @Json(name = "initialRows") val initialRows: Int = 24,
    @Json(name = "termType") val termType: String = "xterm-256color",
    @Json(name = "env") val env: Map<String, String>? = null,
    @Json(name = "startupCommand") val startupCommand: String? = null
)

/**
 * Response payload returning the allocated WebSocket URL, session ID, and authentication token.
 */
@JsonClass(generateAdapter = true)
data class ProvisionSessionResponse(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "websocketUrl") val websocketUrl: String,
    @Json(name = "authToken") val authToken: String,
    @Json(name = "expiresAt") val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    @Json(name = "status") val status: String = "READY"
)

/**
 * Request payload for session heartbeat / keep-alive.
 */
@JsonClass(generateAdapter = true)
data class SessionHeartbeatRequest(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

/**
 * Response payload for session heartbeat.
 */
@JsonClass(generateAdapter = true)
data class SessionHeartbeatResponse(
    @Json(name = "sessionId") val sessionId: String,
    @Json(name = "acknowledged") val acknowledged: Boolean = true,
    @Json(name = "serverTime") val serverTime: Long = System.currentTimeMillis()
)

/**
 * Backend API error response body.
 */
@JsonClass(generateAdapter = true)
data class BackendErrorResponse(
    @Json(name = "code") val code: String = "INTERNAL_ERROR",
    @Json(name = "message") val message: String,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
