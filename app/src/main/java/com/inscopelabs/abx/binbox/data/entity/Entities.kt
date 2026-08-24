package com.inscopelabs.abx.binbox.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "hosts")
data class HostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val host: String,
    val port: Int = 22,
    val protocol: String = "SSH", // SSH, LOCAL_SHELL, DEMO_HOST, TELNET, CUSTOM_SOCKET, WEBSOCKET
    val username: String = "root",
    val authType: String = "PASSWORD", // PASSWORD, PRIVATE_KEY, PASSWORDLESS
    val password: String? = null,
    val keyId: Long? = null,
    val keyPassphrase: String? = null,
    val groupTag: String = "Cloud", // Cloud, HomeLab, Production, Local, IoT, Dev
    val themeId: String = "monokai_pro",
    val startupCommand: String? = null,
    val initialDirectory: String? = null,
    val shellProfileId: String = "default",
    val envVarsJson: String? = null,
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

/**
 * Trust-on-first-use SSH host-key store (Phase 5/9). Only a fingerprint is
 * kept, not the raw key — sufficient to detect a changed key on a later
 * connection, which is the property this table exists to provide.
 */
@Entity(tableName = "known_host_keys", indices = [Index(value = ["host", "port"], unique = true)])
data class KnownHostKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int,
    val keyType: String,
    val fingerprint: String,
    val firstSeenAt: Long = System.currentTimeMillis()
)
