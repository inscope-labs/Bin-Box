package com.inscopelabs.abx.binbox.domain.usecase

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.repository.IHostRepository
import kotlinx.coroutines.flow.Flow

class GetHostsUseCase(private val hostRepository: IHostRepository) {
    operator fun invoke(): Flow<List<ConnectionProfile>> = hostRepository.getAllHosts()
}

class SaveHostUseCase(private val hostRepository: IHostRepository) {
    suspend operator fun invoke(profile: ConnectionProfile): AppResult<Long> =
        hostRepository.saveHost(profile)
}

class DeleteHostUseCase(private val hostRepository: IHostRepository) {
    suspend operator fun invoke(profile: ConnectionProfile): AppResult<Unit> =
        hostRepository.deleteHost(profile)
}

class ToggleFavoriteHostUseCase(private val hostRepository: IHostRepository) {
    suspend operator fun invoke(id: Long, isFavorite: Boolean): AppResult<Unit> =
        hostRepository.toggleFavorite(id, isFavorite)
}

class PingHostUseCase(private val hostRepository: IHostRepository) {
    suspend operator fun invoke(profile: ConnectionProfile): AppResult<Long> =
        hostRepository.pingHost(profile)
}
