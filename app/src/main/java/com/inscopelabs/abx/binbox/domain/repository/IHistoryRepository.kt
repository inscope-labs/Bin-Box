package com.inscopelabs.abx.binbox.domain.repository

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import kotlinx.coroutines.flow.Flow

interface IHistoryRepository {
    fun getRecentHistory(): Flow<List<CommandHistory>>
    suspend fun recordHistory(command: String, hostLabel: String): AppResult<Unit>
    suspend fun clearHistory(): AppResult<Unit>
}
