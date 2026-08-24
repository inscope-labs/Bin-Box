package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.domain.model.VmState
import com.inscopelabs.abx.binbox.domain.model.VmStatus
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.engine.WebSocketShellSession
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.transport.backend.WebSocketTransport
import com.inscopelabs.abx.binbox.transport.backend.api.BinBoxBackendClient
import com.inscopelabs.abx.binbox.transport.backend.models.BackendDiscoveryResponse
import com.inscopelabs.abx.binbox.transport.backend.models.ProvisionSessionRequest
import com.inscopelabs.abx.binbox.transport.backend.models.VmInstanceDto
import com.inscopelabs.abx.binbox.transport.backend.protocol.WsFrameCodec
import com.inscopelabs.abx.binbox.transport.backend.protocol.WsFrameType
import com.inscopelabs.abx.binbox.transport.backend.protocol.WsTerminalFrame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackendTransportTest {

    private val codec = WsFrameCodec()

    @Test
    fun wsFrameCodec_encodesAndDecodesDataFrameCorrectly() {
        val frame = WsTerminalFrame.data("ls -la\n", sessionId = "sess-1")
        val json = codec.encode(frame)
        assertTrue(json.contains("DATA"))
        assertTrue(json.contains("ls -la"))

        val decoded = codec.decode(json)
        assertNotNull(decoded)
        assertEquals(WsFrameType.DATA, decoded!!.frameType)
        assertEquals("ls -la\n", decoded.payload)
        assertEquals("sess-1", decoded.sessionId)
    }

    @Test
    fun wsFrameCodec_encodesAndDecodesResizeFrame() {
        val frame = WsTerminalFrame.resize(cols = 120, rows = 40, widthPx = 1080, heightPx = 1920, sessionId = "sess-2")
        val json = codec.encode(frame)
        assertTrue(json.contains("RESIZE"))

        val decoded = codec.decode(json)
        assertNotNull(decoded)
        assertEquals(WsFrameType.RESIZE, decoded!!.frameType)
        assertEquals(120, decoded.cols)
        assertEquals(40, decoded.rows)
    }

    @Test
    fun wsFrameCodec_encodesAndDecodesBinaryBase64Payload() {
        val rawBytes = byteArrayOf(0x1B, 0x5B, 0x48, 0x1B, 0x5B, 0x32, 0x4A) // ANSI clear screen
        val json = codec.encodeBytesToDataFrame(rawBytes, sessionId = "sess-raw")
        val decoded = codec.decode(json)

        assertNotNull(decoded)
        assertTrue(decoded!!.isBase64)
        val extractedBytes = codec.extractPayloadBytes(decoded)
        assertEquals(rawBytes.size, extractedBytes.size)
        assertEquals(rawBytes[0], extractedBytes[0])
        assertEquals(rawBytes[rawBytes.size - 1], extractedBytes[extractedBytes.size - 1])
    }

    @Test
    fun backendClient_mockDiscoveryAndInstanceList() = runTest {
        val client = BinBoxBackendClient(enableMockFallback = true)

        val discovery = client.getDiscovery()
        assertTrue(discovery.isSuccess)
        val meta = discovery.getOrNull()
        assertNotNull(meta)
        assertTrue(meta!!.capabilities.contains("ws_pty"))

        val instances = client.listInstances()
        assertTrue(instances.isSuccess)
        val vmList = instances.getOrNull()
        assertNotNull(vmList)
        assertTrue(vmList!!.isNotEmpty())
        assertEquals(VmState.RUNNING, vmList[0].state)
    }

    @Test
    fun backendClient_sessionProvisioning() = runTest {
        val client = BinBoxBackendClient(enableMockFallback = true)
        val provisionReq = ProvisionSessionRequest(
            targetHost = "129.153.64.102",
            port = 22,
            protocol = "SSH",
            username = "ubuntu"
        )
        val result = client.provisionSession(provisionReq)
        assertTrue(result.isSuccess)
        val sessionResp = result.getOrNull()
        assertNotNull(sessionResp)
        assertTrue(sessionResp!!.sessionId.isNotBlank())
        assertTrue(sessionResp.websocketUrl.startsWith("ws://") || sessionResp.websocketUrl.startsWith("wss://"))
    }

    @Test
    fun webSocketTransport_constructsAndMaintainsInitialState() {
        val transport = WebSocketTransport(
            url = "wss://gateway.abx.internal/ws/terminal",
            sessionId = "test-session",
            authToken = "test-token-123"
        )
        assertEquals(SessionState.Disconnected, transport.state.value)
        assertEquals(0L, transport.bytesReceived.value)
        assertEquals(0L, transport.bytesSent.value)
    }

    @Test
    fun webSocketShellSession_initializesWithThemeAndKeys() = runTest {
        val session = WebSocketShellSession(
            title = "Test WS Session",
            hostLabel = "gateway.abx.internal",
            url = "wss://gateway.abx.internal/ws/terminal/abc",
            authToken = "test_token",
            initialTheme = TerminalThemes.MonokaiPro
        )

        assertEquals("Test WS Session", session.title)
        assertEquals("gateway.abx.internal", session.hostLabel)
        assertNotNull(session.transport)
        assertEquals(SessionState.Disconnected, session.state.value)

        session.sendSpecialKey(TerminalKey.CTRL_C)
        session.sendFunctionKey(1)
        session.sendInput("uptime\n")
        session.disconnect()
        assertEquals(SessionState.Disconnected, session.state.value)
    }
}
