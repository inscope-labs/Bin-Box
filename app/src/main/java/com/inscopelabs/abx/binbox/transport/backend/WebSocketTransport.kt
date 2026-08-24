package com.inscopelabs.abx.binbox.transport.backend

import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.TransportListener
import com.inscopelabs.abx.binbox.transport.backend.protocol.WsFrameCodec
import com.inscopelabs.abx.binbox.transport.backend.protocol.WsFrameType
import com.inscopelabs.abx.binbox.transport.backend.protocol.WsTerminalFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket-based transport implementation (Phase 7 — Backend / WebSocket Provider).
 *
 * Implements [ITransport] over an encrypted or plaintext WebSocket connection (wss:// or ws://),
 * managing structured framing (DATA, RESIZE, AUTH, PING, PONG, EXIT), heartbeats,
 * and byte metrics.
 */
class WebSocketTransport(
    val url: String,
    val sessionId: String? = null,
    val authToken: String? = null,
    val customHeaders: Map<String, String> = emptyMap(),
    val heartbeatIntervalSeconds: Long = 15L,
    val structuredFraming: Boolean = true,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSockets
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
    private val codec: WsFrameCodec = WsFrameCodec()
) : ITransport {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private var listener: TransportListener? = null
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val isAuthenticated = AtomicBoolean(authToken.isNullOrBlank())

    override fun setListener(listener: TransportListener) {
        this.listener = listener
    }

    override suspend fun connect() {
        if (_state.value == SessionState.Connected || _state.value == SessionState.Connecting) return

        _state.value = SessionState.Connecting

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "BinBox-Terminal-WebSocket/1.0")

        authToken?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
            requestBuilder.addHeader("X-Session-Token", it)
        }
        sessionId?.let {
            requestBuilder.addHeader("X-Session-ID", it)
        }
        customHeaders.forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }

        val request = requestBuilder.build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                if (!authToken.isNullOrBlank() && structuredFraming) {
                    // Perform initial auth handshake frame
                    val authFrame = WsTerminalFrame.auth(
                        sessionId = sessionId ?: "default",
                        token = authToken
                    )
                    ws.send(codec.encode(authFrame))
                    _state.value = SessionState.Connected
                } else {
                    _state.value = SessionState.Connected
                }
                startHeartbeat(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingText(text)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                val byteArr = bytes.toByteArray()
                _bytesReceived.value += byteArr.size
                listener?.onDataReceived(byteArr)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(code, reason)
                _state.value = SessionState.Disconnected
                stopHeartbeat()
                listener?.onClosed(reason.ifBlank { "WebSocket closed by remote host (code $code)" })
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _state.value = SessionState.Disconnected
                stopHeartbeat()
                listener?.onClosed(reason.ifBlank { "Connection closed" })
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                val msg = t.message ?: "WebSocket connection failed"
                _state.value = SessionState.Error(msg)
                stopHeartbeat()
                listener?.onError(msg, t)
            }
        })
    }

    private fun handleIncomingText(text: String) {
        if (!structuredFraming) {
            val bytes = text.toByteArray(Charsets.UTF_8)
            _bytesReceived.value += bytes.size
            listener?.onDataReceived(bytes)
            return
        }

        val frame = codec.decode(text)
        if (frame == null) {
            val bytes = text.toByteArray(Charsets.UTF_8)
            _bytesReceived.value += bytes.size
            listener?.onDataReceived(bytes)
            return
        }

        when (frame.frameType) {
            WsFrameType.AUTH_OK -> {
                isAuthenticated.set(true)
            }
            WsFrameType.DATA -> {
                val bytes = codec.extractPayloadBytes(frame)
                if (bytes.isNotEmpty()) {
                    _bytesReceived.value += bytes.size
                    listener?.onDataReceived(bytes)
                }
            }
            WsFrameType.PING -> {
                webSocket?.send(codec.encode(WsTerminalFrame.pong(sessionId)))
            }
            WsFrameType.PONG, WsFrameType.HEARTBEAT -> {
                // Heartbeat acknowledged
            }
            WsFrameType.EXIT -> {
                val exitCode = frame.exitCode ?: 0
                _state.value = SessionState.Disconnected
                listener?.onClosed("Remote session terminated (exit code $exitCode)")
            }
            WsFrameType.ERROR -> {
                val errorMsg = frame.errorMessage ?: "Unknown backend error"
                listener?.onError(errorMsg, null)
            }
            else -> {
                val bytes = codec.extractPayloadBytes(frame)
                if (bytes.isNotEmpty()) {
                    _bytesReceived.value += bytes.size
                    listener?.onDataReceived(bytes)
                }
            }
        }
    }

    override fun sendData(data: ByteArray) {
        val ws = webSocket ?: return
        if (_state.value != SessionState.Connected) return

        if (structuredFraming) {
            val textFrame = String(data, Charsets.UTF_8)
            val json = codec.encode(WsTerminalFrame.data(payload = textFrame, sessionId = sessionId))
            ws.send(json)
        } else {
            ws.send(ByteString.of(*data))
        }
        _bytesSent.value += data.size
    }

    override fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        val ws = webSocket ?: return
        if (_state.value != SessionState.Connected) return

        if (structuredFraming) {
            val resizeFrame = WsTerminalFrame.resize(
                cols = cols,
                rows = rows,
                widthPx = widthPx,
                heightPx = heightPx,
                sessionId = sessionId
            )
            ws.send(codec.encode(resizeFrame))
        }
    }

    override fun disconnect() {
        stopHeartbeat()
        try {
            webSocket?.close(1000, "User disconnected")
        } catch (_: Exception) {
            // Ignore
        } finally {
            webSocket = null
            _state.value = SessionState.Disconnected
            listener?.onClosed("Disconnected by user")
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        stopHeartbeat()
        if (heartbeatIntervalSeconds <= 0) return

        heartbeatJob = scope.launch {
            while (isActive) {
                delay(heartbeatIntervalSeconds * 1000L)
                if (_state.value == SessionState.Connected) {
                    try {
                        if (structuredFraming) {
                            ws.send(codec.encode(WsTerminalFrame.ping(sessionId)))
                        } else {
                            ws.send(ByteString.of(*("PING".toByteArray(Charsets.UTF_8))))
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
