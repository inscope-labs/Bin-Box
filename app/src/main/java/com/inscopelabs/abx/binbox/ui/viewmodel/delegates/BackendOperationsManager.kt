package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.inscopelabs.abx.binbox.transport.backend.api.BinBoxBackendClient
import com.inscopelabs.abx.binbox.transport.backend.models.BackendDiscoveryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackendOperationsManager(
    private val coroutineScope: CoroutineScope
) {
    private val backendClient = BinBoxBackendClient()

    private val _backendDiscovery = MutableStateFlow<BackendDiscoveryResponse?>(null)
    val backendDiscovery: StateFlow<BackendDiscoveryResponse?> = _backendDiscovery.asStateFlow()

    private val _backendInstances = MutableStateFlow<List<VmStatus>>(emptyList())
    val backendInstances: StateFlow<List<VmStatus>> = _backendInstances.asStateFlow()

    private val _isBackendRefreshing = MutableStateFlow(false)
    val isBackendRefreshing: StateFlow<Boolean> = _isBackendRefreshing.asStateFlow()

    fun refreshBackendInstances() {
        coroutineScope.launch {
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
                BinBoxLogger.e("BackendOperationsManager", "Failed to refresh backend instances", e)
            } finally {
                _isBackendRefreshing.value = false
            }
        }
    }
}
