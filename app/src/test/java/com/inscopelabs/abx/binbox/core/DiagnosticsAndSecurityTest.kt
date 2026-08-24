package com.inscopelabs.abx.binbox.core

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.core.diagnostics.SessionTelemetryTracker
import com.inscopelabs.abx.binbox.core.diagnostics.SystemDiagnosticsCollector
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.logging.LogLevel
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.security.SecureStorageService
import com.inscopelabs.abx.binbox.security.SshKeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiagnosticsAndSecurityTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        BinBoxLogger.clear()
        BinBoxLogger.minLogLevel = LogLevel.DEBUG
    }

    @Test
    fun testBinBoxLogger_bufferingAndLevels() {
        BinBoxLogger.d("TestTag", "Debug message 1")
        BinBoxLogger.i("TestTag", "Info message 2")
        BinBoxLogger.w("TestTag", "Warning message 3")
        BinBoxLogger.e("TestTag", "Error message 4", RuntimeException("Boom"))

        val logs = BinBoxLogger.getLogs()
        assertEquals(4, logs.size)
        assertEquals("Debug message 1", logs[0].message)
        assertEquals(LogLevel.DEBUG, logs[0].level)
        assertEquals(LogLevel.ERROR, logs[3].level)
        assertNotNull(logs[3].throwable)
        assertNotNull(logs[0].formattedTime)

        BinBoxLogger.clear()
        assertTrue(BinBoxLogger.getLogs().isEmpty())
    }

    @Test
    fun testSystemDiagnosticsCollector_snapshot() {
        val collector = SystemDiagnosticsCollector(app)
        val snapshot = collector.collectSnapshot()

        assertNotNull(snapshot)
        assertTrue(snapshot.deviceModel.isNotBlank())
        assertTrue(snapshot.androidVersion.isNotBlank())
        assertTrue(snapshot.sdkInt > 0)
        assertTrue(snapshot.cpuCores >= 1)
        assertTrue(snapshot.kernelInfo.isNotBlank())
        assertTrue(snapshot.memory.totalRamMb > 0)
        assertTrue(snapshot.storage.totalStorageGb >= 0)
        assertNotNull(snapshot.network.transportType)
    }

    @Test
    fun testSessionTelemetryTracker_metricsFlow() {
        val tracker = SessionTelemetryTracker()

        tracker.recordSessionConnected("session-1", "Oracle VM", "SSH")
        val initial = tracker.getMetrics("session-1")
        assertNotNull(initial)
        assertEquals("Oracle VM", initial?.hostLabel)
        assertEquals("SSH", initial?.protocol)
        assertEquals(0L, initial?.bytesIn)

        tracker.recordTraffic("session-1", bytesInDelta = 1024, bytesOutDelta = 256)
        tracker.recordLatency("session-1", latencyMs = 45)

        val updated = tracker.getMetrics("session-1")
        assertNotNull(updated)
        assertEquals(1024L, updated?.bytesIn)
        assertEquals(256L, updated?.bytesOut)
        assertEquals(45L, updated?.latencyMs)
        assertEquals(1L, updated?.packetCount)

        tracker.removeSession("session-1")
        assertEquals(null, tracker.getMetrics("session-1"))
    }

    @Test
    fun testSshKeyManager_rsaAndEcGeneration() {
        val rsaResult = SshKeyManager.generateRsaKey("test_rsa", 2048)
        assertTrue(rsaResult is AppResult.Success)
        val rsaKey = (rsaResult as AppResult.Success).data
        assertEquals("test_rsa", rsaKey.title)
        assertTrue(rsaKey.publicKey.startsWith("ssh-rsa AAA"))
        assertTrue(rsaKey.privateKey.contains("BEGIN RSA PRIVATE KEY"))
        assertTrue(rsaKey.fingerprint.startsWith("SHA256:"))

        val ecResult = SshKeyManager.generateEcKey("test_ecdsa")
        assertTrue(ecResult is AppResult.Success)
        val ecKey = (ecResult as AppResult.Success).data
        assertEquals("test_ecdsa", ecKey.title)
        assertTrue(ecKey.publicKey.startsWith("ecdsa-sha2-nistp256 AAA"))
        assertTrue(ecKey.privateKey.contains("BEGIN EC PRIVATE KEY"))
        assertTrue(ecKey.fingerprint.startsWith("SHA256:"))
    }

    @Test
    fun testSecureStorageService_encryptDecryptAndClear() {
        val storage = SecureStorageService(app)
        val testSecret = "SuperSecretPassword123!".toByteArray(Charsets.UTF_8)

        val encResult = storage.encrypt(testSecret)
        assertTrue(encResult is AppResult.Success)
        val ciphertext = (encResult as AppResult.Success).data

        val decResult = storage.decrypt(ciphertext)
        assertTrue(decResult is AppResult.Success)
        val decrypted = (decResult as AppResult.Success).data
        assertEquals("SuperSecretPassword123!", String(decrypted, Charsets.UTF_8))

        // Sensitive memory wipe
        SecureStorageService.clearSensitiveData(testSecret)
        SecureStorageService.clearSensitiveData(decrypted)
        assertEquals(0.toByte(), testSecret[0])
        assertEquals(0.toByte(), decrypted[0])
    }

    @Test
    fun testCredentialCrypto_roundTripsAndTagsCiphertext() {
        val storage = SecureStorageService(app)

        val encrypted = com.inscopelabs.abx.binbox.security.CredentialCrypto.encryptField(storage, "hunter2")
        assertNotNull(encrypted)
        assertTrue(encrypted!!.startsWith("ENC1:"))
        assertTrue(encrypted != "hunter2")

        val decrypted = com.inscopelabs.abx.binbox.security.CredentialCrypto.decryptField(storage, encrypted)
        assertEquals("hunter2", decrypted)
    }

    @Test
    fun testCredentialCrypto_legacyPlaintextPassesThroughUnchanged() {
        val storage = SecureStorageService(app)

        // A row written before CredentialCrypto existed has no ENC1: prefix.
        // It must come back unchanged, not be treated as corrupt ciphertext.
        val legacyValue = "plaintext-password-from-before-phase-9"
        val result = com.inscopelabs.abx.binbox.security.CredentialCrypto.decryptField(storage, legacyValue)
        assertEquals(legacyValue, result)
    }

    @Test
    fun testCredentialCrypto_nullAndBlankPassThrough() {
        val storage = SecureStorageService(app)
        assertEquals(null, com.inscopelabs.abx.binbox.security.CredentialCrypto.encryptField(storage, null))
        assertEquals(null, com.inscopelabs.abx.binbox.security.CredentialCrypto.decryptField(storage, null))
        assertEquals("", com.inscopelabs.abx.binbox.security.CredentialCrypto.encryptField(storage, ""))
    }

    @Test
    fun testHostKeyStore_trustOnFirstUse_thenAcceptsMatchingKey() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            app, com.inscopelabs.abx.binbox.data.database.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val store = com.inscopelabs.abx.binbox.security.HostKeyStore(db.knownHostKeyDao())
        val fakeKey = "ssh-ed25519-fake-key-bytes-for-test".toByteArray(Charsets.UTF_8)

        // First contact: no stored key yet — trusted and remembered.
        val firstResult = store.check("example.binbox.test", fakeKey)
        assertEquals(com.jcraft.jsch.HostKeyRepository.OK, firstResult)

        // Same key again: matches what was stored — still OK.
        val secondResult = store.check("example.binbox.test", fakeKey)
        assertEquals(com.jcraft.jsch.HostKeyRepository.OK, secondResult)

        db.close()
    }

    @Test
    fun testHostKeyStore_rejectsChangedKey() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            app, com.inscopelabs.abx.binbox.data.database.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val store = com.inscopelabs.abx.binbox.security.HostKeyStore(db.knownHostKeyDao())
        val originalKey = "ssh-ed25519-original-key-bytes".toByteArray(Charsets.UTF_8)
        val rotatedKey = "ssh-ed25519-a-totally-different-key".toByteArray(Charsets.UTF_8)

        assertEquals(com.jcraft.jsch.HostKeyRepository.OK, store.check("pi.homelab.test", originalKey))

        // A different key presented for the same host — must be rejected, not silently trusted.
        val mismatchResult = store.check("pi.homelab.test", rotatedKey)
        assertEquals(com.jcraft.jsch.HostKeyRepository.CHANGED, mismatchResult)

        db.close()
    }

    @Test
    fun testHostKeyStore_bracketedHostPortIsParsedCorrectly() {
        val db = androidx.room.Room.inMemoryDatabaseBuilder(
            app, com.inscopelabs.abx.binbox.data.database.AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val store = com.inscopelabs.abx.binbox.security.HostKeyStore(db.knownHostKeyDao())
        val key = "ssh-rsa-fake-key-bytes".toByteArray(Charsets.UTF_8)

        // JSch formats non-default-port hosts as "[host]:port".
        assertEquals(com.jcraft.jsch.HostKeyRepository.OK, store.check("[pi.homelab.test]:2222", key))

        val stored = db.knownHostKeyDao().findByHostPortBlocking("pi.homelab.test", 2222)
        assertNotNull(stored)
        assertEquals(2222, stored?.port)

        // A different port on the same host is a distinct trust record, not the same one.
        val differentPortResult = store.check("pi.homelab.test", key)
        assertEquals(com.jcraft.jsch.HostKeyRepository.OK, differentPortResult)
        val storedDefaultPort = db.knownHostKeyDao().findByHostPortBlocking("pi.homelab.test", 22)
        assertNotNull(storedDefaultPort)

        db.close()
    }
}
