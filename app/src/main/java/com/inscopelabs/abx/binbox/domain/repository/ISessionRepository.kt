package com.inscopelabs.abx.binbox.domain.repository

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.TerminalSession
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ISessionRepository {
    val activeSessions: StateFlow<List<TerminalSession>>
    val focusedSessionId: StateFlow<String?>

    fun getSession(sessionId: String): TerminalSession?
    fun createSession(session: TerminalSession): AppResult<TerminalSession>
    fun updateSessionState(sessionId: String, state: TerminalSessionState, error: String? = null): AppResult<Unit>
    fun updateSessionDimensions(sessionId: String, cols: Int, rows: Int): AppResult<Unit>
    fun recordSessionTraffic(sessionId: String, bytesIn: Long, bytesOut: Long): AppResult<Unit>
    fun focusSession(sessionId: String): AppResult<Unit>
    fun renameSession(sessionId: String, newTitle: String): AppResult<Unit>
    fun closeSession(sessionId: String): AppResult<Unit>
    fun closeAllSessions(): AppResult<Unit>
}
