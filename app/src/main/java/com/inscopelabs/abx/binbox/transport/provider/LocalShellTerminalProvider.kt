package com.inscopelabs.abx.binbox.transport.provider

import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.LocalProcessTransport
import java.io.File

/**
 * Provider for local shell and Termux process execution.
 */
class LocalShellTerminalProvider : TerminalProvider {
    override val id: String = "provider_local_shell"
    override val displayName: String = "Local Shell Provider"
    override val supportedProtocols: Set<ProtocolType> = setOf(ProtocolType.LOCAL_SHELL)

    override suspend fun createTransport(
        profile: ConnectionProfile,
        shellProfile: ShellProfile
    ): ITransport {
        val workingDir = shellProfile.initialDirectory?.takeIf { it.isNotBlank() }?.let { File(it) }
        val customCommand = if (shellProfile.shellPath.isNotBlank() && shellProfile.id != "default") {
            listOf(shellProfile.shellPath)
        } else null

        return LocalProcessTransport(
            command = customCommand,
            workingDir = workingDir,
            environment = shellProfile.envVars.takeIf { it.isNotEmpty() }
        )
    }
}
