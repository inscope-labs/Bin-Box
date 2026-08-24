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
}
