package com.inscopelabs.abx.binbox.domain.repository

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import kotlinx.coroutines.flow.Flow

interface IHostRepository {
    fun getAllHosts(): Flow<List<ConnectionProfile>>
    suspend fun getHostById(id: Long): ConnectionProfile?
    suspend fun saveHost(profile: ConnectionProfile): AppResult<Long>
    suspend fun deleteHost(profile: ConnectionProfile): AppResult<Unit>
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean): AppResult<Unit>
    suspend fun pingHost(profile: ConnectionProfile): AppResult<Long>
}
