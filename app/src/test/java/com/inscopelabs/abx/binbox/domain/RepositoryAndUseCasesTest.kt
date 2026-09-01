package com.inscopelabs.abx.binbox.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.core.dispatcher.DefaultCoroutineDispatchers
import com.inscopelabs.abx.binbox.core.result.getOrNull
import com.inscopelabs.abx.binbox.data.database.AppDatabase
import com.inscopelabs.abx.binbox.data.repository.HistoryRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.HostRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.KeyRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.SessionRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.SnippetRepositoryImpl
import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import com.inscopelabs.abx.binbox.domain.usecase.ClearHistoryUseCase
import com.inscopelabs.abx.binbox.domain.usecase.ExecuteSnippetUseCase
import com.inscopelabs.abx.binbox.domain.usecase.GenerateKeyPairUseCase
import com.inscopelabs.abx.binbox.domain.usecase.GetHistoryUseCase
import com.inscopelabs.abx.binbox.domain.usecase.GetHostsUseCase
import com.inscopelabs.abx.binbox.domain.usecase.GetKeysUseCase
import com.inscopelabs.abx.binbox.domain.usecase.GetSnippetsUseCase
import com.inscopelabs.abx.binbox.domain.usecase.ManageSessionUseCase
import com.inscopelabs.abx.binbox.domain.usecase.SaveHostUseCase
import com.inscopelabs.abx.binbox.domain.usecase.ToggleFavoriteHostUseCase
import com.inscopelabs.abx.binbox.security.SecureStorageService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepositoryAndUseCasesTest {

    private lateinit var database: AppDatabase
    private lateinit var hostRepo: HostRepositoryImpl
    private lateinit var keyRepo: KeyRepositoryImpl
    private lateinit var snippetRepo: SnippetRepositoryImpl
    private lateinit var historyRepo: HistoryRepositoryImpl
    private lateinit var sessionRepo: SessionRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val dispatchers = DefaultCoroutineDispatchers
        val secureStorage = SecureStorageService(context)
        hostRepo = HostRepositoryImpl(database.hostDao(), secureStorage, dispatchers)
        keyRepo = KeyRepositoryImpl(database.keyDao(), secureStorage, dispatchers)
        snippetRepo = SnippetRepositoryImpl(database.snippetDao(), dispatchers)
        historyRepo = HistoryRepositoryImpl(database.historyDao(), dispatchers)
        sessionRepo = SessionRepositoryImpl()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testHostRepositoryAndUseCases() = runTest {
        val saveHostUseCase = SaveHostUseCase(hostRepo)
        val getHostsUseCase = GetHostsUseCase(hostRepo)
        val toggleFavoriteUseCase = ToggleFavoriteHostUseCase(hostRepo)

        val profile = ConnectionProfile(
            label = "Production DB Cluster",
            host = "db.internal.cloud",
            port = 22,
            protocol = ProtocolType.SSH,
            username = "dbadmin",
            authType = AuthType.PASSWORD,
            groupTag = "Database"
        )

        val saveResult = saveHostUseCase(profile)
        assertTrue(saveResult.isSuccess)
        val hostId = saveResult.getOrNull() ?: 0L
        assertTrue(hostId > 0L)

        val hosts = getHostsUseCase().first()
        assertEquals(1, hosts.size)
        assertEquals("Production DB Cluster", hosts[0].label)
        assertFalse(hosts[0].isFavorite)

        // Toggle Favorite
        toggleFavoriteUseCase(hostId, true)
        val updatedHosts = getHostsUseCase().first()
        assertTrue(updatedHosts[0].isFavorite)

        // Local Shell Ping
        val localProfile = ConnectionProfile(id = hostId, label = "Local", host = "localhost", protocol = ProtocolType.LOCAL_SHELL)
        val pingResult = hostRepo.pingHost(localProfile)
        assertTrue(pingResult.isSuccess)
        assertEquals(1L, pingResult.getOrNull())

        // Delete Host Use Case
        val deleteHostUseCase = com.inscopelabs.abx.binbox.domain.usecase.DeleteHostUseCase(hostRepo)
        val deleteResult = deleteHostUseCase(profile.copy(id = hostId))
        assertTrue(deleteResult.isSuccess)
        val remainingHosts = getHostsUseCase().first()
        assertTrue(remainingHosts.isEmpty())
    }

    @Test
    fun testKeyRepositoryAndGenerationUseCase() = runTest {
        val generateKeyPairUseCase = GenerateKeyPairUseCase(keyRepo)
        val getKeysUseCase = GetKeysUseCase(keyRepo)

        val genResult = generateKeyPairUseCase("dev_key_2048", 2048)
        assertTrue(genResult.isSuccess)
        val key = genResult.getOrNull()
        assertNotNull(key)
        assertEquals("dev_key_2048", key?.title)
        assertTrue(key?.publicKey?.startsWith("ssh-rsa ") == true)
        assertTrue(key?.fingerprint?.startsWith("SHA256:") == true)

        val allKeys = getKeysUseCase().first()
        assertEquals(1, allKeys.size)
        assertEquals(key?.id, allKeys[0].id)
    }

    @Test
    fun testSnippetExecutionAndTemplateInterpolation() = runTest {
        val snippet = Snippet(
            title = "Ping Test",
            commandTemplate = "ping -c {{count:4}} {{host:1.1.1.1}}",
            category = "Network"
        )
        val saveResult = snippetRepo.saveSnippet(snippet)
        val snippetId = saveResult.getOrNull() ?: 0L
        val savedSnippet = snippet.copy(id = snippetId)

        val executeSnippetUseCase = ExecuteSnippetUseCase(snippetRepo, historyRepo)
        val getHistoryUseCase = GetHistoryUseCase(historyRepo)

        // Test default parameters fallback
        val defaultExecResult = executeSnippetUseCase(savedSnippet)
        assertTrue(defaultExecResult.isSuccess)
        assertEquals("ping -c 4 1.1.1.1", defaultExecResult.getOrNull())

        // Test custom parameters interpolation
        val customParams = mapOf("count" to "10", "host" to "8.8.8.8")
        val customExecResult = executeSnippetUseCase(savedSnippet, customParams, "Cloud VPS")
        assertTrue(customExecResult.isSuccess)
        assertEquals("ping -c 10 8.8.8.8", customExecResult.getOrNull())

        // Verify history recording
        val history = getHistoryUseCase().first()
        assertEquals(2, history.size)
        assertEquals("ping -c 10 8.8.8.8", history[0].command)
    }

    @Test
    fun testSessionRepositoryLifecycleAndTraffic() {
        val manageSessionUseCase = ManageSessionUseCase(sessionRepo)
        val profile = ConnectionProfile(label = "Sandbox 1", host = "demo.sandbox", protocol = ProtocolType.DEMO_HOST)

        val startResult = manageSessionUseCase.startSession(profile)
        assertTrue(startResult.isSuccess)
        val session = startResult.getOrNull()
        assertNotNull(session)
        val sessionId = session!!.sessionId

        assertEquals(1, manageSessionUseCase.activeSessions.value.size)
        assertEquals(sessionId, manageSessionUseCase.focusedSessionId.value)

        // State update
        manageSessionUseCase.updateState(sessionId, TerminalSessionState.CONNECTED)
        assertEquals(TerminalSessionState.CONNECTED, sessionRepo.getSession(sessionId)?.state)

        // Traffic update
        manageSessionUseCase.recordTraffic(sessionId, 512L, 128L)
        val sessionWithTraffic = sessionRepo.getSession(sessionId)
        assertEquals(512L, sessionWithTraffic?.totalBytesReceived)
        assertEquals(128L, sessionWithTraffic?.totalBytesSent)

        // Close session
        manageSessionUseCase.closeSession(sessionId)
        assertTrue(manageSessionUseCase.activeSessions.value.isEmpty())
    }

    @Test
    fun testHistoryClearance() = runTest {
        val clearHistoryUseCase = ClearHistoryUseCase(historyRepo)
        val getHistoryUseCase = GetHistoryUseCase(historyRepo)

        historyRepo.recordHistory("ls -la", "Host A")
        historyRepo.recordHistory("docker ps", "Host A")

        assertEquals(2, getHistoryUseCase().first().size)

        clearHistoryUseCase()
        assertTrue(getHistoryUseCase().first().isEmpty())
    }
}
