package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalSearchResults
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.transport.ITransport
import com.inscopelabs.abx.binbox.transport.LocalProcessTransport
import com.inscopelabs.abx.binbox.transport.SshTransport
import com.inscopelabs.abx.binbox.transport.TcpTransport
import com.inscopelabs.abx.binbox.transport.TransportListener
import com.inscopelabs.abx.binbox.transport.backend.WebSocketTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class TerminalKey {
    CTRL_C,
    CTRL_D,
    CTRL_Z,
    CTRL_L,
    CTRL_A,
    CTRL_E,
    ARROW_UP,
    ARROW_DOWN,
    ARROW_LEFT,
    ARROW_RIGHT,
    TAB,
    ESC,
    PAGE_UP,
    PAGE_DOWN,
    HOME,
    END
}

interface ShellSession {
    val id: String
    var title: String
    val hostLabel: String
    val state: StateFlow<SessionState>
    val lines: StateFlow<List<TerminalLine>>
    val rawLogText: String
    val cleanPlainText: String
        get() = lines.value.joinToString("\n") { it.rawText }
    val bytesReceived: StateFlow<Long> get() = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val bytesSent: StateFlow<Long> get() = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val isBracketedPasteMode: Boolean get() = false

    fun start()
    fun sendInput(text: String)
    fun sendSpecialKey(key: TerminalKey)
    fun sendFunctionKey(fIndex: Int) {
        sendRawBytes(TerminalKeyTranslator.translateFunctionKey(fIndex))
    }
    fun sendRawBytes(bytes: ByteArray)
    fun search(query: String, ignoreCase: Boolean = true): TerminalSearchResults = TerminalSearchResults(query)
    fun clear()
    fun reset() {}
    fun disconnect()
    fun updateTheme(theme: TerminalThemePreset)
    fun resize(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {}
}

// ----------------------------------------------------
// Base Transport Shell Session (Bridges ITransport to AnsiParser)
// ----------------------------------------------------
open class TransportShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override var title: String,
    override val hostLabel: String,
    val transport: ITransport,
    private var initialTheme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null,
    private val startupBanner: String? = null
) : ShellSession {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    override val state: StateFlow<SessionState> = transport.state
    override val bytesReceived: StateFlow<Long> = transport.bytesReceived
    override val bytesSent: StateFlow<Long> = transport.bytesSent

    private val ansiParser = AnsiParser(initialTheme, onBell)
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    override val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val logBuffer = StringBuilder()
    override val rawLogText: String
        get() = logBuffer.toString()

    override val isBracketedPasteMode: Boolean
        get() = ansiParser.isBracketedPasteMode

    init {
        transport.setListener(object : TransportListener {
            override fun onDataReceived(data: ByteArray) {
                val text = String(data, Charsets.UTF_8)
                appendOutput(text)
            }

            override fun onClosed(reason: String?) {
                if (!reason.isNullOrBlank()) {
                    appendOutput("\r\n\u001B[33m[$reason]\u001B[0m\r\n")
                }
            }

            override fun onError(message: String, cause: Throwable?) {
                appendOutput("\r\n\u001B[31m✖ Error: $message\u001B[0m\r\n")
            }
        })
    }

    override fun start() {
        startupBanner?.let { appendOutput(it) }
        scope.launch {
            transport.connect()
        }
    }

    override fun sendInput(text: String) {
        transport.sendData(text.toByteArray(Charsets.UTF_8))
    }

    override fun sendSpecialKey(key: TerminalKey) {
        transport.sendData(TerminalKeyTranslator.translateSpecialKey(key))
    }

    override fun sendRawBytes(bytes: ByteArray) {
        transport.sendData(bytes)
    }

    override fun search(query: String, ignoreCase: Boolean): TerminalSearchResults {
        return ansiParser.search(query, ignoreCase)
    }

    override fun clear() {
        ansiParser.clear()
        _lines.value = emptyList()
    }

    override fun reset() {
        ansiParser.reset()
        _lines.value = emptyList()
    }

    override fun disconnect() {
        transport.disconnect()
    }

    override fun updateTheme(theme: TerminalThemePreset) {
        ansiParser.updateTheme(theme)
        _lines.value = ansiParser.getLines()
    }

    override fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        transport.resize(cols, rows, widthPx, heightPx)
    }

    protected fun appendOutput(chunk: String) {
        logBuffer.append(chunk)
        ansiParser.feed(chunk)
        _lines.value = ansiParser.getLines()
    }
}

// ----------------------------------------------------
// SSH Shell Session (Delegates to SshTransport)
// ----------------------------------------------------
class SshShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override var title: String,
    override val hostLabel: String,
    host: String,
    port: Int = 22,
    username: String,
    password: String? = null,
    privateKey: String? = null,
    privateKeyPassphrase: String? = null,
    hostKeyRepository: com.jcraft.jsch.HostKeyRepository? = null,
    initialTheme: TerminalThemePreset,
    onBell: (() -> Unit)? = null
) : TransportShellSession(
    id = id,
    title = title,
    hostLabel = hostLabel,
    transport = SshTransport(
        host = host,
        port = port,
        username = username,
        password = password,
        privateKey = privateKey,
        privateKeyPassphrase = privateKeyPassphrase,
        hostKeyRepository = hostKeyRepository
    ),
    initialTheme = initialTheme,
    onBell = onBell,
    startupBanner = "Connecting to $username@$host:$port via SSH...\r\n"
)

// ----------------------------------------------------
// Local Shell Session (Android sh)
// ----------------------------------------------------
class LocalShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override var title: String = "Local Shell",
    override val hostLabel: String = "localhost",
    command: List<String>? = null,
    workingDir: File? = null,
    environment: Map<String, String>? = null,
    preferTermux: Boolean = true,
    transport: LocalProcessTransport = LocalProcessTransport(
        command = command,
        workingDir = workingDir,
        environment = environment,
        preferTermux = preferTermux
    ),
    initialTheme: TerminalThemePreset,
    onBell: (() -> Unit)? = null
) : TransportShellSession(
    id = id,
    title = title,
    hostLabel = hostLabel,
    transport = transport,
    initialTheme = initialTheme,
    onBell = onBell,
    startupBanner = "\u001B[1;36m┌──(binbox㉿localhost)-[~]\r\n└─$ \u001B[0m\u001B[32mStarting Local Shell (Termux / Android sh)...\u001B[0m\r\n"
)

// ----------------------------------------------------
// Telnet / Raw TCP Shell Session (Delegates to TcpTransport)
// ----------------------------------------------------
class TelnetShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override var title: String,
    override val hostLabel: String,
    host: String,
    port: Int = 23,
    initialTheme: TerminalThemePreset,
    onBell: (() -> Unit)? = null
) : TransportShellSession(
    id = id,
    title = title,
    hostLabel = hostLabel,
    transport = TcpTransport(host = host, port = port),
    initialTheme = initialTheme,
    onBell = onBell,
    startupBanner = "Connecting to $host:$port via TCP/Telnet...\r\n"
)

// ----------------------------------------------------
// WebSocket Backend Shell Session (Delegates to WebSocketTransport)
// ----------------------------------------------------
class WebSocketShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override var title: String = "WebSocket Session",
    override val hostLabel: String = "Backend Gateway",
    url: String,
    sessionId: String? = null,
    authToken: String? = null,
    customHeaders: Map<String, String> = emptyMap(),
    heartbeatIntervalSeconds: Long = 15L,
    structuredFraming: Boolean = true,
    transport: WebSocketTransport = WebSocketTransport(
        url = url,
        sessionId = sessionId,
        authToken = authToken,
        customHeaders = customHeaders,
        heartbeatIntervalSeconds = heartbeatIntervalSeconds,
        structuredFraming = structuredFraming
    ),
    initialTheme: TerminalThemePreset,
    onBell: (() -> Unit)? = null
) : TransportShellSession(
    id = id,
    title = title,
    hostLabel = hostLabel,
    transport = transport,
    initialTheme = initialTheme,
    onBell = onBell,
    startupBanner = "Connecting to $url via WebSocket Relay...\r\n"
)

