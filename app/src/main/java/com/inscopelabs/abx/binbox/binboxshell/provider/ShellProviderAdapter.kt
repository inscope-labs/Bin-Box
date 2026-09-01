package com.inscopelabs.abx.binbox.binboxshell.provider

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.TransportListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Bridges [LocalShellProvider] execution with the unified [ITransport] interface.
 */
class ShellProviderAdapter(
    private val provider: LocalShellProvider,
    private val command: List<String>? = null,
    private val workingDir: File? = null,
    private val environment: Map<String, String>? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : ITransport {

    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private var listener: TransportListener? = null
    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    override fun setListener(listener: TransportListener) {
        this.listener = listener
    }

    override suspend fun connect() {
        if (_state.value is SessionState.Connected || _state.value is SessionState.Connecting) return
        _state.value = SessionState.Connecting

        try {
            val p = provider.createShellProcess(command, workingDir, environment)
            process = p
            outputStream = p.outputStream
            inputStream = p.inputStream

            _state.value = SessionState.Connected
            BinBoxLogger.i(TAG, "Local shell transport connected")

            scope.launch {
                readLoop(p)
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to launch local shell"
            BinBoxLogger.e(TAG, "Connect failed: $errorMsg", e)
            _state.value = SessionState.Error(errorMsg)
            listener?.onError(errorMsg, e)
            cleanup()
        }
    }

    private fun CoroutineScope.readLoop(p: Process) {
        val buffer = ByteArray(4096)
        try {
            while (isActive) {
                val read = inputStream?.read(buffer) ?: -1
                if (read > 0) {
                    _bytesReceived.value += read
                    listener?.onDataReceived(buffer.copyOf(read))
                } else if (read == -1) {
                    break
                }
            }
            val exitCode = try { p.waitFor() } catch (_: Exception) { p.exitValue() }
            _state.value = SessionState.Disconnected
            listener?.onClosed("Process exited with code $exitCode")
        } catch (e: Exception) {
            val msg = e.message ?: "Process read error"
            _state.value = SessionState.Error(msg)
            listener?.onError(msg, e)
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
                listener?.onError("Write failed: ${e.message}", e)
            }
        }
    }

    override fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        BinBoxLogger.d(TAG, "PTY resize requested: ${cols}x$rows")
    }

    override fun disconnect() {
        scope.launch {
            cleanup()
            _state.value = SessionState.Disconnected
            listener?.onClosed("Disconnected")
        }
    }

    private fun cleanup() {
        try {
            outputStream?.close()
            inputStream?.close()
            process?.destroy()
        } catch (e: Exception) {
            BinBoxLogger.w(TAG, "Cleanup exception: ${e.message}", e)
        } finally {
            outputStream = null
            inputStream = null
            process = null
        }
    }

    companion object {
        private const val TAG = "ShellProviderAdapter"
    }
}
