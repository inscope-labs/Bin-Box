package com.inscopelabs.abx.binbox.domain.usecase

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.TerminalSession
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import com.inscopelabs.abx.binbox.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.StateFlow

class ManageSessionUseCase(private val sessionRepository: ISessionRepository) {

    val activeSessions: StateFlow<List<TerminalSession>> = sessionRepository.activeSessions
    val focusedSessionId: StateFlow<String?> = sessionRepository.focusedSessionId

    fun startSession(profile: ConnectionProfile): AppResult<TerminalSession> {
        val newSession = TerminalSession(
            profile = profile,
            state = TerminalSessionState.CONNECTING
        )
        return sessionRepository.createSession(newSession)
    }

    fun focusSession(sessionId: String): AppResult<Unit> =
        sessionRepository.focusSession(sessionId)

    fun renameSession(sessionId: String, newTitle: String): AppResult<Unit> =
        sessionRepository.renameSession(sessionId, newTitle)

    fun updateState(sessionId: String, state: TerminalSessionState, error: String? = null): AppResult<Unit> =
        sessionRepository.updateSessionState(sessionId, state, error)

    fun recordTraffic(sessionId: String, bytesIn: Long, bytesOut: Long): AppResult<Unit> =
        sessionRepository.recordSessionTraffic(sessionId, bytesIn, bytesOut)

    fun closeSession(sessionId: String): AppResult<Unit> =
        sessionRepository.closeSession(sessionId)

    fun closeAllSessions(): AppResult<Unit> =
        sessionRepository.closeAllSessions()
}
