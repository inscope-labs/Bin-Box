package com.inscopelabs.abx.binbox.transport.provider

import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.transport.ITransport

/**
 * Pluggable terminal transport provider (Phase 3 — Session & Transport Framework).
 * Decouples transport instantiation from session management.
 */
interface TerminalProvider {
    val id: String
    val displayName: String
    val supportedProtocols: Set<ProtocolType>

    /**
     * Determines whether this provider can handle the given connection profile.
     */
    fun canHandle(profile: ConnectionProfile): Boolean {
        return supportedProtocols.contains(profile.protocol)
    }

    /**
     * Creates an [ITransport] instance configured for the supplied profiles.
     */
    suspend fun createTransport(
        profile: ConnectionProfile,
        shellProfile: ShellProfile = ShellProfile.DEFAULT
    ): ITransport
}
