package com.inscopelabs.abx.binbox.domain.usecase

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import kotlinx.coroutines.flow.Flow

class GetKeysUseCase(private val keyRepository: IKeyRepository) {
    operator fun invoke(): Flow<List<SshKey>> = keyRepository.getAllKeys()
}

class SaveKeyUseCase(private val keyRepository: IKeyRepository) {
    suspend operator fun invoke(key: SshKey): AppResult<Long> =
        keyRepository.saveKey(key)
}

class DeleteKeyUseCase(private val keyRepository: IKeyRepository) {
    suspend operator fun invoke(key: SshKey): AppResult<Unit> =
        keyRepository.deleteKey(key)
}

class GenerateKeyPairUseCase(private val keyRepository: IKeyRepository) {
    suspend operator fun invoke(title: String, keySize: Int = 2048): AppResult<SshKey> =
        keyRepository.generateRsaKeyPair(title, keySize)
}
