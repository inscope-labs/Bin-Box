package com.inscopelabs.abx.binbox

import androidx.compose.ui.graphics.Color
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.repository.SessionRepositoryImpl
import com.inscopelabs.abx.binbox.domain.model.AuthType
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.ProtocolType
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository
import com.inscopelabs.abx.binbox.terminal.engine.AnsiParser
import com.inscopelabs.abx.binbox.terminal.engine.LocalShellSession
import com.inscopelabs.abx.binbox.terminal.engine.SandboxDemoShellSession
import com.inscopelabs.abx.binbox.terminal.engine.SshShellSession
import com.inscopelabs.abx.binbox.terminal.engine.TelnetShellSession
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionFactory
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TerminalEngineTest {

    private val testTheme = TerminalThemes.MonokaiPro

    @Test
    fun ansiParser_handlesPlainTextAndNewlines() {
        val parser = AnsiParser(testTheme)
        parser.feed("Hello BinBox\r\nSecond line\nThird line")

        val lines = parser.getLines()
        assertEquals(3, lines.size)
        assertEquals("Hello BinBox", lines[0].rawText)
        assertEquals("Second line", lines[1].rawText)
        assertEquals("Third line", lines[2].rawText)
    }

    @Test
    fun ansiParser_handlesSgrFormattingAndStyles() {
        val parser = AnsiParser(testTheme)
        // \u001b[1;3;4;31m Red Bold Italic Underline \u001b[0m Plain
        parser.feed("\u001B[1;3;4;31mStyled Text\u001B[0m Plain Text\n")

        val lines = parser.getLines()
        assertEquals(1, lines.size)
        val segments = lines[0].segments
        assertTrue(segments.size >= 2)

        val styledSegment = segments[0]
        assertEquals("Styled Text", styledSegment.text)
        assertTrue(styledSegment.style.isBold)
        assertTrue(styledSegment.style.isItalic)
        assertTrue(styledSegment.style.isUnderline)
        assertNotNull(styledSegment.style.foregroundColor)

        val plainSegment = segments[1]
        assertEquals(" Plain Text", plainSegment.text)
        assertEquals(false, plainSegment.style.isBold)
        assertEquals(false, plainSegment.style.isItalic)
    }

    @Test
    fun ansiParser_handlesTrueColorRgbSequences() {
        val parser = AnsiParser(testTheme)
        // 38;2;120;200;80m (Foreground RGB) and 48;2;20;30;40m (Background RGB)
        parser.feed("\u001B[38;2;120;200;80;48;2;20;30;40mTrueColor\u001B[0m\n")

        val lines = parser.getLines()
        assertEquals(1, lines.size)
        val seg = lines[0].segments[0]
        assertEquals("TrueColor", seg.text)
        assertEquals(Color(120, 200, 80), seg.style.foregroundColor)
        assertEquals(Color(20, 30, 40), seg.style.backgroundColor)
    }

    @Test
    fun ansiParser_handles256ColorCodes() {
        val parser = AnsiParser(testTheme)
        // 38;5;196m is Bright Red (Color Cube)
        parser.feed("\u001B[38;5;196mColor256\u001B[0m\n")

        val lines = parser.getLines()
        assertEquals(1, lines.size)
        val seg = lines[0].segments[0]
        assertEquals("Color256", seg.text)
        assertNotNull(seg.style.foregroundColor)
    }

    @Test
    fun ansiParser_handlesClearScreenAndScrollbackLimit() {
        val parser = AnsiParser(testTheme)
        parser.maxScrollback = 5

        repeat(10) { i ->
            parser.feed("Line $i\n")
        }

        val linesBeforeClear = parser.getLines()
        assertTrue(linesBeforeClear.size <= 5)

        // Clear screen with 2J
        parser.feed("\u001B[2J")
        val linesAfterClear = parser.getLines()
        assertEquals(0, linesAfterClear.size)
    }

    @Test
    fun ansiParser_triggersBellCallback() {
        var bellCount = 0
        val parser = AnsiParser(testTheme, onBell = { bellCount++ })

        parser.feed("Alert\u0007Beep\u0007")
        assertEquals(2, bellCount)
    }

    @Test
    fun sessionFactory_createsCorrectSessionTypes() = runTest {
        val fakeKeyRepo = object : IKeyRepository {
            override fun getAllKeys(): Flow<List<SshKey>> = flowOf(emptyList())
            override suspend fun getKeyById(id: Long): SshKey? = SshKey(id = id, title = "TestKey", privateKey = "PRIVATE_KEY_DATA", publicKey = "PUB", fingerprint = "FP")
            override suspend fun saveKey(key: SshKey): AppResult<Long> = AppResult.Success(1L)
            override suspend fun deleteKey(key: SshKey): AppResult<Unit> = AppResult.Success(Unit)
            override suspend fun generateRsaKeyPair(title: String, keySize: Int): AppResult<SshKey> =
                AppResult.Success(SshKey(id = 1L, title = title, privateKey = "PRIV", publicKey = "PUB", fingerprint = "FP"))
        }

        val factory = TerminalSessionFactory(keyRepository = fakeKeyRepo)

        // SSH profile
        val sshProfile = ConnectionProfile(
            label = "Production Server",
            host = "ssh.binbox.io",
            port = 22,
            protocol = ProtocolType.SSH,
            username = "admin",
            authType = AuthType.PRIVATE_KEY,
            keyId = 42L
        )
        val sshResult = factory.createSession(sshProfile)
        assertTrue(sshResult is AppResult.Success)
        val sshSession = (sshResult as AppResult.Success).data
        assertTrue(sshSession is SshShellSession)
        assertEquals("Production Server", sshSession.title)

        // Local Shell profile
        val localProfile = ConnectionProfile(
            label = "Local Terminal",
            host = "localhost",
            protocol = ProtocolType.LOCAL_SHELL
        )
        val localResult = factory.createSession(localProfile)
        assertTrue(localResult is AppResult.Success)
        assertTrue((localResult as AppResult.Success).data is LocalShellSession)

        // Demo Host profile
        val demoProfile = ConnectionProfile(
            label = "Demo VPS",
            host = "demo.binbox.io",
            protocol = ProtocolType.DEMO_HOST
        )
        val demoResult = factory.createSession(demoProfile)
        assertTrue(demoResult is AppResult.Success)
        assertTrue((demoResult as AppResult.Success).data is SandboxDemoShellSession)

        // Telnet profile
        val telnetProfile = ConnectionProfile(
            label = "Router Telnet",
            host = "192.168.1.1",
            port = 23,
            protocol = ProtocolType.TELNET
        )
        val telnetResult = factory.createSession(telnetProfile)
        assertTrue(telnetResult is AppResult.Success)
        assertTrue((telnetResult as AppResult.Success).data is TelnetShellSession)
    }

    @Test
    fun sessionManager_managesSessionLifecycleAndSwitching() = runTest {
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())
        val factory = TerminalSessionFactory()
        val sessionRepo = SessionRepositoryImpl()
        val manager = TerminalSessionManager(
            sessionFactory = factory,
            sessionRepository = sessionRepo,
            scope = testScope
        )

        val profile1 = ConnectionProfile(label = "Host A", host = "demo.binbox.io", protocol = ProtocolType.DEMO_HOST)
        val profile2 = ConnectionProfile(label = "Host B", host = "localhost", protocol = ProtocolType.LOCAL_SHELL)

        val res1 = manager.launchSession(profile1)
        val res2 = manager.launchSession(profile2)

        assertTrue(res1 is AppResult.Success)
        assertTrue(res2 is AppResult.Success)
        assertEquals(2, manager.sessions.value.size)
        assertEquals(1, manager.activeSessionIndex.value)
        assertEquals("Host B", manager.activeSession?.title)

        // Switch to session 0
        manager.selectSession(0)
        assertEquals(0, manager.activeSessionIndex.value)
        assertEquals("Host A", manager.activeSession?.title)

        // Send input and special key
        manager.sendInputToActive("uname -a\n")
        manager.sendSpecialKeyToActive(TerminalKey.CTRL_C)
        manager.clearActiveTerminal()

        // Close session
        manager.closeSession(0)
        assertEquals(1, manager.sessions.value.size)
        assertEquals(0, manager.activeSessionIndex.value)
        assertEquals("Host B", manager.activeSession?.title)

        // Snapshot & Reconnect test
        val snapshots = manager.getActiveSnapshots()
        assertEquals(1, snapshots.size)
        assertEquals("Host B", snapshots[0].first.label)

        val reconnectResult = manager.reconnectActiveSession()
        assertNotNull(reconnectResult)
        assertTrue(reconnectResult is AppResult.Success)
        assertEquals(1, manager.sessions.value.size)

        // Restore test
        val restoreResults = manager.restoreSavedSessions(snapshots)
        assertEquals(1, restoreResults.size)
        assertEquals(2, manager.sessions.value.size)

        // Close all
        manager.closeAllSessions()
        assertEquals(0, manager.sessions.value.size)
        testScope.cancel()
    }
}
