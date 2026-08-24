package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.domain.model.TerminalSession
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import com.inscopelabs.abx.binbox.domain.repository.ISessionRepository
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TerminalSessionManager(
    private val sessionFactory: TerminalSessionFactory,
    private val sessionRepository: ISessionRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {

    private val _sessions = MutableStateFlow<List<ShellSession>>(emptyList())
    val sessions: StateFlow<List<ShellSession>> = _sessions.asStateFlow()

    private val _activeSessionIndex = MutableStateFlow(0)
    val activeSessionIndex: StateFlow<Int> = _activeSessionIndex.asStateFlow()

    private val sessionJobs = mutableMapOf<String, MutableList<kotlinx.coroutines.Job>>()

    val activeSession: ShellSession?
        get() = _sessions.value.getOrNull(_activeSessionIndex.value)

    suspend fun launchSession(
        profile: ConnectionProfile,
        shellProfile: ShellProfile = ShellProfile.DEFAULT,
        theme: TerminalThemePreset? = null
    ): AppResult<ShellSession> {
        return when (val createResult = sessionFactory.createSession(profile, shellProfile, theme)) {
            is AppResult.Success -> {
                val session = createResult.data
                _sessions.update { current -> current + session }
                val newIndex = _sessions.value.size - 1
                _activeSessionIndex.value = newIndex

                // Register with domain session repository if provided
                sessionRepository?.let { repo ->
                    repo.createSession(
                        TerminalSession(
                            sessionId = session.id,
                            profile = profile,
                            title = session.title,
                            state = TerminalSessionState.CONNECTING
                        )
                    )
                    repo.focusSession(session.id)
                }

                val jobs = mutableListOf<kotlinx.coroutines.Job>()

                // Observe session state transitions
                val stateJob = scope.launch {
                    session.state.collect { state ->
                        val domainState = when (state) {
                            is SessionState.Connecting -> TerminalSessionState.CONNECTING
                            is SessionState.Connected -> TerminalSessionState.CONNECTED
                            is SessionState.Disconnected -> TerminalSessionState.DISCONNECTED
                            is SessionState.Error -> TerminalSessionState.FAILED
                        }
                        val errorMsg = (state as? SessionState.Error)?.message
                        sessionRepository?.updateSessionState(session.id, domainState, errorMsg)
                    }
                }
                jobs.add(stateJob)

                // Observe traffic
                val trafficJob = scope.launch {
                    session.bytesReceived.collect { received ->
                        sessionRepository?.recordSessionTraffic(session.id, received, session.bytesSent.value)
                    }
                }
                jobs.add(trafficJob)
                sessionJobs[session.id] = jobs

                // Start execution
                session.start()
                BinBoxLogger.i("TerminalSessionManager", "Started terminal session ${session.id} for ${profile.label}")
                AppResult.Success(session)
            }
            is AppResult.Error -> {
                BinBoxLogger.e("TerminalSessionManager", "Failed to launch session for ${profile.label}: ${createResult.error.userMessage}")
                createResult
            }
            is AppResult.Loading -> {
                AppResult.Loading
            }
        }
    }

    fun selectSession(index: Int) {
        if (index in _sessions.value.indices) {
            _activeSessionIndex.value = index
            val session = _sessions.value[index]
            sessionRepository?.focusSession(session.id)
            BinBoxLogger.d("TerminalSessionManager", "Selected session at index $index: ${session.title}")
        }
    }

    fun closeSession(index: Int) {
        val currentList = _sessions.value.toMutableList()
        if (index in currentList.indices) {
            val session = currentList.removeAt(index)
            session.disconnect()
            sessionJobs.remove(session.id)?.forEach { it.cancel() }
            sessionRepository?.closeSession(session.id)
            _sessions.value = currentList

            val currentIndex = _activeSessionIndex.value
            if (currentIndex >= currentList.size) {
                _activeSessionIndex.value = (currentList.size - 1).coerceAtLeast(0)
            }
            BinBoxLogger.i("TerminalSessionManager", "Closed session: ${session.title}")
        }
    }

    fun closeActiveSession() {
        closeSession(_activeSessionIndex.value)
    }

    fun sendInputToActive(text: String) {
        activeSession?.sendInput(text)
    }

    fun sendSpecialKeyToActive(key: TerminalKey) {
        activeSession?.sendSpecialKey(key)
    }

    fun sendRawBytesToActive(bytes: ByteArray) {
        activeSession?.sendRawBytes(bytes)
    }

    fun clearActiveTerminal() {
        activeSession?.clear()
    }

    fun resetActiveTerminal() {
        activeSession?.reset()
    }

    fun searchActiveTerminal(query: String, ignoreCase: Boolean = true): com.inscopelabs.abx.binbox.terminal.model.TerminalSearchResults {
        return activeSession?.search(query, ignoreCase) ?: com.inscopelabs.abx.binbox.terminal.model.TerminalSearchResults(query)
    }

    fun sendFunctionKeyToActive(fIndex: Int) {
        activeSession?.sendFunctionKey(fIndex)
    }

    fun resizeActiveTerminal(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {
        activeSession?.let { session ->
            session.resize(cols, rows, widthPx, heightPx)
            sessionRepository?.updateSessionDimensions(session.id, cols, rows)
        }
    }

    fun updateTheme(theme: TerminalThemePreset) {
        _sessions.value.forEach { it.updateTheme(theme) }
    }

    fun closeAllSessions() {
        _sessions.value.forEach { it.disconnect() }
        sessionJobs.values.forEach { list -> list.forEach { it.cancel() } }
        sessionJobs.clear()
        _sessions.value = emptyList()
        _activeSessionIndex.value = 0
        sessionRepository?.closeAllSessions()
    }
}
