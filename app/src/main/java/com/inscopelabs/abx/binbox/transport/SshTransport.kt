package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
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
import java.util.Properties

/**
 * SSH transport (Phase 5 — SSH Provider), backed by the existing jsch
 * dependency. All JSch-specific code lives here and nowhere else — nothing
 * above [ITransport] (ShellSession, TerminalSessionManager, UI) references
 * JSch types directly.
 *
 * This is a direct extraction of the connection logic that previously lived
 * inline inside SshShellSession; behavior is intentionally unchanged
 * (StrictHostKeyChecking=no, same auth precedence, same PTY defaults) so this
 * refactor is a pure architectural move, not a functional one.
 *
 * Host-key verification, keepalive, and reconnection (also called out in the
 * upgrade plan's Phase 5 deliverables) are not yet implemented — tracked as
 * follow-up, not silently dropped.
 */
class SshTransport(
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val password: String? = null,
    private val privateKey: String? = null,
    private val privateKeyPassphrase: String? = null,
    private val ptyType: String = "xterm-256color",
    private val sessionConnectTimeoutMs: Int = 15000,
    private val channelConnectTimeoutMs: Int = 10000,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ITransport {

    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private var listener: TransportListener? = null

    private var jschSession: Session? = null
    private var channel: ChannelShell? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    override fun setListener(listener: TransportListener) {
        this.listener = listener
    }

    override suspend fun connect() {
        if (_state.value == SessionState.Connected || _state.value == SessionState.Connecting) return
        _state.value = SessionState.Connecting

        try {
            val jsch = JSch()

            if (!privateKey.isNullOrBlank()) {
                val keyBytes = privateKey.toByteArray(Charsets.UTF_8)
                val passBytes = privateKeyPassphrase?.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8)
                jsch.addIdentity("custom_id", keyBytes, null, passBytes)
            }

            val session = jsch.getSession(username, host, port)
            if (!password.isNullOrBlank()) {
                session.setPassword(password)
            }

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            config["PreferredAuthentications"] = "publickey,password,keyboard-interactive"
            session.setConfig(config)
            session.timeout = sessionConnectTimeoutMs

            session.connect()
            jschSession = session

            val shellChannel = session.openChannel("shell") as ChannelShell
            shellChannel.setPtyType(ptyType, 80, 24, 640, 480)
            channel = shellChannel

            outputStream = shellChannel.outputStream
            inputStream = shellChannel.inputStream

            shellChannel.connect(channelConnectTimeoutMs)
            _state.value = SessionState.Connected

            scope.launch {
                readLoop(shellChannel)
            }
        } catch (e: Exception) {
            val message = e.message ?: "SSH connection failed"
            _state.value = SessionState.Error(message)
            listener?.onError(message, e)
            cleanup()
        }
    }

    private suspend fun CoroutineScope.readLoop(shellChannel: ChannelShell) {
        val buffer = ByteArray(4096)
        try {
            while (isActive && shellChannel.isConnected) {
                val read = inputStream?.read(buffer) ?: -1
                if (read > 0) {
                    _bytesReceived.value += read
                    listener?.onDataReceived(buffer.copyOf(read))
                } else if (read == -1) {
                    break
                }
            }
            _state.value = SessionState.Disconnected
            listener?.onClosed("Connection closed by remote host")
        } catch (e: Exception) {
            val message = e.message ?: "Read failure"
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
                listener?.onError("Write error: ${e.message}", e)
            }
        }
    }

    override fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        scope.launch {
            try {
                channel?.setPtySize(cols, rows, widthPx.coerceAtLeast(640), heightPx.coerceAtLeast(480))
            } catch (e: Exception) {
                BinBoxLogger.w("SshTransport", "Resize failed: ${e.message}", e)
            }
        }
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
            channel?.disconnect()
            jschSession?.disconnect()
        } catch (e: Exception) {
            BinBoxLogger.w("SshTransport", "Cleanup encountered a non-fatal error: ${e.message}", e)
        } finally {
            outputStream = null
            inputStream = null
            channel = null
            jschSession = null
        }
    }
}
