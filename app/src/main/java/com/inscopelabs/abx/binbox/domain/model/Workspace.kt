package com.inscopelabs.abx.binbox.domain.model

import java.util.UUID

/**
 * Encapsulates a multi-terminal Workspace grouping specific host profiles,
 * active sessions, and environment settings.
 */
data class Workspace(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconName: String = "Terminal", // "Terminal", "Cloud", "Code", "Server", "Database", "Cpu"
    val colorHex: String = "#38BDF8", // Theme accent color
    val hostProfileIds: List<Long> = emptyList(),
    val defaultShellProfileId: String = "default",
    val isDefault: Boolean = false,
    val autoConnectHosts: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val PRESETS = listOf(
            Workspace(
                id = "ws_development",
                name = "Development",
                description = "Local sandbox, Termux, and development VPS environments",
                iconName = "Code",
                colorHex = "#38BDF8", // Ice Blue
                hostProfileIds = listOf(1L, 2L),
                isDefault = true
            ),
            Workspace(
                id = "ws_abx_cloud",
                name = "ABX Cloud",
                description = "ABX Gateway relays, Oracle Cloud Free VMs, and build clusters",
                iconName = "Cloud",
                colorHex = "#818CF8", // Indigo Accent
                hostProfileIds = listOf(1L, 4L)
            ),
            Workspace(
                id = "ws_operations",
                name = "Operations & Infra",
                description = "Production servers, databases, monitoring, and telemetry relays",
                iconName = "Server",
                colorHex = "#34D399", // Emerald Green
                hostProfileIds = listOf(3L, 4L)
            ),
            Workspace(
                id = "ws_homelab",
                name = "HomeLab & IoT",
                description = "Raspberry Pi cluster, Home Assistant, and local edge devices",
                iconName = "Cpu",
                colorHex = "#F59E0B", // Amber
                hostProfileIds = listOf(3L)
            )
        )
    }
}
