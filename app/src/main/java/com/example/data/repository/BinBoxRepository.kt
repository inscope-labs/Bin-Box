package com.example.data.repository

import android.util.Base64
import com.example.data.database.AppDatabase
import com.example.data.entity.HistoryEntity
import com.example.data.entity.HostEntity
import com.example.data.entity.KeyEntity
import com.example.data.entity.SnippetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey

class BinBoxRepository(private val database: AppDatabase) {

    val allHosts: Flow<List<HostEntity>> = database.hostDao().getAllHosts()
    val allKeys: Flow<List<KeyEntity>> = database.keyDao().getAllKeys()
    val allSnippets: Flow<List<SnippetEntity>> = database.snippetDao().getAllSnippets()
    val recentHistory: Flow<List<HistoryEntity>> = database.historyDao().getRecentHistory()

    suspend fun getHostById(id: Long): HostEntity? = withContext(Dispatchers.IO) {
        database.hostDao().getHostById(id)
    }

    suspend fun saveHost(host: HostEntity): Long = withContext(Dispatchers.IO) {
        if (host.id == 0L) {
            database.hostDao().insertHost(host)
        } else {
            database.hostDao().updateHost(host)
            host.id
        }
    }

    suspend fun deleteHost(host: HostEntity) = withContext(Dispatchers.IO) {
        database.hostDao().deleteHost(host)
    }

    suspend fun toggleHostFavorite(host: HostEntity) = withContext(Dispatchers.IO) {
        database.hostDao().toggleFavorite(host.id, !host.isFavorite)
    }

    suspend fun saveKey(key: KeyEntity): Long = withContext(Dispatchers.IO) {
        database.keyDao().insertKey(key)
    }

    suspend fun deleteKey(key: KeyEntity) = withContext(Dispatchers.IO) {
        database.keyDao().deleteKey(key)
    }

    suspend fun saveSnippet(snippet: SnippetEntity): Long = withContext(Dispatchers.IO) {
        if (snippet.id == 0L) {
            database.snippetDao().insertSnippet(snippet)
        } else {
            database.snippetDao().updateSnippet(snippet)
            snippet.id
        }
    }

    suspend fun deleteSnippet(snippet: SnippetEntity) = withContext(Dispatchers.IO) {
        database.snippetDao().deleteSnippet(snippet)
    }

    suspend fun incrementSnippetUsage(id: Long) = withContext(Dispatchers.IO) {
        database.snippetDao().incrementUsage(id)
    }

    suspend fun recordHistory(command: String, hostLabel: String) = withContext(Dispatchers.IO) {
        if (command.isNotBlank()) {
            database.historyDao().insertHistory(
                HistoryEntity(
                    command = command.trim(),
                    hostLabel = hostLabel
                )
            )
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.historyDao().clearHistory()
    }

    suspend fun pingHost(hostEntity: HostEntity): Long? = withContext(Dispatchers.IO) {
        if (hostEntity.protocol == "LOCAL_SHELL") {
            database.hostDao().updateLatency(hostEntity.id, 1L)
            return@withContext 1L
        }

        if (hostEntity.protocol == "DEMO_HOST") {
            val simulatedPing = (8..25).random().toLong()
            database.hostDao().updateLatency(hostEntity.id, simulatedPing)
            return@withContext simulatedPing
        }

        try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            val address = InetSocketAddress(hostEntity.host, if (hostEntity.port > 0) hostEntity.port else 22)
            socket.connect(address, 2000)
            val elapsed = System.currentTimeMillis() - startTime
            socket.close()
            database.hostDao().updateLatency(hostEntity.id, elapsed)
            elapsed
        } catch (e: Exception) {
            database.hostDao().updateLatency(hostEntity.id, null)
            null
        }
    }

    suspend fun generateRsaKeyPair(title: String, keySize: Int = 2048): KeyEntity = withContext(Dispatchers.IO) {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(keySize)
        val kp = kpg.generateKeyPair()

        val privateKeyPem = buildString {
            append("-----BEGIN RSA PRIVATE KEY-----\n")
            append(Base64.encodeToString(kp.private.encoded, Base64.DEFAULT).trim())
            append("\n-----END RSA PRIVATE KEY-----\n")
        }

        // Format SSH-RSA Public Key
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

        // Calculate SHA256 Fingerprint
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(byteOs.toByteArray())
        val fingerprint = "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP).trimEnd('=')

        val keyEntity = KeyEntity(
            title = title.ifBlank { "id_rsa_${keySize}_" + System.currentTimeMillis().toString().takeLast(4) },
            keyType = "RSA $keySize-bit",
            publicKey = fullPublicKey,
            privateKey = privateKeyPem,
            fingerprint = fingerprint
        )

        val id = database.keyDao().insertKey(keyEntity)
        keyEntity.copy(id = id)
    }
}
