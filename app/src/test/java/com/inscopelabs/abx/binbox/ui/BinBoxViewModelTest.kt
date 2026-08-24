package com.inscopelabs.abx.binbox.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.model.TerminalSession
import com.inscopelabs.abx.binbox.domain.model.TerminalSessionState
import com.inscopelabs.abx.binbox.domain.repository.IHistoryRepository
import com.inscopelabs.abx.binbox.domain.repository.IHostRepository
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import com.inscopelabs.abx.binbox.domain.repository.ISessionRepository
import com.inscopelabs.abx.binbox.domain.repository.ISnippetRepository
import com.inscopelabs.abx.binbox.domain.usecase.HistoryUseCases
import com.inscopelabs.abx.binbox.domain.usecase.HostUseCases
import com.inscopelabs.abx.binbox.domain.usecase.KeyUseCases
import com.inscopelabs.abx.binbox.domain.usecase.ManageSessionUseCase
import com.inscopelabs.abx.binbox.domain.usecase.SnippetUseCases
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionFactory
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.ui.i18n.AppLanguage
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BinBoxViewModelTest {

    private lateinit var app: Application
    private lateinit var testScope: CoroutineScope
    private lateinit var viewModel: BinBoxViewModel

    // Fake Repositories
    private val fakeHosts = MutableStateFlow<List<ConnectionProfile>>(
        listOf(
            ConnectionProfile(id = 1L, label = "Production VPS", host = "10.0.0.1", protocol = ProtocolType.SSH)
        )
    )

    private val fakeKeys = MutableStateFlow<List<SshKey>>(
        listOf(
            SshKey(id = 1L, title = "Personal Key", keyType = "RSA", publicKey = "PUB_KEY", privateKey = "PRIV_KEY", fingerprint = "SHA256:123")
        )
    )

    private val fakeSnippets = MutableStateFlow<List<Snippet>>(
        listOf(
            Snippet(id = 1L, title = "System Update", commandTemplate = "sudo apt update && sudo apt upgrade -y", category = "System")
        )
    )

    private val fakeHistory = MutableStateFlow<List<CommandHistory>>(
        listOf(
            CommandHistory(id = 1L, command = "docker ps", hostLabel = "Production VPS")
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app = ApplicationProvider.getApplicationContext()
        testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val hostRepo = object : IHostRepository {
            override fun getAllHosts(): Flow<List<ConnectionProfile>> = fakeHosts.asStateFlow()
            override suspend fun getHostById(id: Long): ConnectionProfile? = fakeHosts.value.find { it.id == id }
            override suspend fun saveHost(profile: ConnectionProfile): AppResult<Long> {
                fakeHosts.value = fakeHosts.value + profile.copy(id = 2L)
                return AppResult.Success(2L)
            }
            override suspend fun deleteHost(profile: ConnectionProfile): AppResult<Unit> {
                fakeHosts.value = fakeHosts.value.filter { it.id != profile.id }
                return AppResult.Success(Unit)
            }
            override suspend fun toggleFavorite(id: Long, isFavorite: Boolean): AppResult<Unit> {
                fakeHosts.value = fakeHosts.value.map { if (it.id == id) it.copy(isFavorite = isFavorite) else it }
                return AppResult.Success(Unit)
            }
            override suspend fun pingHost(profile: ConnectionProfile): AppResult<Long> = AppResult.Success(24L)
        }

        val keyRepo = object : IKeyRepository {
            override fun getAllKeys(): Flow<List<SshKey>> = fakeKeys.asStateFlow()
            override suspend fun getKeyById(id: Long): SshKey? = fakeKeys.value.find { it.id == id }
            override suspend fun saveKey(key: SshKey): AppResult<Long> {
                fakeKeys.value = fakeKeys.value + key.copy(id = 2L)
                return AppResult.Success(2L)
            }
            override suspend fun deleteKey(key: SshKey): AppResult<Unit> {
                fakeKeys.value = fakeKeys.value.filter { it.id != key.id }
                return AppResult.Success(Unit)
            }
            override suspend fun generateRsaKeyPair(title: String, keySize: Int): AppResult<SshKey> {
                val newKey = SshKey(id = 2L, title = title, keyType = "RSA-$keySize", publicKey = "PUB_GEN", privateKey = "PRIV_GEN", fingerprint = "FP_GEN")
                fakeKeys.value = fakeKeys.value + newKey
                return AppResult.Success(newKey)
            }
        }

        val snippetRepo = object : ISnippetRepository {
            override fun getAllSnippets(): Flow<List<Snippet>> = fakeSnippets.asStateFlow()
            override suspend fun getSnippetById(id: Long): Snippet? = fakeSnippets.value.find { it.id == id }
            override suspend fun saveSnippet(snippet: Snippet): AppResult<Long> {
                fakeSnippets.value = fakeSnippets.value + snippet.copy(id = 2L)
                return AppResult.Success(2L)
            }
            override suspend fun deleteSnippet(snippet: Snippet): AppResult<Unit> {
                fakeSnippets.value = fakeSnippets.value.filter { it.id != snippet.id }
                return AppResult.Success(Unit)
            }
            override suspend fun incrementUsage(id: Long): AppResult<Unit> = AppResult.Success(Unit)
        }

        val historyRepo = object : IHistoryRepository {
            override fun getRecentHistory(): Flow<List<CommandHistory>> = fakeHistory.asStateFlow()
            override suspend fun recordHistory(command: String, hostLabel: String): AppResult<Unit> {
                fakeHistory.value = listOf(CommandHistory(id = 2L, command = command, hostLabel = hostLabel)) + fakeHistory.value
                return AppResult.Success(Unit)
            }
            override suspend fun clearHistory(): AppResult<Unit> {
                fakeHistory.value = emptyList()
                return AppResult.Success(Unit)
            }
        }

        val sessionRepo = object : ISessionRepository {
            private val sessionsFlow = MutableStateFlow<List<TerminalSession>>(emptyList())
            private val focusFlow = MutableStateFlow<String?>(null)
            override val activeSessions = sessionsFlow.asStateFlow()
            override val focusedSessionId = focusFlow.asStateFlow()

            override fun getSession(sessionId: String): TerminalSession? =
                sessionsFlow.value.find { it.sessionId == sessionId }

            override fun createSession(session: TerminalSession): AppResult<TerminalSession> {
                sessionsFlow.value = sessionsFlow.value + session
                focusFlow.value = session.sessionId
                return AppResult.Success(session)
            }
            override fun updateSessionState(sessionId: String, state: TerminalSessionState, error: String?): AppResult<Unit> = AppResult.Success(Unit)
            override fun updateSessionDimensions(sessionId: String, cols: Int, rows: Int): AppResult<Unit> = AppResult.Success(Unit)
            override fun recordSessionTraffic(sessionId: String, bytesIn: Long, bytesOut: Long): AppResult<Unit> = AppResult.Success(Unit)
            override fun focusSession(sessionId: String): AppResult<Unit> {
                focusFlow.value = sessionId
                return AppResult.Success(Unit)
            }
            override fun renameSession(sessionId: String, newTitle: String): AppResult<Unit> {
                sessionsFlow.value = sessionsFlow.value.map {
                    if (it.sessionId == sessionId) it.copy(title = newTitle) else it
                }
                return AppResult.Success(Unit)
            }
            override fun closeSession(sessionId: String): AppResult<Unit> {
                sessionsFlow.value = sessionsFlow.value.filter { it.sessionId != sessionId }
                return AppResult.Success(Unit)
            }
            override fun closeAllSessions(): AppResult<Unit> {
                sessionsFlow.value = emptyList()
                focusFlow.value = null
                return AppResult.Success(Unit)
            }
        }

        val hostUseCases = HostUseCases.create(hostRepo)
        val keyUseCases = KeyUseCases.create(keyRepo)
        val snippetUseCases = SnippetUseCases.create(snippetRepo, historyRepo)
        val historyUseCases = HistoryUseCases.create(historyRepo)
        val manageSessionUseCase = ManageSessionUseCase(sessionRepo)

        val factory = TerminalSessionFactory(keyRepository = keyRepo)
        val sessionManager = TerminalSessionManager(
            sessionFactory = factory,
            sessionRepository = sessionRepo,
            scope = testScope
        )

        viewModel = BinBoxViewModel(
            application = app,
            hostUseCases = hostUseCases,
            keyUseCases = keyUseCases,
            snippetUseCases = snippetUseCases,
            historyUseCases = historyUseCases,
            manageSessionUseCase = manageSessionUseCase,
            sessionManager = sessionManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cancel()
    }

    @Test
    fun appTabNavigation_updatesCorrectly() {
        assertEquals(AppTab.TERMINAL, viewModel.currentAppTab.value)
        viewModel.setAppTab(AppTab.HOSTS)
        assertEquals(AppTab.HOSTS, viewModel.currentAppTab.value)
        viewModel.setAppTab(AppTab.KEYS)
        assertEquals(AppTab.KEYS, viewModel.currentAppTab.value)
        viewModel.setAppTab(AppTab.SNIPPETS)
        assertEquals(AppTab.SNIPPETS, viewModel.currentAppTab.value)
        viewModel.setAppTab(AppTab.SETTINGS)
        assertEquals(AppTab.SETTINGS, viewModel.currentAppTab.value)
    }

    @Test
    fun preferenceCustomization_updatesAndResets() {
        viewModel.setTheme(TerminalThemes.Dracula)
        assertEquals(TerminalThemes.Dracula.id, viewModel.currentTheme.value.id)

        viewModel.setFontSize(18)
        assertEquals(18, viewModel.fontSizeSp.value)

        viewModel.setCursorStyle(CursorStyle.UNDERLINE)
        assertEquals(CursorStyle.UNDERLINE, viewModel.cursorStyle.value)

        viewModel.toggleHapticFeedback(false)
        assertFalse(viewModel.hapticFeedbackEnabled.value)

        viewModel.setLanguage(AppLanguage.FRENCH)
        assertEquals(AppLanguage.FRENCH, viewModel.appLanguage.value)

        viewModel.resetPreferences()
        assertEquals(TerminalThemes.MonokaiPro.id, viewModel.currentTheme.value.id)
        assertEquals(13, viewModel.fontSizeSp.value)
        assertEquals(CursorStyle.BLOCK, viewModel.cursorStyle.value)
        assertTrue(viewModel.hapticFeedbackEnabled.value)
        assertEquals(AppLanguage.SYSTEM, viewModel.appLanguage.value)
    }

    @Test
    fun keypadModifiers_toggleState() {
        assertFalse(viewModel.ctrlLatched.value)
        viewModel.toggleCtrl()
        assertTrue(viewModel.ctrlLatched.value)
        viewModel.toggleCtrl()
        assertFalse(viewModel.ctrlLatched.value)

        assertFalse(viewModel.altLatched.value)
        viewModel.toggleAlt()
        assertTrue(viewModel.altLatched.value)
        viewModel.toggleAlt()
        assertFalse(viewModel.altLatched.value)
    }

    @Test
    fun searchState_updatesCorrectly() {
        assertFalse(viewModel.isSearching.value)
        assertEquals("", viewModel.searchQuery.value)

        viewModel.toggleSearching(true)
        assertTrue(viewModel.isSearching.value)

        viewModel.setSearchQuery("grep error")
        assertEquals("grep error", viewModel.searchQuery.value)

        viewModel.toggleSearching(false)
        assertFalse(viewModel.isSearching.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun telemetry_probeAndDismiss() {
        assertNull(viewModel.telemetry.value)
        viewModel.probeHostTelemetry("edge-node.internal")
        assertNotNull(viewModel.telemetry.value)
        assertEquals("edge-node.internal", viewModel.telemetry.value?.hostLabel)

        viewModel.dismissTelemetry()
        assertNull(viewModel.telemetry.value)
    }

    @Test
    fun sessions_creationAndSwitching() = runTest {
        viewModel.openLocalSession().join()
        assertTrue(viewModel.sessions.value.isNotEmpty())
        val initialSize = viewModel.sessions.value.size

        viewModel.openDemoSession().join()
        assertEquals(initialSize + 1, viewModel.sessions.value.size)

        val active = viewModel.activeSession.value
        assertNotNull(active)

        viewModel.selectSession(0)
        assertEquals(0, viewModel.activeSessionIndex.value)

        viewModel.sendSpecialKey(TerminalKey.CTRL_C)
        viewModel.sendCommand("echo test")
        viewModel.clearCurrentTerminal()

        viewModel.closeSession(0)
        assertEquals(initialSize, viewModel.sessions.value.size)
    }

    @Test
    fun snackbar_showsAndClears() {
        assertNull(viewModel.snackbarMessage.value)
        viewModel.showSnackbar("Test Message")
        assertEquals("Test Message", viewModel.snackbarMessage.value)
        viewModel.clearSnackbar()
        assertNull(viewModel.snackbarMessage.value)
    }
}
