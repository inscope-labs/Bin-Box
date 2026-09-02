package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.inscopelabs.abx.binbox.domain.usecase.HostUseCases
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HostOperationsManager(
    private val hostUseCases: HostUseCases,
    private val coroutineScope: CoroutineScope,
    private val showSnackbar: (String) -> Unit
) {
    val domainHosts: StateFlow<List<ConnectionProfile>> = hostUseCases.getHosts()
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val hosts: StateFlow<List<HostEntity>> = domainHosts
        .map { list -> list.map { it.toEntity() } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    private val _hostFilterTag = MutableStateFlow("All")
    val hostFilterTag: StateFlow<String> = _hostFilterTag.asStateFlow()

    private val _hostSearchQuery = MutableStateFlow("")
    val hostSearchQuery: StateFlow<String> = _hostSearchQuery.asStateFlow()

    val recentHosts: StateFlow<List<ConnectionProfile>> = domainHosts.map { list ->
        list.filter { it.lastConnectedAt != null }
            .sortedByDescending { it.lastConnectedAt ?: 0L }
            .take(6)
    }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    fun setHostFilterTag(tag: String) {
        _hostFilterTag.value = tag
    }

    fun setHostSearchQuery(query: String) {
        _hostSearchQuery.value = query
    }

    fun saveHost(host: HostEntity) {
        coroutineScope.launch {
            val result = hostUseCases.saveHost(host.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Saved host: ${host.label}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to save host: ${result.error.userMessage}")
            }
        }
    }

    fun deleteHost(host: HostEntity, onHostDeleted: (Long) -> Unit) {
        coroutineScope.launch {
            BinBoxLogger.i("HostOperationsManager", "Deleting host profile: ${host.label} (ID: ${host.id})")
            val result = hostUseCases.deleteHost(host.toDomain())
            if (result is AppResult.Success) {
                onHostDeleted(host.id)
                showSnackbar("Deleted host: ${host.label}")
                BinBoxLogger.i("HostOperationsManager", "Host successfully deleted: ${host.label}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to delete host: ${result.error.userMessage}")
                BinBoxLogger.e("HostOperationsManager", "Failed to delete host: ${host.label} - ${result.error.userMessage}")
            }
        }
    }

    fun toggleHostFavorite(host: HostEntity) {
        coroutineScope.launch {
            hostUseCases.toggleFavorite(host.id, !host.isFavorite)
        }
    }

    fun pingHost(host: HostEntity) {
        coroutineScope.launch {
            when (val result = hostUseCases.pingHost(host.toDomain())) {
                is AppResult.Success -> showSnackbar("${host.label}: ${result.data}ms latency")
                is AppResult.Error -> showSnackbar("${host.label}: ${result.error.userMessage}")
                is AppResult.Loading -> {}
            }
        }
    }

    fun pingAllHosts() {
        coroutineScope.launch {
            val list = domainHosts.value
            list.forEach { hostUseCases.pingHost(it) }
            showSnackbar("Pinged ${list.size} hosts")
        }
    }

    fun connectToHost(
        hostEntity: HostEntity,
        sessionManager: TerminalSessionManager,
        theme: TerminalThemePreset,
        onConnected: () -> Unit
    ): Job = coroutineScope.launch {
        val profile = hostEntity.toDomain()
        val result = sessionManager.launchSession(profile, theme = theme)

        if (result is AppResult.Success) {
            val session = result.data
            onConnected()

            if (!profile.startupCommand.isNullOrBlank()) {
                coroutineScope.launch {
                    session.state.filter { it is SessionState.Connected }.first()
                    delay(500)
                    session.sendInput("${profile.startupCommand}\n")
                }
            }
        } else if (result is AppResult.Error) {
            showSnackbar("Connection failed: ${result.error.userMessage}")
        }
    }

    fun connectToVmInstance(
        vm: VmStatus,
        sessionManager: TerminalSessionManager,
        theme: TerminalThemePreset,
        onConnected: () -> Unit
    ) {
        val targetIp = vm.publicIp ?: vm.privateIp ?: return
        val profile = ConnectionProfile(
            label = vm.displayName,
            host = targetIp,
            port = 22,
            protocol = ProtocolType.SSH,
            username = "ubuntu",
            themeId = theme.id
        )
        coroutineScope.launch {
            val result = sessionManager.launchSession(profile, theme = theme)
            if (result is AppResult.Success) {
                onConnected()
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to connect to VM: ${result.error.userMessage}")
            }
        }
    }
}
