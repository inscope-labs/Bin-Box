package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Raw TCP / Telnet transport (Phase 3 & 4 — Transport Framework).
 *
 * Connects directly to a socket endpoint and streams raw bytes bidirectionally.
 */
class TcpTransport(
    private val host: String,
    private val port: Int,
    private val connectTimeoutMs: Int = 10000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ITransport {

    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private var listener: TransportListener? = null

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    override fun setListener(listener: TransportListener) {
        this.listener = listener
    }

    override suspend fun connect() {
        if (_state.value == SessionState.Connected || _state.value == SessionState.Connecting) return
        _state.value = SessionState.Connecting

        try {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket = s
            outputStream = s.getOutputStream()
            inputStream = s.getInputStream()

            _state.value = SessionState.Connected

            scope.launch {
                readLoop(s)
            }
        } catch (e: Exception) {
            val message = e.message ?: "TCP connection to $host:$port failed"
            BinBoxLogger.e("TcpTransport", message, e)
            _state.value = SessionState.Error(message)
            listener?.onError(message, e)
            cleanup()
        }
    }

    private suspend fun CoroutineScope.readLoop(s: Socket) {
        val buffer = ByteArray(4096)
        try {
            while (isActive && s.isConnected && !s.isClosed) {
                val read = inputStream?.read(buffer) ?: -1
                if (read > 0) {
                    _bytesReceived.value += read
                    listener?.onDataReceived(buffer.copyOf(read))
                } else if (read == -1) {
                    break
                }
            }

            _state.value = SessionState.Disconnected
            listener?.onClosed("TCP connection closed by remote host")
        } catch (e: Exception) {
            val message = e.message ?: "TCP read error"
            _state.value = SessionState.Error(message)
            listener?.onError(message, e)
        } finally {
            cleanup()
        }
    }

    override fun sendData(data: ByteArray) {
        scope.launch {
            try {
                outputStream?.let {
                    it.write(data)
                    it.flush()
                    _bytesSent.value += data.size
                }
            } catch (e: Exception) {
                listener?.onError("TCP write error: ${e.message}", e)
            }
        }
    }

    override fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        // Raw TCP has no in-band resize by default
    }

    override fun disconnect() {
        scope.launch {
            cleanup()
            _state.value = SessionState.Disconnected
            listener?.onClosed("Disconnected by user")
        }
    }

    private fun cleanup() {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            BinBoxLogger.w("TcpTransport", "Cleanup warning: ${e.message}", e)
        } finally {
            outputStream = null
            inputStream = null
            socket = null
        }
    }
}
