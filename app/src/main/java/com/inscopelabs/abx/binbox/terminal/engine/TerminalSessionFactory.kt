package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes

class TerminalSessionFactory(
    private val keyRepository: IKeyRepository? = null,
    private val onBell: (() -> Unit)? = null
) {

    suspend fun createSession(
        profile: ConnectionProfile,
        shellProfile: ShellProfile = ShellProfile.DEFAULT,
        themeOverride: TerminalThemePreset? = null
    ): AppResult<ShellSession> {
        return try {
            val theme = themeOverride ?: TerminalThemes.getThemeById(profile.themeId)

            val session: ShellSession = when (profile.protocol) {
                ProtocolType.LOCAL_SHELL -> {
                    val workingDir = shellProfile.initialDirectory?.takeIf { it.isNotBlank() }?.let { java.io.File(it) }
                    val customCommand = if (shellProfile.shellPath.isNotBlank() && shellProfile.id != "default") {
                        listOf(shellProfile.shellPath)
                    } else null

                    LocalShellSession(
                        title = profile.label.ifBlank { "Local Shell" },
                        hostLabel = "localhost",
                        command = customCommand,
                        workingDir = workingDir,
                        environment = shellProfile.envVars.takeIf { it.isNotEmpty() },
                        initialTheme = theme,
                        onBell = onBell
                    )
                }

                ProtocolType.DEMO_HOST -> {
                    SandboxDemoShellSession(
                        title = profile.label.ifBlank { "Sandbox Linux Host" },
                        hostLabel = profile.host.ifBlank { "demo.binbox.io" },
                        initialTheme = theme,
                        onBell = onBell
                    )
                }

                ProtocolType.TELNET, ProtocolType.CUSTOM_SOCKET -> {
                    TelnetShellSession(
                        title = profile.label,
                        hostLabel = profile.host,
                        host = profile.host,
                        port = if (profile.port > 0) profile.port else 23,
                        initialTheme = theme,
                        onBell = onBell
                    )
                }

                ProtocolType.SSH -> {
                    var privateKeyContent: String? = null
                    if (profile.authType == AuthType.PRIVATE_KEY && profile.keyId != null && keyRepository != null) {
                        val key = keyRepository.getKeyById(profile.keyId)
                        privateKeyContent = key?.privateKey
                    }

                    SshShellSession(
                        title = profile.label,
                        hostLabel = profile.host,
                        host = profile.host,
                        port = if (profile.port > 0) profile.port else 22,
                        username = profile.username.ifBlank { "root" },
                        password = profile.password,
                        privateKey = privateKeyContent,
                        privateKeyPassphrase = profile.keyPassphrase,
                        initialTheme = theme,
                        onBell = onBell
                    )
                }
            }

            BinBoxLogger.i(
                "TerminalSessionFactory",
                "Created ${session::class.simpleName} for profile ${profile.label} [${profile.protocol}]"
            )
            AppResult.Success(session)
        } catch (e: Throwable) {
            BinBoxLogger.e("TerminalSessionFactory", "Failed to construct shell session for ${profile.label}", e)
            AppResult.Error(AppError.SessionError("Failed to initialize session: ${e.message}", e))
        }
    }
}
