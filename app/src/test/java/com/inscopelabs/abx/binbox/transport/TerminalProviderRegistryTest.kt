package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.transport.provider.LocalShellTerminalProvider
import com.inscopelabs.abx.binbox.transport.provider.SshTerminalProvider
import com.inscopelabs.abx.binbox.transport.provider.TcpTerminalProvider
import com.inscopelabs.abx.binbox.transport.provider.TerminalProviderRegistry
import com.inscopelabs.abx.binbox.transport.provider.WebSocketTerminalProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalProviderRegistryTest {

    @Test
    fun terminalProviderRegistry_registersAndFindsProvidersCorrectly() {
        val registry = TerminalProviderRegistry.createDefault()

        val sshProfile = ConnectionProfile(
            label = "My SSH",
            host = "1.2.3.4",
            protocol = ProtocolType.SSH
        )
        val sshProvider = registry.getProviderForProfile(sshProfile)
        assertNotNull(sshProvider)
        assertTrue(sshProvider is SshTerminalProvider)

        val localProfile = ConnectionProfile(
            label = "My Local",
            host = "localhost",
            protocol = ProtocolType.LOCAL_SHELL
        )
        val localProvider = registry.getProviderForProfile(localProfile)
        assertNotNull(localProvider)
        assertTrue(localProvider is LocalShellTerminalProvider)

        val tcpProfile = ConnectionProfile(
            label = "Telnet Switch",
            host = "192.168.1.1",
            protocol = ProtocolType.TELNET
        )
        val tcpProvider = registry.getProviderForProfile(tcpProfile)
        assertNotNull(tcpProvider)
        assertTrue(tcpProvider is TcpTerminalProvider)

        val wsProfile = ConnectionProfile(
            label = "Backend Relay",
            host = "backend.binbox.io",
            protocol = ProtocolType.WEBSOCKET
        )
        val wsProvider = registry.getProviderForProfile(wsProfile)
        assertNotNull(wsProvider)
        assertTrue(wsProvider is WebSocketTerminalProvider)
    }

    @Test
    fun terminalProviderRegistry_unregistersProvider() {
        val registry = TerminalProviderRegistry()
        val local = LocalShellTerminalProvider()
        registry.registerProvider(local)

        assertEquals(1, registry.getAllProviders().size)
        assertNotNull(registry.getProviderForProtocol(ProtocolType.LOCAL_SHELL))

        registry.unregisterProvider(local.id)
        assertEquals(0, registry.getAllProviders().size)
        assertNull(registry.getProviderForProtocol(ProtocolType.LOCAL_SHELL))
    }
}
