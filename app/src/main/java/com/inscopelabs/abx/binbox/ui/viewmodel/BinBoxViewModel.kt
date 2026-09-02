package com.inscopelabs.abx.binbox.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.abx.binbox.core.diagnostics.DeviceDiagnostics
import com.inscopelabs.abx.binbox.core.diagnostics.SessionMetrics
import com.inscopelabs.abx.binbox.core.logging.LogEntry
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.ShellProfile
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.inscopelabs.abx.binbox.domain.model.Workspace
import com.inscopelabs.abx.binbox.domain.usecase.HistoryUseCases
import com.inscopelabs.abx.binbox.domain.usecase.HostUseCases
import com.inscopelabs.abx.binbox.domain.usecase.KeyUseCases
import com.inscopelabs.abx.binbox.domain.usecase.ManageSessionUseCase
import com.inscopelabs.abx.binbox.domain.usecase.SnippetUseCases
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.transport.backend.models.BackendDiscoveryResponse
import com.inscopelabs.abx.binbox.ui.i18n.AppLanguage
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.viewmodel.delegates.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    TERMINAL("Terminal"),
    HOSTS("Hosts"),
    SNIPPETS("Scripts"),
    KEYS("Keys"),
    SETTINGS("Settings")
}

data class ServerTelemetry(
    val hostLabel: String,
    val osInfo: String,
    val uptime: String,
    val cpuUsage: String,
    val memUsage: String,
    val diskUsage: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BinBoxViewModel(
    application: Application,
    val hostUseCases: HostUseCases,
    val keyUseCases: KeyUseCases,
    val snippetUseCases: SnippetUseCases,
    val historyUseCases: HistoryUseCases,
    val manageSessionUseCase: ManageSessionUseCase,
    val sessionManager: TerminalSessionManager
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        graph = DependencyGraph.create(application)
    )

    private constructor(
        application: Application,
        graph: DependencyGraph
    ) : this(
        application = application,
        hostUseCases = graph.hostUseCases,
        keyUseCases = graph.keyUseCases,
        snippetUseCases = graph.snippetUseCases,
        historyUseCases = graph.historyUseCases,
        manageSessionUseCase = graph.manageSessionUseCase,
        sessionManager = graph.sessionManager
    )

    // Status Toast / Snackbar Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(msg: String) { _snackbarMessage.value = msg }
    fun clearSnackbar() { _snackbarMessage.value = null }

    // Delegates
    private val preferencesManager = TerminalPreferencesManager(application, viewModelScope)
    private val workspaceManager = WorkspaceStateManager()
    private val searchManager = SearchStateManager()
    private val hapticHelper = HapticFeedbackHelper(application)
    private val hostOps = HostOperationsManager(hostUseCases, viewModelScope, ::showSnackbar)
    private val keyOps = KeyOperationsManager(keyUseCases, viewModelScope, ::showSnackbar)
    private val snippetOps = SnippetOperationsManager(snippetUseCases, historyUseCases, viewModelScope, ::showSnackbar)
    private val backendOps = BackendOperationsManager(viewModelScope)
    private val diagManager = BinBoxDiagnosticsManager(application, viewModelScope)

    // Reactive Domain & Entity Data Streams
    val domainHosts: StateFlow<List<ConnectionProfile>> = hostOps.domainHosts
    val connectionProfiles: StateFlow<List<ConnectionProfile>> = hostOps.domainHosts
    val hosts: StateFlow<List<HostEntity>> = hostOps.hosts
    val recentHosts: StateFlow<List<ConnectionProfile>> = hostOps.recentHosts
    val hostFilterTag: StateFlow<String> = hostOps.hostFilterTag
    val hostSearchQuery: StateFlow<String> = hostOps.hostSearchQuery

    val domainKeys: StateFlow<List<SshKey>> = keyOps.domainKeys
    val keys: StateFlow<List<KeyEntity>> = keyOps.keys

    val domainSnippets: StateFlow<List<Snippet>> = snippetOps.domainSnippets
    val snippets: StateFlow<List<SnippetEntity>> = snippetOps.snippets
    val domainHistory: StateFlow<List<CommandHistory>> = snippetOps.domainHistory
    val history: StateFlow<List<HistoryEntity>> = snippetOps.history
    val selectedSnippetForRun: StateFlow<SnippetEntity?> = snippetOps.selectedSnippetForRun

    // UI Navigation State
    private val _currentAppTab = MutableStateFlow(AppTab.TERMINAL)
    val currentAppTab: StateFlow<AppTab> = _currentAppTab.asStateFlow()

    // Terminal Appearance & Settings
    val appLanguage: StateFlow<AppLanguage> = preferencesManager.appLanguage
    val strings: StateFlow<AppStrings> = preferencesManager.strings
    val currentTheme: StateFlow<TerminalThemePreset> = preferencesManager.currentTheme
    val fontSizeSp: StateFlow<Int> = preferencesManager.fontSizeSp
    val cursorStyle: StateFlow<CursorStyle> = preferencesManager.cursorStyle
    val fontFamilyName: StateFlow<String> = preferencesManager.fontFamilyName
    val bellMode: StateFlow<String> = preferencesManager.bellMode
    val bufferLineLimit: StateFlow<Int> = preferencesManager.bufferLineLimit
    val wordWrapEnabled: StateFlow<Boolean> = preferencesManager.wordWrapEnabled
    val hapticFeedbackEnabled: StateFlow<Boolean> = preferencesManager.hapticFeedbackEnabled

    // Workspaces
    val workspaces: StateFlow<List<Workspace>> = workspaceManager.workspaces
    val activeWorkspace: StateFlow<Workspace> = workspaceManager.activeWorkspace
    val isWorkspaceDialogOpen: StateFlow<Boolean> = workspaceManager.isWorkspaceDialogOpen

    // Shell Profiles
    private val _shellProfiles = MutableStateFlow<List<ShellProfile>>(ShellProfile.ALL_PRESETS)
    val shellProfiles: StateFlow<List<ShellProfile>> = _shellProfiles.asStateFlow()

    private val _defaultShellProfile = MutableStateFlow<ShellProfile>(ShellProfile.DEFAULT)
    val defaultShellProfile: StateFlow<ShellProfile> = _defaultShellProfile.asStateFlow()

    // Active Sessions & Multi-Tabs
    val sessions: StateFlow<List<ShellSession>> = sessionManager.sessions
    val activeSessionIndex: StateFlow<Int> = sessionManager.activeSessionIndex

    val activeSession: StateFlow<ShellSession?> = combine(sessions, activeSessionIndex) { list, idx ->
        if (list.isNotEmpty() && idx in list.indices) list[idx] else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isSessionSwitcherOpen = MutableStateFlow(false)
    val isSessionSwitcherOpen: StateFlow<Boolean> = _isSessionSwitcherOpen.asStateFlow()

    private val _renameDialogSessionIndex = MutableStateFlow<Int?>(null)
    val renameDialogSessionIndex: StateFlow<Int?> = _renameDialogSessionIndex.asStateFlow()

    // Keypad Modifier States (Ctrl, Alt)
    private val _ctrlLatched = MutableStateFlow(false)
    val ctrlLatched: StateFlow<Boolean> = _ctrlLatched.asStateFlow()

    private val _altLatched = MutableStateFlow(false)
    val altLatched: StateFlow<Boolean> = _altLatched.asStateFlow()

    // Buffer Search
    val searchQuery: StateFlow<String> = searchManager.searchQuery
    val isSearching: StateFlow<Boolean> = searchManager.isSearching
    val isSearchCaseSensitive: StateFlow<Boolean> = searchManager.isSearchCaseSensitive
    val isSearchRegex: StateFlow<Boolean> = searchManager.isSearchRegex
    val searchMatchIndex: StateFlow<Int> = searchManager.searchMatchIndex
    val searchMatchTotal: StateFlow<Int> = searchManager.searchMatchTotal

    // Host Telemetry & Diagnostics
    val telemetry: StateFlow<ServerTelemetry?> = diagManager.telemetry
    val systemDiagnostics: StateFlow<DeviceDiagnostics?> = diagManager.systemDiagnostics
    val sessionMetrics: StateFlow<Map<String, SessionMetrics>> = diagManager.sessionMetrics
    val logEntries: StateFlow<List<LogEntry>> = diagManager.logEntries

    // Backend Gateway
    val backendDiscovery: StateFlow<BackendDiscoveryResponse?> = backendOps.backendDiscovery
    val backendInstances: StateFlow<List<VmStatus>> = backendOps.backendInstances
    val isBackendRefreshing: StateFlow<Boolean> = backendOps.isBackendRefreshing

    init {
        refreshBackendInstances()
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            if (sessions.value.isEmpty()) {
                openDemoSession()
            }
        }
    }

    // Navigation & Preferences Handlers
    fun setAppTab(tab: AppTab) { _currentAppTab.value = tab }

    fun setLanguage(lang: AppLanguage) {
        preferencesManager.setLanguage(lang)
        showSnackbar("Language updated: ${lang.displayName}")
    }

    fun resetPreferences() {
        preferencesManager.resetPreferences()
        sessionManager.updateTheme(currentTheme.value)
        showSnackbar("Preferences reset to defaults")
    }

    fun setTheme(theme: TerminalThemePreset) {
        preferencesManager.setTheme(theme)
        sessionManager.updateTheme(theme)
    }

    fun setFontSize(sizeSp: Int) = preferencesManager.setFontSize(sizeSp)
    fun setFontFamily(name: String) = preferencesManager.setFontFamily(name)
    fun setBellMode(mode: String) = preferencesManager.setBellMode(mode)
    fun setBufferLineLimit(limit: Int) = preferencesManager.setBufferLineLimit(limit)
    fun setWordWrapEnabled(enabled: Boolean) = preferencesManager.setWordWrapEnabled(enabled)
    fun setCursorStyle(style: CursorStyle) = preferencesManager.setCursorStyle(style)
    fun toggleHapticFeedback(enabled: Boolean) = preferencesManager.toggleHapticFeedback(enabled)

    // Workspace Management
    fun selectWorkspace(workspaceId: String) {
        val target = workspaceManager.selectWorkspace(workspaceId)
        if (target != null) showSnackbar("Active workspace: ${target.name}")
    }

    fun switchWorkspace(workspace: Workspace) {
        workspaceManager.switchWorkspace(workspace)
        showSnackbar("Active workspace: ${workspace.name}")
    }

    fun setWorkspaceDialogOpen(isOpen: Boolean) = workspaceManager.setWorkspaceDialogOpen(isOpen)
    fun openWorkspaceDialog() = workspaceManager.openWorkspaceDialog()
    fun closeWorkspaceDialog() = workspaceManager.closeWorkspaceDialog()

    fun createWorkspace(
        name: String,
        description: String = "",
        iconName: String = "Terminal",
        colorHex: String = "#38BDF8",
        hostIds: List<Long> = emptyList()
    ) {
        val newWs = workspaceManager.createWorkspace(name, description, iconName, colorHex, hostIds)
        showSnackbar("Created workspace: ${newWs.name}")
    }

    fun deleteWorkspace(workspaceId: String) {
        if (!workspaceManager.deleteWorkspace(workspaceId)) {
            showSnackbar("Cannot delete the only workspace")
        } else {
            showSnackbar("Workspace removed")
        }
    }

    fun launchAllInWorkspace(workspace: Workspace) {
        val assignedHosts = domainHosts.value.filter { workspace.hostProfileIds.contains(it.id) }
        if (assignedHosts.isEmpty()) {
            showSnackbar("No hosts configured in ${workspace.name}")
            return
        }
        viewModelScope.launch {
            assignedHosts.forEach { hostProfile ->
                val shellProfile = ShellProfile.getProfileById(hostProfile.shellProfileId)
                sessionManager.launchSession(hostProfile, shellProfile, currentTheme.value)
            }
            _currentAppTab.value = AppTab.TERMINAL
            showSnackbar("Launched ${assignedHosts.size} sessions in ${workspace.name}")
        }
    }

    fun setDefaultShellProfile(profile: ShellProfile) {
        _defaultShellProfile.value = profile
        showSnackbar("Default shell set to: ${profile.name}")
    }

    // Multi-Terminal UX & Tabs
    fun setSessionSwitcherOpen(isOpen: Boolean) { _isSessionSwitcherOpen.value = isOpen }
    fun openRenameDialog(index: Int) { _renameDialogSessionIndex.value = index }
    fun closeRenameDialog() { _renameDialogSessionIndex.value = null }

    fun renameSession(index: Int, newTitle: String) {
        sessionManager.renameSession(index, newTitle)
        _renameDialogSessionIndex.value = null
        showSnackbar("Session renamed to: $newTitle")
    }

    fun moveSession(fromIndex: Int, toIndex: Int) = sessionManager.moveSession(fromIndex, toIndex)

    fun duplicateSession(index: Int) {
        viewModelScope.launch {
            val result = sessionManager.duplicateSession(index)
            if (result is AppResult.Success) showSnackbar("Cloned session tab")
        }
    }

    fun closeAllSessions() {
        sessionManager.closeAllSessions()
        _isSessionSwitcherOpen.value = false
        showSnackbar("Closed all sessions")
    }

    // Host Filter Tag & Search
    fun setHostFilterTag(tag: String) = hostOps.setHostFilterTag(tag)
    fun setHostSearchQuery(query: String) = hostOps.setHostSearchQuery(query)

    // Search Navigation
    fun toggleSearchCaseSensitive() = searchManager.toggleSearchCaseSensitive()
    fun toggleSearchRegex() = searchManager.toggleSearchRegex()
    fun nextSearchMatch() = searchManager.nextSearchMatch()
    fun prevSearchMatch() = searchManager.prevSearchMatch()
    fun setSearchMatchStats(current: Int, total: Int) = searchManager.setSearchMatchStats(current, total)
    fun setSearchQuery(query: String) = searchManager.setSearchQuery(query)
    fun toggleSearching(searching: Boolean) = searchManager.toggleSearching(searching)

    fun toggleCtrl() {
        _ctrlLatched.value = !_ctrlLatched.value
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    fun toggleAlt() {
        _altLatched.value = !_altLatched.value
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    // Session Management Actions
    fun selectSession(index: Int) {
        sessionManager.selectSession(index)
        _currentAppTab.value = AppTab.TERMINAL
    }

    fun closeSession(index: Int) = sessionManager.closeSession(index)

    fun openDemoSession(): kotlinx.coroutines.Job = viewModelScope.launch {
        val demoProfile = ConnectionProfile(
            label = "Cloud Demo (SSH)",
            host = "vps-demo.binbox.io",
            protocol = ProtocolType.DEMO_HOST,
            themeId = currentTheme.value.id
        )
        sessionManager.launchSession(demoProfile, theme = currentTheme.value)
    }

    fun openLocalSession(): kotlinx.coroutines.Job = viewModelScope.launch {
        val localProfile = ConnectionProfile(
            label = "Local Device",
            host = "localhost",
            protocol = ProtocolType.LOCAL_SHELL,
            themeId = currentTheme.value.id
        )
        sessionManager.launchSession(localProfile, theme = currentTheme.value)
    }

    fun openWebSocketSession(
        url: String,
        label: String = "WS Relay Terminal",
        authToken: String? = null
    ): kotlinx.coroutines.Job = viewModelScope.launch {
        val wsProfile = ConnectionProfile(
            label = label,
            host = url,
            protocol = ProtocolType.WEBSOCKET,
            password = authToken,
            themeId = currentTheme.value.id
        )
        val result = sessionManager.launchSession(wsProfile, theme = currentTheme.value)
        if (result is AppResult.Success) {
            _currentAppTab.value = AppTab.TERMINAL
        } else if (result is AppResult.Error) {
            showSnackbar("WebSocket connection failed: ${result.error.userMessage}")
        }
    }

    fun refreshBackendInstances() = backendOps.refreshBackendInstances()

    fun connectToVmInstance(vm: VmStatus) = hostOps.connectToVmInstance(
        vm = vm,
        sessionManager = sessionManager,
        theme = currentTheme.value,
        onConnected = { _currentAppTab.value = AppTab.TERMINAL }
    )

    fun connectToHost(hostEntity: HostEntity): kotlinx.coroutines.Job = hostOps.connectToHost(
        hostEntity = hostEntity,
        sessionManager = sessionManager,
        theme = currentTheme.value,
        onConnected = { _currentAppTab.value = AppTab.TERMINAL }
    )

    // Terminal Input & Keypad Dispatch
    fun sendCommand(command: String) {
        val session = activeSession.value ?: return
        if (command.isNotBlank()) {
            viewModelScope.launch {
                historyUseCases.recordHistory(command, session.hostLabel)
            }
        }
        session.sendInput(command + "\n")
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    fun sendRawInput(text: String) {
        val session = activeSession.value ?: return
        if (_ctrlLatched.value) {
            _ctrlLatched.value = false
            if (text.length == 1) {
                val c = text.first().uppercaseChar()
                if (c in 'A'..'Z') {
                    val ctrlByte = (c.code - 64).toByte()
                    session.sendRawBytes(byteArrayOf(ctrlByte))
                    hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
                    return
                }
            }
        }
        session.sendInput(text)
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    fun sendSpecialKey(key: TerminalKey) {
        sessionManager.sendSpecialKeyToActive(key)
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    fun sendTextSnippet(text: String) {
        sessionManager.sendInputToActive(text)
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    fun clearCurrentTerminal() {
        sessionManager.clearActiveTerminal()
        hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
    }

    fun resizeTerminal(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {
        sessionManager.resizeActiveTerminal(cols, rows, widthPx, heightPx)
    }

    // Snippets & Quick Scripts
    fun openSnippetDialog(snippet: SnippetEntity) = snippetOps.openSnippetDialog(snippet)
    fun dismissSnippetDialog() = snippetOps.dismissSnippetDialog()

    fun executeSnippet(snippet: SnippetEntity, resolvedCommand: String) {
        snippetOps.executeSnippet(
            snippet = snippet,
            resolvedCommand = resolvedCommand,
            targetHostLabel = activeSession.value?.hostLabel ?: "Terminal",
            sessionManager = sessionManager,
            onExecuted = {
                _currentAppTab.value = AppTab.TERMINAL
                hapticHelper.triggerHaptic(hapticFeedbackEnabled.value)
            }
        )
    }

    fun saveSnippet(snippet: SnippetEntity) = snippetOps.saveSnippet(snippet)
    fun deleteSnippet(snippet: SnippetEntity) = snippetOps.deleteSnippet(snippet)

    // Hosts Operations
    fun saveHost(host: HostEntity) = hostOps.saveHost(host)
    fun deleteHost(host: HostEntity) = hostOps.deleteHost(host) { hostId ->
        workspaceManager.removeHostFromWorkspaces(hostId)
    }
    fun toggleHostFavorite(host: HostEntity) = hostOps.toggleHostFavorite(host)
    fun pingHost(host: HostEntity) = hostOps.pingHost(host)
    fun pingAllHosts() = hostOps.pingAllHosts()

    // SSH Key Management
    fun generateRsaKey(title: String, keySize: Int = 2048) = keyOps.generateRsaKey(title, keySize)
    fun saveCustomKey(key: KeyEntity) = keyOps.saveCustomKey(key)
    fun deleteKey(key: KeyEntity) = keyOps.deleteKey(key)

    // Server Telemetry Quick Probe
    fun probeHostTelemetry(hostLabel: String) = diagManager.probeHostTelemetry(hostLabel)
    fun dismissTelemetry() = diagManager.dismissTelemetry()
    fun onTerminalBell() = hapticHelper.onTerminalBell(hapticFeedbackEnabled.value)

    // Diagnostics & Log Management
    fun refreshSystemDiagnostics() = diagManager.refreshSystemDiagnostics()
    fun refreshLogs() = diagManager.refreshLogs()
    fun clearLogs() = diagManager.clearLogs()
}
