package com.inscopelabs.abx.binbox.data.repository

import com.inscopelabs.abx.binbox.core.dispatcher.CoroutineDispatchersProvider
import com.inscopelabs.abx.binbox.core.dispatcher.DefaultCoroutineDispatchers
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.dao.HistoryDao
import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val dispatchers: CoroutineDispatchersProvider = DefaultCoroutineDispatchers
) : IHistoryRepository {

    override fun getRecentHistory(): Flow<List<CommandHistory>> {
        return historyDao.getRecentHistory()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun recordHistory(command: String, hostLabel: String): AppResult<Unit> = withContext(dispatchers.io) {
        if (command.isBlank()) return@withContext AppResult.Success(Unit)
        try {
            historyDao.insertHistory(
                HistoryEntity(
                    command = command.trim(),
                    hostLabel = hostLabel
                )
            )
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("HistoryRepository", "Failed to record history", e)
            AppResult.Error(AppError.IoError("Failed to record command history", e))
        }
    }

    override suspend fun clearHistory(): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            historyDao.clearHistory()
            BinBoxLogger.i("HistoryRepository", "Command history cleared")
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("HistoryRepository", "Failed to clear history", e)
            AppResult.Error(AppError.IoError("Failed to clear command history", e))
        }
    }
}
