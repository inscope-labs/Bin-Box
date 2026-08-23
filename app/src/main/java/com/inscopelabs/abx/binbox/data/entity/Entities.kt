package com.inscopelabs.abx.binbox.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hosts")
data class HostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val host: String,
    val port: Int = 22,
    val protocol: String = "SSH", // SSH, LOCAL_SHELL, DEMO_HOST, TELNET, CUSTOM_SOCKET
    val username: String = "root",
    val authType: String = "PASSWORD", // PASSWORD, PRIVATE_KEY, PASSWORDLESS
    val password: String? = null,
    val keyId: Long? = null,
    val keyPassphrase: String? = null,
    val groupTag: String = "Cloud", // Cloud, HomeLab, Production, Local, IoT, Dev
    val themeId: String = "monokai_pro",
    val startupCommand: String? = null,
    val keepAliveSeconds: Int = 30,
    val isFavorite: Boolean = false,
    val lastLatencyMs: Long? = null,
    val lastConnectedAt: Long? = null
)

@Entity(tableName = "ssh_keys")
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val keyType: String = "RSA", // RSA, ED25519
    val publicKey: String,
    val privateKey: String,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val commandTemplate: String,
    val category: String = "System", // System, Docker, Git, Network, Database, Custom
    val description: String = "",
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)

@Entity(tableName = "command_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val command: String,
    val hostLabel: String,
    val timestamp: Long = System.currentTimeMillis()
)
