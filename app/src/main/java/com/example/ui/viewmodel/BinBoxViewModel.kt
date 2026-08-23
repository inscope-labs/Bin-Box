package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.HistoryEntity
import com.example.data.entity.HostEntity
import com.example.data.entity.KeyEntity
import com.example.data.entity.SnippetEntity
import com.example.data.repository.BinBoxRepository
import com.example.terminal.engine.*
import com.example.terminal.model.*
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.Translations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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

class BinBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BinBoxRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = BinBoxRepository(db)
    }

    // Database Flows
    val hosts: StateFlow<List<HostEntity>> = repository.allHosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val keys: StateFlow<List<KeyEntity>> = repository.allKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snippets: StateFlow<List<SnippetEntity>> = repository.allSnippets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.recentHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Navigation State
    private val _currentAppTab = MutableStateFlow(AppTab.TERMINAL)
    val currentAppTab = _currentAppTab.asStateFlow()

    // Terminal Appearance & Settings
    private val prefs = application.getSharedPreferences("binbox_prefs", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("pref_language", AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code)
    )
    val appLanguage = _appLanguage.asStateFlow()

    val strings: StateFlow<AppStrings> = _appLanguage.map { lang ->
        Translations.getStringsFor(lang)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Translations.getStringsFor(_appLanguage.value))

    private val _currentTheme = MutableStateFlow(TerminalThemes.MonokaiPro)
    val currentTheme = _currentTheme.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(13)
    val fontSizeSp = _fontSizeSp.asStateFlow()

    private val _cursorStyle = MutableStateFlow(CursorStyle.BLOCK)
    val cursorStyle = _cursorStyle.asStateFlow()

    private val _hapticFeedbackEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled = _hapticFeedbackEnabled.asStateFlow()

    // Active Sessions & Multi-Tabs
    private val _sessions = MutableStateFlow<List<ShellSession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _activeSessionIndex = MutableStateFlow(0)
    val activeSessionIndex = _activeSessionIndex.asStateFlow()

    val activeSession: StateFlow<ShellSession?> = combine(_sessions, _activeSessionIndex) { list, idx ->
        if (list.isNotEmpty() && idx in list.indices) list[idx] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Keypad Modifier States (Ctrl, Alt)
    private val _ctrlLatched = MutableStateFlow(false)
    val ctrlLatched = _ctrlLatched.asStateFlow()

    private val _altLatched = MutableStateFlow(false)
    val altLatched = _altLatched.asStateFlow()

    // Buffer Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    // Host Telemetry Dialog
    private val _telemetry = MutableStateFlow<ServerTelemetry?>(null)
    val telemetry = _telemetry.asStateFlow()

    // Snippet Parameter Runner Dialog
    private val _selectedSnippetForRun = MutableStateFlow<SnippetEntity?>(null)
    val selectedSnippetForRun = _selectedSnippetForRun.asStateFlow()

    // Status Toast / Snackbar Message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    init {
        // Auto-launch demo session or local session on startup if none open
        viewModelScope.launch {
            // Small delay to let DB seed
            kotlinx.coroutines.delay(300)
            if (_sessions.value.isEmpty()) {
                openDemoSession()
            }
        }
    }

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
        _sessions.value.forEach { it.updateTheme(theme) }
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
    // Session Management
    // ----------------------------------------------------
    fun selectSession(index: Int) {
        if (index in _sessions.value.indices) {
            _activeSessionIndex.value = index
            _currentAppTab.value = AppTab.TERMINAL
        }
    }

    fun closeSession(index: Int) {
        val currentList = _sessions.value.toMutableList()
        if (index in currentList.indices) {
            val sessionToClose = currentList.removeAt(index)
            sessionToClose.disconnect()
            _sessions.value = currentList

            if (currentList.isEmpty()) {
                _activeSessionIndex.value = 0
            } else if (_activeSessionIndex.value >= currentList.size) {
                _activeSessionIndex.value = currentList.size - 1
            }
        }
    }

    fun openDemoSession() {
        val session = SandboxDemoShellSession(
            title = "Cloud Demo (SSH)",
            hostLabel = "vps-demo.binbox.io",
            initialTheme = _currentTheme.value,
            onBell = { onTerminalBell() }
        )
        addAndStartSession(session)
    }

    fun openLocalSession() {
        val session = LocalShellSession(
            title = "Local Device",
            hostLabel = "localhost",
            initialTheme = _currentTheme.value,
            onBell = { onTerminalBell() }
        )
        addAndStartSession(session)
    }

    fun connectToHost(hostEntity: HostEntity) {
        viewModelScope.launch {
            var privateKeyContent: String? = null
            if (hostEntity.authType == "PRIVATE_KEY" && hostEntity.keyId != null) {
                val key = repository.allKeys.firstOrNull()?.find { it.id == hostEntity.keyId }
                privateKeyContent = key?.privateKey
            }

            val session: ShellSession = when (hostEntity.protocol) {
                "LOCAL_SHELL" -> LocalShellSession(
                    title = hostEntity.label,
                    hostLabel = hostEntity.host,
                    initialTheme = _currentTheme.value,
                    onBell = { onTerminalBell() }
                )
                "DEMO_HOST" -> SandboxDemoShellSession(
                    title = hostEntity.label,
                    hostLabel = hostEntity.host,
                    initialTheme = _currentTheme.value,
                    onBell = { onTerminalBell() }
                )
                "TELNET" -> TelnetShellSession(
                    title = hostEntity.label,
                    hostLabel = hostEntity.host,
                    host = hostEntity.host,
                    port = if (hostEntity.port > 0) hostEntity.port else 23,
                    initialTheme = _currentTheme.value,
                    onBell = { onTerminalBell() }
                )
                else -> SshShellSession(
                    title = hostEntity.label,
                    hostLabel = "${hostEntity.username}@${hostEntity.host}",
                    host = hostEntity.host,
                    port = if (hostEntity.port > 0) hostEntity.port else 22,
                    username = hostEntity.username,
                    password = hostEntity.password,
                    privateKey = privateKeyContent,
                    privateKeyPassphrase = hostEntity.keyPassphrase,
                    initialTheme = _currentTheme.value,
                    onBell = { onTerminalBell() }
                )
            }

            addAndStartSession(session)
            _currentAppTab.value = AppTab.TERMINAL

            // If host has startup command, send it after connection
            if (!hostEntity.startupCommand.isNullOrBlank()) {
                launch {
                    session.state.filter { it is SessionState.Connected }.first()
                    kotlinx.coroutines.delay(500)
                    session.sendInput("${hostEntity.startupCommand}\n")
                }
            }
        }
    }

    private fun addAndStartSession(session: ShellSession) {
        val currentList = _sessions.value.toMutableList()
        currentList.add(session)
        _sessions.value = currentList
        _activeSessionIndex.value = currentList.size - 1
        session.start()
    }

    // ----------------------------------------------------
    // Terminal Input & Keypad Dispatch
    // ----------------------------------------------------
    fun sendCommand(command: String) {
        val session = activeSession.value ?: return
        if (command.isNotBlank()) {
            viewModelScope.launch {
                repository.recordHistory(command, session.hostLabel)
            }
        }

        session.sendInput(command + "\n")
        triggerHaptic()
    }

    fun sendRawInput(text: String) {
        val session = activeSession.value ?: return

        if (_ctrlLatched.value) {
            // Apply Ctrl modifier to single characters
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
        val session = activeSession.value ?: return
        session.sendSpecialKey(key)
        triggerHaptic()
    }

    fun sendTextSnippet(text: String) {
        val session = activeSession.value ?: return
        session.sendInput(text)
        triggerHaptic()
    }

    fun clearCurrentTerminal() {
        activeSession.value?.clear()
        triggerHaptic()
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
            repository.incrementSnippetUsage(snippet.id)
            repository.recordHistory(resolvedCommand, activeSession.value?.hostLabel ?: "Terminal")
        }

        _currentAppTab.value = AppTab.TERMINAL
        activeSession.value?.sendInput(resolvedCommand + "\n")
        showSnackbar("Executed: ${snippet.title}")
        triggerHaptic()
    }

    fun saveSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.saveSnippet(snippet)
            showSnackbar("Saved snippet: ${snippet.title}")
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        viewModelScope.launch {
            repository.deleteSnippet(snippet)
            showSnackbar("Deleted snippet: ${snippet.title}")
        }
    }

    // ----------------------------------------------------
    // Hosts Operations
    // ----------------------------------------------------
    fun saveHost(host: HostEntity) {
        viewModelScope.launch {
            repository.saveHost(host)
            showSnackbar("Saved host: ${host.label}")
        }
    }

    fun deleteHost(host: HostEntity) {
        viewModelScope.launch {
            repository.deleteHost(host)
            showSnackbar("Deleted host: ${host.label}")
        }
    }

    fun toggleHostFavorite(host: HostEntity) {
        viewModelScope.launch {
            repository.toggleHostFavorite(host)
        }
    }

    fun pingHost(host: HostEntity) {
        viewModelScope.launch {
            val latency = repository.pingHost(host)
            if (latency != null) {
                showSnackbar("${host.label}: ${latency}ms latency")
            } else {
                showSnackbar("${host.label}: Host unreachable")
            }
        }
    }

    fun pingAllHosts() {
        viewModelScope.launch {
            val list = hosts.value
            list.forEach { repository.pingHost(it) }
            showSnackbar("Pinged ${list.size} hosts")
        }
    }

    // ----------------------------------------------------
    // SSH Key Management
    // ----------------------------------------------------
    fun generateRsaKey(title: String, keySize: Int = 2048) {
        viewModelScope.launch {
            try {
                val newKey = repository.generateRsaKeyPair(title, keySize)
                showSnackbar("Generated SSH Key: ${newKey.title}")
            } catch (e: Exception) {
                showSnackbar("Key generation failed: ${e.message}")
            }
        }
    }

    fun saveCustomKey(key: KeyEntity) {
        viewModelScope.launch {
            repository.saveKey(key)
            showSnackbar("Saved key: ${key.title}")
        }
    }

    fun deleteKey(key: KeyEntity) {
        viewModelScope.launch {
            repository.deleteKey(key)
            showSnackbar("Deleted key: ${key.title}")
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
    // Haptics and Bell
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
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(15)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration unsupported
        }
    }

    private fun onTerminalBell() {
        if (!_hapticFeedbackEnabled.value) return
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(80, 200))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
}
