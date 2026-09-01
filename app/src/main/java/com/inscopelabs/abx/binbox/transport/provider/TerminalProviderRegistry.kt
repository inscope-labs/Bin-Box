package com.inscopelabs.abx.binbox.transport.provider

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import com.jcraft.jsch.HostKeyRepository

/**
 * Registry and coordinator for TerminalProviders (Phase 3 — Session & Transport Framework).
 */
class TerminalProviderRegistry {

    private val providers = mutableListOf<TerminalProvider>()

    fun registerProvider(provider: TerminalProvider) {
        synchronized(providers) {
            providers.removeAll { it.id == provider.id }
            providers.add(provider)
        }
        BinBoxLogger.i("TerminalProviderRegistry", "Registered provider: ${provider.displayName} [${provider.id}] for protocols ${provider.supportedProtocols}")
    }

    fun unregisterProvider(id: String) {
        synchronized(providers) {
            providers.removeAll { it.id == id }
        }
        BinBoxLogger.i("TerminalProviderRegistry", "Unregistered provider with ID: $id")
    }

    fun getProviderForProfile(profile: ConnectionProfile): TerminalProvider? {
        synchronized(providers) {
            return providers.firstOrNull { it.canHandle(profile) }
        }
    }

    fun getProviderForProtocol(protocol: ProtocolType): TerminalProvider? {
        synchronized(providers) {
            return providers.firstOrNull { it.supportedProtocols.contains(protocol) }
        }
    }

    fun getAllProviders(): List<TerminalProvider> {
        synchronized(providers) {
            return providers.toList()
        }
    }

    companion object {
        fun createDefault(
            keyRepository: IKeyRepository? = null,
            hostKeyRepository: HostKeyRepository? = null
        ): TerminalProviderRegistry {
            return TerminalProviderRegistry().apply {
                registerProvider(LocalShellTerminalProvider())
                registerProvider(SshTerminalProvider(keyRepository, hostKeyRepository))
                registerProvider(TcpTerminalProvider())
                registerProvider(WebSocketTerminalProvider())
            }
        }
    }
}
