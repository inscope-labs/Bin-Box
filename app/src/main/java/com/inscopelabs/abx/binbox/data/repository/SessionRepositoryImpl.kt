package com.inscopelabs.abx.binbox.data.repository

import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.TerminalSession
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import com.inscopelabs.abx.binbox.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

class SessionRepositoryImpl : ISessionRepository {

    private val sessionMap = ConcurrentHashMap<String, TerminalSession>()

    private val _activeSessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    override val activeSessions: StateFlow<List<TerminalSession>> = _activeSessions.asStateFlow()

    private val _focusedSessionId = MutableStateFlow<String?>(null)
    override val focusedSessionId: StateFlow<String?> = _focusedSessionId.asStateFlow()

    private fun syncStateFlow() {
        _activeSessions.update { sessionMap.values.sortedBy { it.createdAt } }
    }

    override fun getSession(sessionId: String): TerminalSession? {
        return sessionMap[sessionId]
    }

    override fun createSession(session: TerminalSession): AppResult<TerminalSession> {
        return try {
            sessionMap[session.sessionId] = session
            syncStateFlow()
            if (_focusedSessionId.value == null) {
                _focusedSessionId.value = session.sessionId
            }
            BinBoxLogger.i("SessionRepository", "Created session: ${session.sessionId} for ${session.profile.label}")
            AppResult.Success(session)
        } catch (e: Throwable) {
            BinBoxLogger.e("SessionRepository", "Failed to create session", e)
            AppResult.Error(AppError.SessionError("Failed to register session", e))
        }
    }

    override fun updateSessionState(
        sessionId: String,
        state: TerminalSessionState,
        error: String?
    ): AppResult<Unit> {
        val session = sessionMap[sessionId] ?: return AppResult.Error(
            AppError.SessionError("Session $sessionId not found")
        )
        val updated = session.copy(
            state = state,
            lastActiveAt = System.currentTimeMillis(),
            lastError = error ?: session.lastError
        )
        sessionMap[sessionId] = updated
        syncStateFlow()
        BinBoxLogger.d("SessionRepository", "Session $sessionId state changed to $state")
        return AppResult.Success(Unit)
    }

    override fun updateSessionDimensions(sessionId: String, cols: Int, rows: Int): AppResult<Unit> {
        val session = sessionMap[sessionId] ?: return AppResult.Error(
            AppError.SessionError("Session $sessionId not found")
        )
        val updated = session.copy(cols = cols, rows = rows)
        sessionMap[sessionId] = updated
        syncStateFlow()
        return AppResult.Success(Unit)
    }

    override fun recordSessionTraffic(sessionId: String, bytesIn: Long, bytesOut: Long): AppResult<Unit> {
        val session = sessionMap[sessionId] ?: return AppResult.Error(
            AppError.SessionError("Session $sessionId not found")
        )
        val updated = session.copy(
            totalBytesReceived = session.totalBytesReceived + bytesIn,
            totalBytesSent = session.totalBytesSent + bytesOut,
            lastActiveAt = System.currentTimeMillis()
        )
        sessionMap[sessionId] = updated
        syncStateFlow()
        return AppResult.Success(Unit)
    }

    override fun focusSession(sessionId: String): AppResult<Unit> {
        if (!sessionMap.containsKey(sessionId)) {
            return AppResult.Error(AppError.SessionError("Cannot focus non-existent session: $sessionId"))
        }
        _focusedSessionId.value = sessionId
        BinBoxLogger.d("SessionRepository", "Focused session: $sessionId")
        return AppResult.Success(Unit)
    }

    override fun renameSession(sessionId: String, newTitle: String): AppResult<Unit> {
        val session = sessionMap[sessionId] ?: return AppResult.Error(
            AppError.SessionError("Session $sessionId not found for rename")
        )
        sessionMap[sessionId] = session.copy(title = newTitle, lastActiveAt = System.currentTimeMillis())
        syncStateFlow()
        BinBoxLogger.d("SessionRepository", "Renamed session $sessionId to $newTitle")
        return AppResult.Success(Unit)
    }

    override fun closeSession(sessionId: String): AppResult<Unit> {
        val removed = sessionMap.remove(sessionId)
        if (removed != null) {
            syncStateFlow()
            if (_focusedSessionId.value == sessionId) {
                _focusedSessionId.value = sessionMap.keys.firstOrNull()
            }
            BinBoxLogger.i("SessionRepository", "Closed session: $sessionId")
            return AppResult.Success(Unit)
        }
        return AppResult.Error(AppError.SessionError("Session $sessionId not found for closing"))
    }

    override fun closeAllSessions(): AppResult<Unit> {
        sessionMap.clear()
        _focusedSessionId.value = null
        syncStateFlow()
        BinBoxLogger.i("SessionRepository", "Closed all active sessions")
        return AppResult.Success(Unit)
    }
}
