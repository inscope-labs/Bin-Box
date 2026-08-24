package com.inscopelabs.abx.binbox.data.repository

import android.util.Base64
import com.inscopelabs.abx.binbox.core.dispatcher.CoroutineDispatchersProvider
import com.inscopelabs.abx.binbox.core.dispatcher.DefaultCoroutineDispatchers
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.dao.KeyDao
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

class KeyRepositoryImpl(
    private val keyDao: KeyDao,
    private val dispatchers: CoroutineDispatchersProvider = DefaultCoroutineDispatchers
) : IKeyRepository {

    override fun getAllKeys(): Flow<List<SshKey>> {
        return keyDao.getAllKeys()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getKeyById(id: Long): SshKey? = withContext(dispatchers.io) {
        keyDao.getKeyById(id)?.toDomain()
    }

    override suspend fun saveKey(key: SshKey): AppResult<Long> = withContext(dispatchers.io) {
        try {
            val id = keyDao.insertKey(key.toEntity())
            BinBoxLogger.d("KeyRepository", "Saved key: ${key.title} (ID: $id)")
            AppResult.Success(id)
        } catch (e: Throwable) {
            BinBoxLogger.e("KeyRepository", "Failed to save key: ${key.title}", e)
            AppResult.Error(AppError.CryptoError("Failed to save key", e))
        }
    }

    override suspend fun deleteKey(key: SshKey): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            keyDao.deleteKey(key.toEntity())
            BinBoxLogger.d("KeyRepository", "Deleted key: ${key.title}")
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("KeyRepository", "Failed to delete key: ${key.title}", e)
            AppResult.Error(AppError.CryptoError("Failed to delete key", e))
        }
    }

    override suspend fun generateRsaKeyPair(title: String, keySize: Int): AppResult<SshKey> = withContext(dispatchers.io) {
        when (val genResult = com.inscopelabs.abx.binbox.security.SshKeyManager.generateRsaKey(title, keySize)) {
            is AppResult.Success -> {
                val key = genResult.data
                try {
                    val id = keyDao.insertKey(key.toEntity())
                    val savedKey = key.copy(id = id)
                    BinBoxLogger.i("KeyRepository", "Saved generated RSA KeyPair (ID: $id, ${key.fingerprint})")
                    AppResult.Success(savedKey)
                } catch (e: Throwable) {
                    BinBoxLogger.e("KeyRepository", "Failed to persist generated RSA key", e)
                    AppResult.Error(AppError.CryptoError("Failed to persist generated RSA key", e))
                }
            }
            is AppResult.Error -> genResult
            is AppResult.Loading -> AppResult.Loading
        }
    }
}
