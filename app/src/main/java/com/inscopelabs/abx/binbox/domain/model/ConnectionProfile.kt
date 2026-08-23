package com.inscopelabs.abx.binbox.domain.model

/**
 * Provider-independent connection profile definition.
 */
data class ConnectionProfile(
    val id: Long = 0,
    val label: String,
    val host: String,
    val port: Int = 22,
    val protocol: ProtocolType = ProtocolType.SSH,
    val username: String = "root",
    val authType: AuthType = AuthType.PASSWORD,
    val password: String? = null,
    val keyId: Long? = null,
    val keyPassphrase: String? = null,
    val groupTag: String = "Cloud",
    val themeId: String = "monokai_pro",
    val startupCommand: String? = null,
    val keepAliveSeconds: Int = 30,
    val isFavorite: Boolean = false,
    val lastLatencyMs: Long? = null,
    val lastConnectedAt: Long? = null
) {
    val displayEndpoint: String
        get() = when (protocol) {
            ProtocolType.LOCAL_SHELL -> "localhost (Local PTY)"
            ProtocolType.DEMO_HOST -> "demo.sandbox (Simulated)"
            else -> "$username@$host:$port"
        }
}
