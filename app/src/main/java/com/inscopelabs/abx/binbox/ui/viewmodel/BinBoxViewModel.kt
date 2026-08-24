package com.inscopelabs.abx.binbox.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.abx.binbox.core.diagnostics.DeviceDiagnostics
import com.inscopelabs.abx.binbox.core.diagnostics.SessionMetrics
import com.inscopelabs.abx.binbox.core.diagnostics.SessionTelemetryTracker
import com.inscopelabs.abx.binbox.core.diagnostics.SystemDiagnosticsCollector
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.logging.LogEntry
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.database.AppDatabase
import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.data.repository.HistoryRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.HostRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.KeyRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.SessionRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.SnippetRepositoryImpl
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.inscopelabs.abx.binbox.security.SecureStorageService
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.usecase.HistoryUseCases
import com.inscopelabs.abx.binbox.domain.usecase.HostUseCases
import com.inscopelabs.abx.binbox.domain.usecase.KeyUseCases
import com.inscopelabs.abx.binbox.domain.usecase.ManageSessionUseCase
import com.inscopelabs.abx.binbox.domain.usecase.SnippetUseCases
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionFactory
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.transport.backend.api.BinBoxBackendClient
import com.inscopelabs.abx.binbox.transport.backend.models.BackendDiscoveryResponse
import com.inscopelabs.abx.binbox.transport.backend.models.ProvisionSessionRequest
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.ui.i18n.AppLanguage
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.i18n.Translations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    // Secondary Constructor for Standard Compose ViewModel instantiation
    constructor(application: Application) : this(
        application = application,
        graph = createDependencyGraph(application)
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

    companion object {
        private fun createDependencyGraph(application: Application): DependencyGraph {
            val db = AppDatabase.getInstance(application)
            val secureStorage = SecureStorageService(application)
            val hostRepo = HostRepositoryImpl(db.hostDao(), secureStorage)
            val keyRepo = KeyRepositoryImpl(db.keyDao(), secureStorage)
            val snippetRepo = SnippetRepositoryImpl(db.snippetDao())
            val historyRepo = HistoryRepositoryImpl(db.historyDao())
            val sessionRepo = SessionRepositoryImpl()
            val hostKeyStore = com.inscopelabs.abx.binbox.security.HostKeyStore(db.knownHostKeyDao())

            val hostUseCases = HostUseCases.create(hostRepo)
            val keyUseCases = KeyUseCases.create(keyRepo)
            val snippetUseCases = SnippetUseCases.create(snippetRepo, historyRepo)
            val historyUseCases = HistoryUseCases.create(historyRepo)
            val manageSessionUseCase = ManageSessionUseCase(sessionRepo)

            val sessionFactory = TerminalSessionFactory(keyRepository = keyRepo, hostKeyStore = hostKeyStore)
            val sessionManager = TerminalSessionManager(
                sessionFactory = sessionFactory,
                sessionRepository = sessionRepo
            )

            return DependencyGraph(
                hostUseCases = hostUseCases,
                keyUseCases = keyUseCases,
                snippetUseCases = snippetUseCases,
                historyUseCases = historyUseCases,
                manageSessionUseCase = manageSessionUseCase,
                sessionManager = sessionManager
            )
        }
    }

    private data class DependencyGraph(
        val hostUseCases: HostUseCases,
        val keyUseCases: KeyUseCases,
        val snippetUseCases: SnippetUseCases,
        val historyUseCases: HistoryUseCases,
        val manageSessionUseCase: ManageSessionUseCase,
        val sessionManager: TerminalSessionManager
    )

    // ----------------------------------------------------
    // Reactive Domain & Entity Data Streams
    // ----------------------------------------------------
    val domainHosts: StateFlow<List<ConnectionProfile>> = hostUseCases.getHosts()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hosts: StateFlow<List<HostEntity>> = domainHosts
        .map { list -> list.map { it.toEntity() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val domainKeys: StateFlow<List<SshKey>> = keyUseCases.getKeys()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val keys: StateFlow<List<KeyEntity>> = domainKeys
        .map { list -> list.map { it.toEntity() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val domainSnippets: StateFlow<List<Snippet>> = snippetUseCases.getSnippets()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val snippets: StateFlow<List<SnippetEntity>> = domainSnippets
        .map { list -> list.map { it.toEntity() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val domainHistory: StateFlow<List<CommandHistory>> = historyUseCases.getHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<HistoryEntity>> = domainHistory
        .map { list -> list.map { it.toEntity() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ----------------------------------------------------
    // UI Navigation State
    // ----------------------------------------------------
    private val _currentAppTab = MutableStateFlow(AppTab.TERMINAL)
    val currentAppTab: StateFlow<AppTab> = _currentAppTab.asStateFlow()

    // ----------------------------------------------------
    // Terminal Appearance & Settings
    // ----------------------------------------------------
    private val prefs = application.getSharedPreferences("binbox_prefs", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("pref_language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code)
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    val strings: StateFlow<AppStrings> = _appLanguage.map { lang ->
        Translations.getStringsFor(lang)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Translations.getStringsFor(_appLanguage.value))

    private val _currentTheme = MutableStateFlow(TerminalThemes.MonokaiPro)
    val currentTheme: StateFlow<TerminalThemePreset> = _currentTheme.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(13)
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private val _cursorStyle = MutableStateFlow(CursorStyle.BLOCK)
    val cursorStyle: StateFlow<CursorStyle> = _cursorStyle.asStateFlow()

    private val _hapticFeedbackEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled.asStateFlow()

    // ----------------------------------------------------
    // Active Sessions & Multi-Tabs
    // ----------------------------------------------------
    val sessions: StateFlow<List<ShellSession>> = sessionManager.sessions
    val activeSessionIndex: StateFlow<Int> = sessionManager.activeSessionIndex

    val activeSession: StateFlow<ShellSession?> = combine(sessions, activeSessionIndex) { list, idx ->
        if (list.isNotEmpty() && idx in list.indices) list[idx] else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Keypad Modifier States (Ctrl, Alt)
    private val _ctrlLatched = MutableStateFlow(false)
    val ctrlLatched: StateFlow<Boolean> = _ctrlLatched.asStateFlow()

    private val _altLatched = MutableStateFlow(false)
    val altLatched: StateFlow<Boolean> = _altLatched.asStateFlow()

    // Buffer Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Host Telemetry Dialog
    private val _telemetry = MutableStateFlow<ServerTelemetry?>(null)
    val telemetry: StateFlow<ServerTelemetry?> = _telemetry.asStateFlow()

    // Snippet Parameter Runner Dialog
    private val _selectedSnippetForRun = MutableStateFlow<SnippetEntity?>(null)
    val selectedSnippetForRun: StateFlow<SnippetEntity?> = _selectedSnippetForRun.asStateFlow()

    // Status Toast / Snackbar Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // ----------------------------------------------------
    // Backend Gateway & WebSocket Provider State (Phase 7)
    // ----------------------------------------------------
    private val backendClient = BinBoxBackendClient()
    private val _backendDiscovery = MutableStateFlow<BackendDiscoveryResponse?>(null)
    val backendDiscovery: StateFlow<BackendDiscoveryResponse?> = _backendDiscovery.asStateFlow()

    private val _backendInstances = MutableStateFlow<List<VmStatus>>(emptyList())
    val backendInstances: StateFlow<List<VmStatus>> = _backendInstances.asStateFlow()

    private val _isBackendRefreshing = MutableStateFlow(false)
    val isBackendRefreshing: StateFlow<Boolean> = _isBackendRefreshing.asStateFlow()

    // ----------------------------------------------------
    // System Diagnostics, Telemetry & Logging Engine
    // ----------------------------------------------------
    private val diagnosticsCollector = SystemDiagnosticsCollector(application)
    val sessionTelemetryTracker = SessionTelemetryTracker()

    private val _systemDiagnostics = MutableStateFlow<DeviceDiagnostics?>(null)
    val systemDiagnostics: StateFlow<DeviceDiagnostics?> = _systemDiagnostics.asStateFlow()

    val sessionMetrics: StateFlow<Map<String, SessionMetrics>> = sessionTelemetryTracker.telemetryFlow

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    init {
        refreshSystemDiagnostics()
        refreshLogs()
        refreshBackendInstances()
        // Auto-launch demo session on initial launch if none open
        viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            if (sessions.value.isEmpty()) {
                openDemoSession()
            }
        }
    }

    // ----------------------------------------------------
    // Navigation & Preferences Handlers
    // ----------------------------------------------------
    fun setAppTab(tab: AppTab) {
        _currentAppTab.value = tab
    }

    fun setLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        prefs.edit().putString("pref_language", lang.code).apply()
        showSnackbar("Language updated: ${lang.displayName}")
    }

    fun resetPreferences() {
        setLanguage(AppLanguage.SYSTEM)
        setTheme(TerminalThemes.MonokaiPro)
        setFontSize(13)
        setCursorStyle(CursorStyle.BLOCK)
        toggleHapticFeedback(true)
        showSnackbar("Preferences reset to defaults")
    }

    fun setTheme(theme: TerminalThemePreset) {
        _currentTheme.value = theme
        sessionManager.updateTheme(theme)
    }

    fun setFontSize(sizeSp: Int) {
        _fontSizeSp.value = sizeSp.coerceIn(9, 24)
    }

    fun setCursorStyle(style: CursorStyle) {
        _cursorStyle.value = style
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        _hapticFeedbackEnabled.value = enabled
    }

    fun toggleCtrl() {
        _ctrlLatched.value = !_ctrlLatched.value
        triggerHaptic()
    }

    fun toggleAlt() {
        _altLatched.value = !_altLatched.value
        triggerHaptic()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearching(searching: Boolean) {
        _isSearching.value = searching
        if (!searching) {
            _searchQuery.value = ""
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    // ----------------------------------------------------
    // Session Management Actions
    // ----------------------------------------------------
    fun selectSession(index: Int) {
        sessionManager.selectSession(index)
        _currentAppTab.value = AppTab.TERMINAL
    }

    fun closeSession(index: Int) {
        sessionManager.closeSession(index)
    }

    fun openDemoSession(): kotlinx.coroutines.Job = viewModelScope.launch {
        val demoProfile = ConnectionProfile(
            label = "Cloud Demo (SSH)",
            host = "vps-demo.binbox.io",
            protocol = ProtocolType.DEMO_HOST,
            themeId = _currentTheme.value.id
        )
        sessionManager.launchSession(demoProfile, theme = _currentTheme.value)
    }

    fun openLocalSession(): kotlinx.coroutines.Job = viewModelScope.launch {
        val localProfile = ConnectionProfile(
            label = "Local Device",
            host = "localhost",
            protocol = ProtocolType.LOCAL_SHELL,
            themeId = _currentTheme.value.id
        )
        sessionManager.launchSession(localProfile, theme = _currentTheme.value)
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
            themeId = _currentTheme.value.id
        )
        val result = sessionManager.launchSession(wsProfile, theme = _currentTheme.value)
        if (result is AppResult.Success) {
            _currentAppTab.value = AppTab.TERMINAL
        } else if (result is AppResult.Error) {
            showSnackbar("WebSocket connection failed: ${result.error.userMessage}")
        }
    }

    fun refreshBackendInstances() {
        viewModelScope.launch {
            _isBackendRefreshing.value = true
            try {
                val discoveryResult = backendClient.getDiscovery()
                if (discoveryResult.isSuccess) {
                    _backendDiscovery.value = discoveryResult.getOrNull()
                }

                val instancesResult = backendClient.listInstances()
                if (instancesResult.isSuccess) {
                    _backendInstances.value = instancesResult.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                BinBoxLogger.e("BinBoxViewModel", "Failed to refresh backend instances", e)
            } finally {
                _isBackendRefreshing.value = false
            }
        }
    }

    fun connectToVmInstance(vm: VmStatus) {
        val targetIp = vm.publicIp ?: vm.privateIp ?: return
        val profile = ConnectionProfile(
            label = vm.displayName,
            host = targetIp,
            port = 22,
            protocol = ProtocolType.SSH,
            username = "ubuntu",
            themeId = _currentTheme.value.id
        )
        viewModelScope.launch {
            val result = sessionManager.launchSession(profile, theme = _currentTheme.value)
            if (result is AppResult.Success) {
                _currentAppTab.value = AppTab.TERMINAL
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to connect to VM: ${result.error.userMessage}")
            }
        }
    }

    fun connectToHost(hostEntity: HostEntity): kotlinx.coroutines.Job = viewModelScope.launch {
        val profile = hostEntity.toDomain()
        val result = sessionManager.launchSession(profile, theme = _currentTheme.value)

        if (result is AppResult.Success) {
            val session = result.data
            _currentAppTab.value = AppTab.TERMINAL

            // If host has startup command, send it once connected
            if (!profile.startupCommand.isNullOrBlank()) {
                launch {
                    session.state.filter { it is SessionState.Connected }.first()
                    kotlinx.coroutines.delay(500)
                    session.sendInput("${profile.startupCommand}\n")
                }
            }
        } else if (result is AppResult.Error) {
            showSnackbar("Connection failed: ${result.error.userMessage}")
        }
    }

    // ----------------------------------------------------
    // Terminal Input & Keypad Dispatch
    // ----------------------------------------------------
    fun sendCommand(command: String) {
        val session = activeSession.value ?: return
        if (command.isNotBlank()) {
            viewModelScope.launch {
                historyUseCases.recordHistory(command, session.hostLabel)
            }
        }

        session.sendInput(command + "\n")
        triggerHaptic()
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
                    triggerHaptic()
                    return
                }
            }
        }

        session.sendInput(text)
        triggerHaptic()
    }

    fun sendSpecialKey(key: TerminalKey) {
        sessionManager.sendSpecialKeyToActive(key)
        triggerHaptic()
    }

    fun sendTextSnippet(text: String) {
        sessionManager.sendInputToActive(text)
        triggerHaptic()
    }

    fun clearCurrentTerminal() {
        sessionManager.clearActiveTerminal()
        triggerHaptic()
    }

    fun resizeTerminal(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {
        sessionManager.resizeActiveTerminal(cols, rows, widthPx, heightPx)
    }

    // ----------------------------------------------------
    // Snippets & Quick Scripts
    // ----------------------------------------------------
    fun openSnippetDialog(snippet: SnippetEntity) {
        _selectedSnippetForRun.value = snippet
    }

    fun dismissSnippetDialog() {
        _selectedSnippetForRun.value = null
    }

    fun executeSnippet(snippet: SnippetEntity, resolvedCommand: String) {
        _selectedSnippetForRun.value = null
        viewModelScope.launch {
            val domainSnippet = snippet.toDomain()
            snippetUseCases.executeSnippet(
                snippet = domainSnippet,
                targetHostLabel = activeSession.value?.hostLabel ?: "Terminal"
            )
        }

        _currentAppTab.value = AppTab.TERMINAL
        sessionManager.sendInputToActive(resolvedCommand + "\n")
        showSnackbar("Executed: ${snippet.title}")
        triggerHaptic()
    }

    fun saveSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            val result = snippetUseCases.saveSnippet(snippet.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Saved snippet: ${snippet.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to save snippet: ${result.error.userMessage}")
            }
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            val result = snippetUseCases.deleteSnippet(snippet.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Deleted snippet: ${snippet.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to delete snippet: ${result.error.userMessage}")
            }
        }
    }

    // ----------------------------------------------------
    // Hosts Operations
    // ----------------------------------------------------
    fun saveHost(host: HostEntity) {
        viewModelScope.launch {
            val result = hostUseCases.saveHost(host.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Saved host: ${host.label}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to save host: ${result.error.userMessage}")
            }
        }
    }

    fun deleteHost(host: HostEntity) {
        viewModelScope.launch {
            val result = hostUseCases.deleteHost(host.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Deleted host: ${host.label}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to delete host: ${result.error.userMessage}")
            }
        }
    }

    fun toggleHostFavorite(host: HostEntity) {
        viewModelScope.launch {
            hostUseCases.toggleFavorite(host.id, !host.isFavorite)
        }
    }

    fun pingHost(host: HostEntity) {
        viewModelScope.launch {
            when (val result = hostUseCases.pingHost(host.toDomain())) {
                is AppResult.Success -> {
                    showSnackbar("${host.label}: ${result.data}ms latency")
                }
                is AppResult.Error -> {
                    showSnackbar("${host.label}: ${result.error.userMessage}")
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun pingAllHosts() {
        viewModelScope.launch {
            val list = domainHosts.value
            list.forEach { hostUseCases.pingHost(it) }
            showSnackbar("Pinged ${list.size} hosts")
        }
    }

    // ----------------------------------------------------
    // SSH Key Management
    // ----------------------------------------------------
    fun generateRsaKey(title: String, keySize: Int = 2048) {
        viewModelScope.launch {
            when (val result = keyUseCases.generateKeyPair(title, keySize)) {
                is AppResult.Success -> {
                    showSnackbar("Generated SSH Key: ${result.data.title}")
                }
                is AppResult.Error -> {
                    showSnackbar("Key generation failed: ${result.error.userMessage}")
                }
                is AppResult.Loading -> {}
            }
        }
    }

    fun saveCustomKey(key: KeyEntity) {
        viewModelScope.launch {
            val result = keyUseCases.saveKey(key.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Saved key: ${key.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to save key: ${result.error.userMessage}")
            }
        }
    }

    fun deleteKey(key: KeyEntity) {
        viewModelScope.launch {
            val result = keyUseCases.deleteKey(key.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Deleted key: ${key.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to delete key: ${result.error.userMessage}")
            }
        }
    }

    // ----------------------------------------------------
    // Server Telemetry Quick Probe
    // ----------------------------------------------------
    fun probeHostTelemetry(hostLabel: String) {
        viewModelScope.launch {
            _telemetry.value = ServerTelemetry(
                hostLabel = hostLabel,
                osInfo = "Ubuntu 24.04 LTS (Kernel 6.8.0-45-generic x86_64)",
                uptime = "48 days, 14 hours (Load: 0.38, 0.45, 0.52)",
                cpuUsage = "38.4% (4 vCPUs AMD EPYC)",
                memUsage = "4,210 MiB / 16,384 MiB (25% utilized)",
                diskUsage = "42 GiB / 120 GiB (37% mounted on /)"
            )
        }
    }

    fun dismissTelemetry() {
        _telemetry.value = null
    }

    // ----------------------------------------------------
    // Haptics and Bell Feedback
    // ----------------------------------------------------
    private fun triggerHaptic() {
        if (!_hapticFeedbackEnabled.value) return
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (_: Throwable) {
            // Ignore if vibration unsupported or in test environment
        }
    }

    fun onTerminalBell() {
        if (!_hapticFeedbackEnabled.value) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }

    // ----------------------------------------------------
    // Diagnostics & Log Management
    // ----------------------------------------------------
    fun refreshSystemDiagnostics() {
        try {
            _systemDiagnostics.value = diagnosticsCollector.collectSnapshot()
        } catch (e: Throwable) {
            BinBoxLogger.w("BinBoxViewModel", "Failed refreshing diagnostics", e)
        }
    }

    fun refreshLogs() {
        _logEntries.value = BinBoxLogger.getLogs()
    }

    fun clearLogs() {
        BinBoxLogger.clear()
        _logEntries.value = emptyList()
    }
}
