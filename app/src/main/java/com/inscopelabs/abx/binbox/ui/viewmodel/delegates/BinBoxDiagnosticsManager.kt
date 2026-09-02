package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import android.app.Application
import com.inscopelabs.abx.binbox.core.diagnostics.DeviceDiagnostics
import com.inscopelabs.abx.binbox.core.diagnostics.SessionMetrics
import com.inscopelabs.abx.binbox.core.diagnostics.SessionTelemetryTracker
import com.inscopelabs.abx.binbox.core.diagnostics.SystemDiagnosticsCollector
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.logging.LogEntry
import com.inscopelabs.abx.binbox.ui.viewmodel.ServerTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BinBoxDiagnosticsManager(
    application: Application,
    private val coroutineScope: CoroutineScope
) {
    private val diagnosticsCollector = SystemDiagnosticsCollector(application)
    val sessionTelemetryTracker = SessionTelemetryTracker()

    private val _systemDiagnostics = MutableStateFlow<DeviceDiagnostics?>(null)
    val systemDiagnostics: StateFlow<DeviceDiagnostics?> = _systemDiagnostics.asStateFlow()

    val sessionMetrics: StateFlow<Map<String, SessionMetrics>> = sessionTelemetryTracker.telemetryFlow

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    private val _telemetry = MutableStateFlow<ServerTelemetry?>(null)
    val telemetry: StateFlow<ServerTelemetry?> = _telemetry.asStateFlow()

    init {
        refreshSystemDiagnostics()
        refreshLogs()
    }

    fun probeHostTelemetry(hostLabel: String) {
        coroutineScope.launch {
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

    fun refreshSystemDiagnostics() {
        try {
            _systemDiagnostics.value = diagnosticsCollector.collectSnapshot()
        } catch (e: Throwable) {
            BinBoxLogger.w("BinBoxDiagnosticsManager", "Failed refreshing diagnostics", e)
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
