package com.inscopelabs.abx.binbox.transport.backend.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Standard frame types supported by the BinBox WebSocket Terminal Protocol.
 */
enum class WsFrameType {
    AUTH,
    AUTH_OK,
    DATA,
    RESIZE,
    PING,
    PONG,
    HEARTBEAT,
    EXIT,
    ERROR,
    TELEMETRY
}

/**
 * Structured WebSocket frame for multiplexed or structured terminal communications.
 */
@JsonClass(generateAdapter = true)
data class WsTerminalFrame(
    @Json(name = "type") val type: String,
    @Json(name = "sessionId") val sessionId: String? = null,
    @Json(name = "payload") val payload: String? = null,
    @Json(name = "isBase64") val isBase64: Boolean = false,
    @Json(name = "cols") val cols: Int? = null,
    @Json(name = "rows") val rows: Int? = null,
    @Json(name = "widthPx") val widthPx: Int? = null,
    @Json(name = "heightPx") val heightPx: Int? = null,
    @Json(name = "exitCode") val exitCode: Int? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
    @Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
) {
    val frameType: WsFrameType
        get() = try {
            WsFrameType.valueOf(type.uppercase())
        } catch (_: Exception) {
            WsFrameType.DATA
        }

    companion object {
        fun auth(sessionId: String, token: String): WsTerminalFrame = WsTerminalFrame(
            type = WsFrameType.AUTH.name,
            sessionId = sessionId,
            token = token
        )

        fun data(payload: String, sessionId: String? = null, isBase64: Boolean = false): WsTerminalFrame = WsTerminalFrame(
            type = WsFrameType.DATA.name,
            sessionId = sessionId,
            payload = payload,
            isBase64 = isBase64
        )

        fun resize(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0, sessionId: String? = null): WsTerminalFrame = WsTerminalFrame(
            type = WsFrameType.RESIZE.name,
            sessionId = sessionId,
            cols = cols,
            rows = rows,
            widthPx = widthPx,
            heightPx = heightPx
        )

        fun ping(sessionId: String? = null): WsTerminalFrame = WsTerminalFrame(
            type = WsFrameType.PING.name,
            sessionId = sessionId
        )

        fun pong(sessionId: String? = null): WsTerminalFrame = WsTerminalFrame(
            type = WsFrameType.PONG.name,
            sessionId = sessionId
        )
    }
}
