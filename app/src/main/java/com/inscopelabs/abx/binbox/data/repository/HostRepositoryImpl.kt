package com.inscopelabs.abx.binbox.data.repository

import com.inscopelabs.abx.binbox.core.dispatcher.CoroutineDispatchersProvider
import com.inscopelabs.abx.binbox.core.dispatcher.DefaultCoroutineDispatchers
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.dao.HostDao
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.repository.IHostRepository
import com.inscopelabs.abx.binbox.security.CredentialCrypto
import com.inscopelabs.abx.binbox.security.SecureStorageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class HostRepositoryImpl(
    private val hostDao: HostDao,
    private val secureStorage: SecureStorageService,
    private val dispatchers: CoroutineDispatchersProvider = DefaultCoroutineDispatchers
) : IHostRepository {

    override fun getAllHosts(): Flow<List<ConnectionProfile>> {
        return hostDao.getAllHosts()
            .map { list -> list.map { it.toDomain().withDecryptedSecrets() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getHostById(id: Long): ConnectionProfile? = withContext(dispatchers.io) {
        hostDao.getHostById(id)?.toDomain()?.withDecryptedSecrets()
    }

    override suspend fun saveHost(profile: ConnectionProfile): AppResult<Long> = withContext(dispatchers.io) {
        try {
            val entity = profile.withEncryptedSecrets().toEntity()
            val id = if (entity.id == 0L) {
                hostDao.insertHost(entity)
            } else {
                hostDao.updateHost(entity)
                entity.id
            }
            BinBoxLogger.d("HostRepository", "Host saved successfully: ${profile.label} (ID: $id)")
            AppResult.Success(id)
        } catch (e: Throwable) {
            BinBoxLogger.e("HostRepository", "Failed to save host: ${profile.label}", e)
            AppResult.Error(AppError.IoError("Failed to save host configuration", e))
        }
    }

    override suspend fun deleteHost(profile: ConnectionProfile): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            val rowsDeleted = hostDao.deleteHostById(profile.id)
            if (rowsDeleted == 0) {
                hostDao.deleteHost(profile.withEncryptedSecrets().toEntity())
            }
            BinBoxLogger.d("HostRepository", "Host deleted: ${profile.label} (ID: ${profile.id})")
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("HostRepository", "Failed to delete host: ${profile.label}", e)
            AppResult.Error(AppError.IoError("Failed to delete host", e))
        }
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            hostDao.toggleFavorite(id, isFavorite)
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            AppResult.Error(AppError.IoError("Failed to toggle favorite status", e))
        }
    }

    override suspend fun pingHost(profile: ConnectionProfile): AppResult<Long> = withContext(dispatchers.io) {
        if (profile.protocol == ProtocolType.LOCAL_SHELL) {
            hostDao.updateLatency(profile.id, 1L)
            return@withContext AppResult.Success(1L)
        }

        if (profile.protocol == ProtocolType.DEMO_HOST) {
            val simulatedPing = (8..25).random().toLong()
            hostDao.updateLatency(profile.id, simulatedPing)
            return@withContext AppResult.Success(simulatedPing)
        }

        try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            val port = if (profile.port > 0) profile.port else 22
            val address = InetSocketAddress(profile.host, port)
            socket.connect(address, 2000)
            val latency = System.currentTimeMillis() - startTime
            socket.close()
            hostDao.updateLatency(profile.id, latency)
            BinBoxLogger.d("HostRepository", "Ping successful for ${profile.host}:$port - ${latency}ms")
            AppResult.Success(latency)
        } catch (e: Throwable) {
            hostDao.updateLatency(profile.id, null)
            BinBoxLogger.w("HostRepository", "Ping failed for ${profile.host}:${profile.port}", e)
            AppResult.Error(AppError.NetworkError("Host unreachable: ${e.message}", e))
        }
    }

    private fun ConnectionProfile.withEncryptedSecrets(): ConnectionProfile = copy(
        password = CredentialCrypto.encryptField(secureStorage, password),
        keyPassphrase = CredentialCrypto.encryptField(secureStorage, keyPassphrase)
    )

    private fun ConnectionProfile.withDecryptedSecrets(): ConnectionProfile = copy(
        password = CredentialCrypto.decryptField(secureStorage, password),
        keyPassphrase = CredentialCrypto.decryptField(secureStorage, keyPassphrase)
    )
}
