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
        try {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(keySize)
            val kp = kpg.generateKeyPair()

            val privateKeyPem = buildString {
                append("-----BEGIN RSA PRIVATE KEY-----\n")
                append(Base64.encodeToString(kp.private.encoded, Base64.DEFAULT).trim())
                append("\n-----END RSA PRIVATE KEY-----\n")
            }

            val rsaPub = kp.public as RSAPublicKey
            val byteOs = ByteArrayOutputStream()
            val dos = DataOutputStream(byteOs)
            dos.writeInt("ssh-rsa".toByteArray().size)
            dos.write("ssh-rsa".toByteArray())
            val exp = rsaPub.publicExponent.toByteArray()
            dos.writeInt(exp.size)
            dos.write(exp)
            val mod = rsaPub.modulus.toByteArray()
            dos.writeInt(mod.size)
            dos.write(mod)
            dos.close()

            val pubKeyBase64 = Base64.encodeToString(byteOs.toByteArray(), Base64.NO_WRAP)
            val fullPublicKey = "ssh-rsa $pubKeyBase64 binbox@device"

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(byteOs.toByteArray())
            val fingerprint = "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP).trimEnd('=')

            val formattedTitle = title.ifBlank { "id_rsa_${keySize}_" + System.currentTimeMillis().toString().takeLast(4) }
            val key = SshKey(
                title = formattedTitle,
                keyType = "RSA $keySize-bit",
                publicKey = fullPublicKey,
                privateKey = privateKeyPem,
                fingerprint = fingerprint,
                createdAt = System.currentTimeMillis()
            )

            val id = keyDao.insertKey(key.toEntity())
            val savedKey = key.copy(id = id)
            BinBoxLogger.i("KeyRepository", "Generated RSA KeyPair ($keySize-bit): $fingerprint")
            AppResult.Success(savedKey)
        } catch (e: Throwable) {
            BinBoxLogger.e("KeyRepository", "Failed to generate RSA KeyPair", e)
            AppResult.Error(AppError.CryptoError("Failed to generate RSA Key: ${e.message}", e))
        }
    }
}
