package com.inscopelabs.abx.binbox.transport.provider

import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.SshTransport
import com.jcraft.jsch.HostKeyRepository

/**
 * Provider for SSH connections backed by JSch.
 */
class SshTerminalProvider(
    private val keyRepository: IKeyRepository? = null,
    private val hostKeyRepository: HostKeyRepository? = null
) : TerminalProvider {
    override val id: String = "provider_ssh"
    override val displayName: String = "SSH Provider"
    override val supportedProtocols: Set<ProtocolType> = setOf(ProtocolType.SSH)

    override suspend fun createTransport(
        profile: ConnectionProfile,
        shellProfile: ShellProfile
    ): ITransport {
        var privateKeyContent: String? = null
        if (profile.authType == AuthType.PRIVATE_KEY && profile.keyId != null && keyRepository != null) {
            val key = keyRepository.getKeyById(profile.keyId)
            privateKeyContent = key?.privateKey
        }

        return SshTransport(
            host = profile.host,
            port = if (profile.port > 0) profile.port else 22,
            username = profile.username.ifBlank { "root" },
            password = profile.password,
            privateKey = privateKeyContent,
            privateKeyPassphrase = profile.keyPassphrase,
            hostKeyRepository = hostKeyRepository
        )
    }
}
