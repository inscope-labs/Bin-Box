package com.inscopelabs.abx.binbox.domain.model

data class SshKey(
    val id: Long = 0,
    val title: String,
    val keyType: String = "RSA",
    val publicKey: String,
    val privateKey: String,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class Snippet(
    val id: Long = 0,
    val title: String,
    val commandTemplate: String,
    val category: String = "System",
    val description: String = "",
    val isFavorite: Boolean = false,
    val usageCount: Int = 0
)

data class CommandHistory(
    val id: Long = 0,
    val command: String,
    val hostLabel: String,
    val timestamp: Long = System.currentTimeMillis()
)
