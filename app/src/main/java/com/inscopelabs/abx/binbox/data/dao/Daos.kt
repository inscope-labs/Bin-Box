package com.inscopelabs.abx.binbox.data.dao

import androidx.room.*
import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.data.entity.KnownHostKeyEntity
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HostDao {
    @Query("SELECT * FROM hosts ORDER BY isFavorite DESC, lastConnectedAt DESC, id DESC")
    fun getAllHosts(): Flow<List<HostEntity>>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getHostById(id: Long): HostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: HostEntity): Long

    @Update
    suspend fun updateHost(host: HostEntity)

    @Delete
    suspend fun deleteHost(host: HostEntity)

    @Query("UPDATE hosts SET lastLatencyMs = :latencyMs WHERE id = :id")
    suspend fun updateLatency(id: Long, latencyMs: Long?)

    @Query("UPDATE hosts SET lastConnectedAt = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)

    @Query("UPDATE hosts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}

@Dao
interface KeyDao {
    @Query("SELECT * FROM ssh_keys ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM ssh_keys WHERE id = :id")
    suspend fun getKeyById(id: Long): KeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntity): Long

    @Delete
    suspend fun deleteKey(key: KeyEntity)
}

@Dao
interface SnippetDao {
    @Query("SELECT * FROM snippets ORDER BY isFavorite DESC, usageCount DESC, id DESC")
    fun getAllSnippets(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity): Long

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    @Delete
    suspend fun deleteSnippet(snippet: SnippetEntity)

    @Query("UPDATE snippets SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

/**
 * Deliberately non-suspend: JSch's HostKeyRepository interface is called
 * synchronously mid-handshake (inside session.connect()), which itself
 * already runs on a background dispatcher in SshTransport — blocking here
 * is correct, not an oversight.
 */
@Dao
interface KnownHostKeyDao {
    @Query("SELECT * FROM known_host_keys WHERE host = :host AND port = :port LIMIT 1")
    fun findByHostPortBlocking(host: String, port: Int): KnownHostKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBlocking(entity: KnownHostKeyEntity): Long

    @Query("DELETE FROM known_host_keys WHERE host = :host AND port = :port")
    fun deleteByHostPortBlocking(host: String, port: Int)

    @Query("SELECT * FROM known_host_keys ORDER BY firstSeenAt DESC")
    fun getAllBlocking(): List<KnownHostKeyEntity>
}
