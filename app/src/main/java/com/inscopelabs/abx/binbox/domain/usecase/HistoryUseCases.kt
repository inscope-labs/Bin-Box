package com.inscopelabs.abx.binbox.domain.usecase

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(private val historyRepository: IHistoryRepository) {
    operator fun invoke(): Flow<List<CommandHistory>> = historyRepository.getRecentHistory()
}

class RecordHistoryUseCase(private val historyRepository: IHistoryRepository) {
    suspend operator fun invoke(command: String, hostLabel: String): AppResult<Unit> =
        historyRepository.recordHistory(command, hostLabel)
}

class ClearHistoryUseCase(private val historyRepository: IHistoryRepository) {
    suspend operator fun invoke(): AppResult<Unit> =
        historyRepository.clearHistory()
}
