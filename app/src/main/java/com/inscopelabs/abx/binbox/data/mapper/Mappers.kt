package com.inscopelabs.abx.binbox.data.mapper

import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.model.SshKey

fun HostEntity.toDomain(): ConnectionProfile = ConnectionProfile(
    id = id,
    label = label,
    host = host,
    port = port,
    protocol = runCatching { ProtocolType.valueOf(protocol) }.getOrDefault(ProtocolType.SSH),
    username = username,
    authType = runCatching { AuthType.valueOf(authType) }.getOrDefault(AuthType.PASSWORD),
    password = password,
    keyId = keyId,
    keyPassphrase = keyPassphrase,
    groupTag = groupTag,
    themeId = themeId,
    startupCommand = startupCommand,
    keepAliveSeconds = keepAliveSeconds,
    isFavorite = isFavorite,
    lastLatencyMs = lastLatencyMs,
    lastConnectedAt = lastConnectedAt
)

fun ConnectionProfile.toEntity(): HostEntity = HostEntity(
    id = id,
    label = label,
    host = host,
    port = port,
    protocol = protocol.name,
    username = username,
    authType = authType.name,
    password = password,
    keyId = keyId,
    keyPassphrase = keyPassphrase,
    groupTag = groupTag,
    themeId = themeId,
    startupCommand = startupCommand,
    keepAliveSeconds = keepAliveSeconds,
    isFavorite = isFavorite,
    lastLatencyMs = lastLatencyMs,
    lastConnectedAt = lastConnectedAt
)

fun KeyEntity.toDomain(): SshKey = SshKey(
    id = id,
    title = title,
    keyType = keyType,
    publicKey = publicKey,
    privateKey = privateKey,
    fingerprint = fingerprint,
    createdAt = createdAt
)

fun SshKey.toEntity(): KeyEntity = KeyEntity(
    id = id,
    title = title,
    keyType = keyType,
    publicKey = publicKey,
    privateKey = privateKey,
    fingerprint = fingerprint,
    createdAt = createdAt
)

fun SnippetEntity.toDomain(): Snippet = Snippet(
    id = id,
    title = title,
    commandTemplate = commandTemplate,
    category = category,
    description = description,
    isFavorite = isFavorite,
    usageCount = usageCount
)

fun Snippet.toEntity(): SnippetEntity = SnippetEntity(
    id = id,
    title = title,
    commandTemplate = commandTemplate,
    category = category,
    description = description,
    isFavorite = isFavorite,
    usageCount = usageCount
)

fun HistoryEntity.toDomain(): CommandHistory = CommandHistory(
    id = id,
    command = command,
    hostLabel = hostLabel,
    timestamp = timestamp
)

fun CommandHistory.toEntity(): HistoryEntity = HistoryEntity(
    id = id,
    command = command,
    hostLabel = hostLabel,
    timestamp = timestamp
)
