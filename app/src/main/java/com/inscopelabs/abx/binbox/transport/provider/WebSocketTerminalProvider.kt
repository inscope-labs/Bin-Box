package com.inscopelabs.abx.binbox.transport.provider

import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.backend.WebSocketTransport

/**
 * Provider for WebSocket streaming backend connections.
 */
class WebSocketTerminalProvider : TerminalProvider {
    override val id: String = "provider_websocket"
    override val displayName: String = "WebSocket Provider"
    override val supportedProtocols: Set<ProtocolType> = setOf(ProtocolType.WEBSOCKET)

    override suspend fun createTransport(
        profile: ConnectionProfile,
        shellProfile: ShellProfile
    ): ITransport {
        val wsUrl = if (profile.host.startsWith("ws://") || profile.host.startsWith("wss://")) {
            profile.host
        } else {
            val scheme = if (profile.port == 443 || profile.port == 8443) "wss" else "ws"
            val portPart = if (profile.port > 0) ":${profile.port}" else ""
            "$scheme://${profile.host}$portPart/ws/terminal"
        }

        return WebSocketTransport(
            url = wsUrl,
            authToken = profile.password,
            heartbeatIntervalSeconds = profile.keepAliveSeconds.toLong().coerceAtLeast(5L)
        )
    }
}
