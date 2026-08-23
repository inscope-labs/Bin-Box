package com.inscopelabs.abx.binbox.domain.model

import java.util.UUID

/**
 * Encapsulates an active or persisted terminal session lifecycle and buffer state.
 */
data class TerminalSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val profile: ConnectionProfile,
    val state: TerminalSessionState = TerminalSessionState.DISCONNECTED,
    val title: String = profile.label,
    val shellProfile: ShellProfile = ShellProfile.DEFAULT,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val cols: Int = 80,
    val rows: Int = 24,
    val totalBytesReceived: Long = 0L,
    val totalBytesSent: Long = 0L,
    val lastError: String? = null
) {
    val isConnected: Boolean
        get() = state == TerminalSessionState.CONNECTED

    val isTerminal: Boolean
        get() = state == TerminalSessionState.FAILED || state == TerminalSessionState.TERMINATED
}
