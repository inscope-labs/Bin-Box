package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.terminal.engine.LocalShellSession
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.transport.local.TermuxDiscovery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocalTransportTest {

    @Test
    fun termuxDiscovery_returnsValidEnvironmentSnapshot() {
        val env = TermuxDiscovery.detectBestShell()
        assertNotNull(env)
        assertNotNull(env.shellPath)
        assertTrue(env.shellPath.isNotBlank())
        assertNotNull(env.environmentVariables)
        assertTrue(env.environmentVariables.containsKey("TERM"))
    }

    @Test
    fun termuxDiscovery_fallbackExecutableIsAlwaysAvailable() {
        val available = TermuxDiscovery.discoverAvailableShells()
        assertTrue(available.isNotEmpty())
    }

    @Test
    fun localProcessTransport_constructsSuccessfully() {
        val transport = LocalProcessTransport(
            command = listOf("/bin/sh"),
            workingDir = File("/tmp"),
            environment = mapOf("TEST_VAR" to "1"),
            preferTermux = false
        )
        assertNotNull(transport)
        assertEquals(SessionState.Disconnected, transport.state.value)
        assertEquals(0L, transport.bytesReceived.value)
        assertEquals(0L, transport.bytesSent.value)
    }

    @Test
    fun localShellSession_initializesWithTransport() = runTest {
        val session = LocalShellSession(
            title = "Test Local Shell",
            hostLabel = "localhost",
            initialTheme = TerminalThemes.MonokaiPro
        )

        assertEquals("Test Local Shell", session.title)
        assertEquals("localhost", session.hostLabel)
        assertNotNull(session.transport)
        assertEquals(SessionState.Disconnected, session.state.value)

        // Verifying special keys and raw bytes pass cleanly
        session.sendSpecialKey(TerminalKey.CTRL_C)
        session.sendFunctionKey(1)
        session.sendInput("echo hello\n")
        session.clear()
        assertEquals(0, session.lines.value.size)
        session.disconnect()
        assertEquals(SessionState.Disconnected, session.state.value)
    }
}

