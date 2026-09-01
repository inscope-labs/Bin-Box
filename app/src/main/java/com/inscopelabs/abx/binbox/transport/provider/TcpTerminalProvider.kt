package com.inscopelabs.abx.binbox.transport.provider

import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.TcpTransport

/**
 * Provider for raw TCP / Telnet connections.
 */
class TcpTerminalProvider : TerminalProvider {
    override val id: String = "provider_tcp"
    override val displayName: String = "TCP / Telnet Provider"
    override val supportedProtocols: Set<ProtocolType> = setOf(
        ProtocolType.TELNET,
        ProtocolType.CUSTOM_SOCKET
    )

    override suspend fun createTransport(
        profile: ConnectionProfile,
        shellProfile: ShellProfile
    ): ITransport {
        val defaultPort = if (profile.protocol == ProtocolType.TELNET) 23 else 8080
        val port = if (profile.port > 0) profile.port else defaultPort
        return TcpTransport(
            host = profile.host,
            port = port
        )
    }
}
