package com.inscopelabs.abx.binbox.domain.repository

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.SshKey
import kotlinx.coroutines.flow.Flow

interface IKeyRepository {
    fun getAllKeys(): Flow<List<SshKey>>
    suspend fun getKeyById(id: Long): SshKey?
    suspend fun saveKey(key: SshKey): AppResult<Long>
    suspend fun deleteKey(key: SshKey): AppResult<Unit>
    suspend fun generateRsaKeyPair(title: String, keySize: Int = 2048): AppResult<SshKey>
}
