package com.inscopelabs.abx.binbox.terminal.engine

import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
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
    val title: String
    val hostLabel: String
    val state: StateFlow<SessionState>
    val lines: StateFlow<List<TerminalLine>>
    val rawLogText: String
    val bytesReceived: StateFlow<Long> get() = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val bytesSent: StateFlow<Long> get() = kotlinx.coroutines.flow.MutableStateFlow(0L)

    fun start()
    fun sendInput(text: String)
    fun sendSpecialKey(key: TerminalKey)
    fun sendRawBytes(bytes: ByteArray)
    fun clear()
    fun disconnect()
    fun updateTheme(theme: TerminalThemePreset)
    fun resize(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {}
}

// ----------------------------------------------------
// SSH Shell Session — delegates to SshTransport (Phase 3/5)
// ----------------------------------------------------
class SshShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String,
    override val hostLabel: String,
    host: String,
    port: Int = 22,
    username: String,
    password: String? = null,
    privateKey: String? = null,
    privateKeyPassphrase: String? = null,
    private var initialTheme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null
) : ShellSession {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val ansiParser = AnsiParser(initialTheme, onBell)
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    override val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private val logBuffer = StringBuilder()
    override val rawLogText: String
        get() = logBuffer.toString()

    // All JSch-specific connection logic now lives inside SshTransport.
    // This class knows only about the ITransport contract.
    private val transport: com.inscopelabs.abx.binbox.transport.ITransport =
        com.inscopelabs.abx.binbox.transport.SshTransport(
            host = host,
            port = port,
            username = username,
            password = password,
            privateKey = privateKey,
            privateKeyPassphrase = privateKeyPassphrase
        )

    init {
        transport.setListener(object : com.inscopelabs.abx.binbox.transport.TransportListener {
            override fun onDataReceived(data: ByteArray) {
                appendOutput(String(data, Charsets.UTF_8))
            }

            override fun onClosed(reason: String?) {
                appendBanner("\r\n\u001B[33m[${reason ?: "Connection closed"}]\u001B[0m\r\n")
            }

            override fun onError(message: String, cause: Throwable?) {
                appendBanner("\r\n\u001B[31m✖ SSH Error: $message\u001B[0m\r\n")
            }
        })

        scope.launch { transport.state.collect { _state.value = it } }
        scope.launch { transport.bytesReceived.collect { _bytesReceived.value = it } }
        scope.launch { transport.bytesSent.collect { _bytesSent.value = it } }
    }

    override fun start() {
        if (_state.value == SessionState.Connected || _state.value == SessionState.Connecting) return
        appendBanner("Connecting to $hostLabel via SSH...\r\n")
        scope.launch {
            transport.connect()
            if (transport.state.value == SessionState.Connected) {
                appendBanner("\u001B[32m✔ Connected to $hostLabel\u001B[0m\r\n\r\n")
            }
        }
    }

    override fun resize(cols: Int, rows: Int, widthPx: Int, heightPx: Int) {
        transport.resize(cols, rows, widthPx, heightPx)
    }

    override fun sendInput(text: String) {
        transport.sendData(text.toByteArray(Charsets.UTF_8))
    }

    override fun sendSpecialKey(key: TerminalKey) {
        val bytes = when (key) {
            TerminalKey.CTRL_C -> byteArrayOf(0x03)
            TerminalKey.CTRL_D -> byteArrayOf(0x04)
            TerminalKey.CTRL_Z -> byteArrayOf(0x1A)
            TerminalKey.CTRL_L -> byteArrayOf(0x0C)
            TerminalKey.CTRL_A -> byteArrayOf(0x01)
            TerminalKey.CTRL_E -> byteArrayOf(0x05)
            TerminalKey.ARROW_UP -> "\u001B[A".toByteArray()
            TerminalKey.ARROW_DOWN -> "\u001B[B".toByteArray()
            TerminalKey.ARROW_RIGHT -> "\u001B[C".toByteArray()
            TerminalKey.ARROW_LEFT -> "\u001B[D".toByteArray()
            TerminalKey.TAB -> "\t".toByteArray()
            TerminalKey.ESC -> "\u001B".toByteArray()
            TerminalKey.PAGE_UP -> "\u001B[5~".toByteArray()
            TerminalKey.PAGE_DOWN -> "\u001B[6~".toByteArray()
            TerminalKey.HOME -> "\u001B[H".toByteArray()
            TerminalKey.END -> "\u001B[F".toByteArray()
        }
        sendRawBytes(bytes)
    }

    override fun sendRawBytes(bytes: ByteArray) {
        transport.sendData(bytes)
    }

    override fun clear() {
        ansiParser.clear()
        _lines.value = emptyList()
    }

    override fun disconnect() {
        transport.disconnect()
    }

    override fun updateTheme(theme: TerminalThemePreset) {
        ansiParser.updateTheme(theme)
        _lines.value = ansiParser.getLines()
    }

    private fun appendBanner(text: String) {
        appendOutput(text)
    }

    private fun appendOutput(chunk: String) {
        logBuffer.append(chunk)
        ansiParser.feed(chunk)
        _lines.value = ansiParser.getLines()
    }
}

// ----------------------------------------------------
// Local Shell Session (Android sh)
// ----------------------------------------------------
class LocalShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "Local Shell",
    override val hostLabel: String = "localhost",
    private var initialTheme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null
) : ShellSession {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val ansiParser = AnsiParser(initialTheme, onBell)
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    override val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private val logBuffer = StringBuilder()
    override val rawLogText: String
        get() = logBuffer.toString()

    private var process: Process? = null
    private var processOut: OutputStream? = null
    private var processIn: InputStream? = null
    private var processErr: InputStream? = null

    override fun start() {
        if (_state.value == SessionState.Connected) return
        _state.value = SessionState.Connecting

        appendOutput("\u001B[1;36m┌──(binbox㉿localhost)-[~]\r\n└─$ \u001B[0m\u001B[32mStarting Local Shell (Android /system/bin/sh)...\u001B[0m\r\n")

        scope.launch {
            try {
                val shellPath = if (java.io.File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"
                val pb = ProcessBuilder(shellPath)
                pb.environment()["TERM"] = "xterm-256color"
                pb.environment()["PS1"] = "\\u@\\h:\\w\\$ "
                pb.redirectErrorStream(true)

                val p = pb.start()
                process = p
                processOut = p.outputStream
                processIn = p.inputStream

                _state.value = SessionState.Connected
                appendOutput("\u001B[1;32m✔ Local process initialized. Type 'help' or standard POSIX commands.\u001B[0m\r\n\r\n")

                // Prompt user
                sendInput("echo 'Welcome to Bin Box Local Shell. OS: Android (Linux Kernel '$(uname -r)')'\n")

                val buffer = ByteArray(2048)
                while (isActive) {
                    val read = processIn?.read(buffer) ?: -1
                    if (read > 0) {
                        _bytesReceived.value += read
                        val text = String(buffer, 0, read, Charsets.UTF_8)
                        appendOutput(text)
                    } else if (read == -1) {
                        break
                    }
                }

                _state.value = SessionState.Disconnected
                appendOutput("\r\n\u001B[33m[Local shell process exited with code ${p.exitValue()}]\u001B[0m\r\n")
            } catch (e: Exception) {
                _state.value = SessionState.Error(e.message ?: "Failed to start local shell")
                appendOutput("\r\n\u001B[31m✖ Local Shell Error: ${e.message}\u001B[0m\r\n")
            }
        }
    }

    override fun sendInput(text: String) {
        scope.launch {
            try {
                processOut?.let {
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    it.write(bytes)
                    it.flush()
                    _bytesSent.value += bytes.size
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun sendSpecialKey(key: TerminalKey) {
        when (key) {
            TerminalKey.CTRL_C -> sendRawBytes(byteArrayOf(0x03))
            TerminalKey.CTRL_D -> sendRawBytes(byteArrayOf(0x04))
            TerminalKey.CTRL_Z -> sendRawBytes(byteArrayOf(0x1A))
            TerminalKey.CTRL_L -> sendRawBytes(byteArrayOf(0x0C))
            TerminalKey.TAB -> sendRawBytes("\t".toByteArray())
            TerminalKey.ARROW_UP -> sendRawBytes("\u001B[A".toByteArray())
            TerminalKey.ARROW_DOWN -> sendRawBytes("\u001B[B".toByteArray())
            TerminalKey.ARROW_LEFT -> sendRawBytes("\u001B[D".toByteArray())
            TerminalKey.ARROW_RIGHT -> sendRawBytes("\u001B[C".toByteArray())
            TerminalKey.ESC -> sendRawBytes("\u001B".toByteArray())
            TerminalKey.HOME -> sendRawBytes("\u001B[H".toByteArray())
            TerminalKey.END -> sendRawBytes("\u001B[F".toByteArray())
            else -> {}
        }
    }

    override fun sendRawBytes(bytes: ByteArray) {
        scope.launch {
            try {
                processOut?.let {
                    it.write(bytes)
                    it.flush()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun clear() {
        ansiParser.clear()
        _lines.value = emptyList()
    }

    override fun disconnect() {
        try {
            process?.destroy()
            processOut?.close()
            processIn?.close()
        } catch (e: Exception) {
            // Ignore
        }
        _state.value = SessionState.Disconnected
        appendOutput("\r\n\u001B[33m[Local shell terminated]\u001B[0m\r\n")
    }

    override fun updateTheme(theme: TerminalThemePreset) {
        ansiParser.updateTheme(theme)
        _lines.value = ansiParser.getLines()
    }

    private fun appendOutput(chunk: String) {
        logBuffer.append(chunk)
        ansiParser.feed(chunk)
        _lines.value = ansiParser.getLines()
    }
}

// ----------------------------------------------------
// Telnet / Raw TCP Shell Session
// ----------------------------------------------------
class TelnetShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String,
    override val hostLabel: String,
    private val host: String,
    private val port: Int = 23,
    private var initialTheme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null
) : ShellSession {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val ansiParser = AnsiParser(initialTheme, onBell)
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    override val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val logBuffer = StringBuilder()
    override val rawLogText: String
        get() = logBuffer.toString()

    private var socket: Socket? = null
    private var outStream: OutputStream? = null
    private var inStream: InputStream? = null

    override fun start() {
        if (_state.value == SessionState.Connected || _state.value == SessionState.Connecting) return
        _state.value = SessionState.Connecting

        appendOutput("Connecting to $host:$port via Telnet/TCP...\r\n")

        scope.launch {
            try {
                val s = Socket(host, port)
                s.soTimeout = 0
                socket = s
                outStream = s.getOutputStream()
                inStream = s.getInputStream()

                _state.value = SessionState.Connected
                appendOutput("\u001B[32m✔ Connected to Telnet host $host:$port\u001B[0m\r\n\r\n")

                val buffer = ByteArray(2048)
                while (isActive && s.isConnected && !s.isClosed) {
                    val read = inStream?.read(buffer) ?: -1
                    if (read > 0) {
                        // Filter telnet IAC sequences if standard telnet (IAC=255)
                        val text = String(buffer, 0, read, Charsets.UTF_8)
                        appendOutput(text)
                    } else if (read == -1) {
                        break
                    }
                }

                _state.value = SessionState.Disconnected
                appendOutput("\r\n\u001B[33m[Telnet connection closed]\u001B[0m\r\n")
            } catch (e: Exception) {
                _state.value = SessionState.Error(e.message ?: "Telnet failed")
                appendOutput("\r\n\u001B[31m✖ Telnet Error: ${e.message}\u001B[0m\r\n")
            } finally {
                disconnect()
            }
        }
    }

    override fun sendInput(text: String) {
        scope.launch {
            try {
                outStream?.let {
                    it.write(text.toByteArray(Charsets.UTF_8))
                    it.flush()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun sendSpecialKey(key: TerminalKey) {
        when (key) {
            TerminalKey.CTRL_C -> sendRawBytes(byteArrayOf(0x03))
            TerminalKey.CTRL_D -> sendRawBytes(byteArrayOf(0x04))
            TerminalKey.CTRL_Z -> sendRawBytes(byteArrayOf(0x1A))
            TerminalKey.CTRL_L -> sendRawBytes(byteArrayOf(0x0C))
            TerminalKey.TAB -> sendRawBytes("\t".toByteArray())
            TerminalKey.ARROW_UP -> sendRawBytes("\u001B[A".toByteArray())
            TerminalKey.ARROW_DOWN -> sendRawBytes("\u001B[B".toByteArray())
            TerminalKey.ARROW_LEFT -> sendRawBytes("\u001B[D".toByteArray())
            TerminalKey.ARROW_RIGHT -> sendRawBytes("\u001B[C".toByteArray())
            TerminalKey.ESC -> sendRawBytes("\u001B".toByteArray())
            TerminalKey.HOME -> sendRawBytes("\u001B[H".toByteArray())
            TerminalKey.END -> sendRawBytes("\u001B[F".toByteArray())
            else -> {}
        }
    }

    override fun sendRawBytes(bytes: ByteArray) {
        scope.launch {
            try {
                outStream?.let {
                    it.write(bytes)
                    it.flush()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun clear() {
        ansiParser.clear()
        _lines.value = emptyList()
    }

    override fun disconnect() {
        try {
            outStream?.close()
            inStream?.close()
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        _state.value = SessionState.Disconnected
    }

    override fun updateTheme(theme: TerminalThemePreset) {
        ansiParser.updateTheme(theme)
        _lines.value = ansiParser.getLines()
    }

    private fun appendOutput(chunk: String) {
        logBuffer.append(chunk)
        ansiParser.feed(chunk)
        _lines.value = ansiParser.getLines()
    }
}

// ----------------------------------------------------
// Interactive Demo / Sandbox Linux Host Shell Session
// ----------------------------------------------------
class SandboxDemoShellSession(
    override val id: String = UUID.randomUUID().toString(),
    override val title: String = "Cloud Linux Demo",
    override val hostLabel: String = "vps-demo.binbox.io",
    private var initialTheme: TerminalThemePreset,
    private val onBell: (() -> Unit)? = null
) : ShellSession {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _state = MutableStateFlow<SessionState>(SessionState.Disconnected)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private val ansiParser = AnsiParser(initialTheme, onBell)
    private val _lines = MutableStateFlow<List<TerminalLine>>(emptyList())
    override val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val _bytesReceived = MutableStateFlow(0L)
    override val bytesReceived: StateFlow<Long> = _bytesReceived.asStateFlow()

    private val _bytesSent = MutableStateFlow(0L)
    override val bytesSent: StateFlow<Long> = _bytesSent.asStateFlow()

    private val logBuffer = StringBuilder()
    override val rawLogText: String
        get() = logBuffer.toString()

    private var currentDir = "/var/www/binbox"
    private var hostname = "binbox-node-01"
    private var username = "root"

    override fun start() {
        if (_state.value == SessionState.Connected) return
        _state.value = SessionState.Connecting

        appendOutput("\u001B[33mConnecting to $username@$hostLabel (SSH Sandbox Simulation)...\u001B[0m\r\n")

        scope.launch {
            delay(400)
            _state.value = SessionState.Connected
            appendOutput("\u001B[32m✔ Authenticated with ED25519 key (fingerprint: SHA256:d8f1e09c2a...)\u001B[0m\r\n")
            appendOutput("\u001B[36mLinux binbox-node-01 6.8.0-45-generic #45-Ubuntu SMP PREEMPT_DYNAMIC x86_64\u001B[0m\r\n\r\n")
            appendOutput(getFastFetchBanner())
            appendPrompt()
        }
    }

    override fun sendInput(text: String) {
        val cleanCmd = text.trim()
        appendOutput("$cleanCmd\r\n")

        scope.launch {
            executeMockCommand(cleanCmd)
            appendPrompt()
        }
    }

    private fun executeMockCommand(cmd: String) {
        val parts = cmd.split(" ").filter { it.isNotBlank() }
        val root = parts.firstOrNull()?.lowercase() ?: ""

        when (root) {
            "" -> { /* Just enter */ }
            "clear" -> {
                clear()
            }
            "help" -> {
                appendOutput(
                    "\u001B[1;36m=== Bin Box Sandbox Shell Commands ===\u001B[0m\r\n" +
                    " • \u001B[33mfastfetch\u001B[0m / \u001B[33mneofetch\u001B[0m : Display rich system telemetry art\r\n" +
                    " • \u001B[33mbtop\u001B[0m / \u001B[33mhtop\u001B[0m / \u001B[33mtop\u001B[0m : Interactive process monitor\r\n" +
                    " • \u001B[33mdocker ps\u001B[0m / \u001B[33mdocker stats\u001B[0m : Container cluster status\r\n" +
                    " • \u001B[33mdf -h\u001B[0m / \u001B[33mfree -m\u001B[0m / \u001B[33muptime\u001B[0m : Resource statistics\r\n" +
                    " • \u001B[33mls -la\u001B[0m / \u001B[33mpwd\u001B[0m / \u001B[33mcd\u001B[0m : File system navigation\r\n" +
                    " • \u001B[33mcat [file]\u001B[0m : Read server configs (e.g. cat nginx.conf)\r\n" +
                    " • \u001B[33mping [host]\u001B[0m / \u001B[33mcurl [url]\u001B[0m : Network diagnostics\r\n" +
                    " • \u001B[33msystemctl status\u001B[0m : Linux systemd services\r\n" +
                    " • \u001B[33muname -a\u001B[0m / \u001B[33mwhoami\u001B[0m / \u001B[33mdate\u001B[0m : Host identification\r\n\r\n"
                )
            }
            "fastfetch", "neofetch" -> {
                appendOutput(getFastFetchBanner())
            }
            "btop", "htop", "top" -> {
                appendOutput(
                    "\u001B[1;37;44m CPU [||||||||||||||||||||                38.4%]   Tasks: 184, 2 running \u001B[0m\r\n" +
                    "\u001B[1;37;42m Mem [||||||||||||||||||||||||            4.2G/16G]   Swap: 0B/4G           \u001B[0m\r\n" +
                    "\u001B[1;37;45m Disk [|||||||||||                        42.1G/120G] Load: 0.42 0.55 0.61 \u001B[0m\r\n" +
                    "\u001B[1;33m  PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND\u001B[0m\r\n" +
                    " 1042 root      20   0 1829304 429184  38292 S  14.2   2.6  14:22.18 dockerd\r\n" +
                    " 1892 nginx     20   0  128492  18492   8920 S   8.5   0.1   8:10.04 nginx: worker\r\n" +
                    " 2041 postgres  20   0  894820 184920  52194 S   6.2   1.1  45:12.30 postgres\r\n" +
                    " 3192 node      20   0  482910  94820  24910 S   4.8   0.6   2:44.91 node api.js\r\n" +
                    " 4012 redis     20   0   48291  12480   4910 S   1.2   0.1   1:18.42 redis-server\r\n" +
                    " 5891 root      20   0   14920   3910   2840 R   0.9   0.0   0:00.18 htop\r\n\r\n"
                )
            }
            "docker" -> {
                if (parts.contains("ps")) {
                    appendOutput(
                        "\u001B[1;34mCONTAINER ID   IMAGE                 COMMAND                  CREATED         STATUS         PORTS                    NAMES\u001B[0m\r\n" +
                        "8f9a2b1c4e5d   nginx:alpine          \"/docker-entrypoint.…\"   3 days ago      Up 3 days      0.0.0.0:80->80/tcp       frontend-proxy\r\n" +
                        "3a7c9e1b2f4d   postgres:16-alpine    \"docker-entrypoint.s…\"   2 weeks ago     Up 2 weeks     0.0.0.0:5432->5432/tcp   postgres-master\r\n" +
                        "9c1b4f8e2a3d   redis:7-alpine        \"docker-entrypoint.s…\"   2 weeks ago     Up 2 weeks     0.0.0.0:6379->6379/tcp   redis-cache\r\n" +
                        "5e2a8d1c9b3f   binbox/backend:v2.4   \"node server.js\"         5 hours ago     Up 5 hours     0.0.0.0:3000->3000/tcp   binbox-api\r\n\r\n"
                    )
                } else {
                    appendOutput("\u001B[33mDocker version 27.1.1, build 6312585\r\nUsage: docker [OPTIONS] COMMAND (e.g. docker ps)\u001B[0m\r\n")
                }
            }
            "ls" -> {
                appendOutput(
                    "\u001B[1;34mdrwxr-xr-x\u001B[0m  8 root root 4096 Aug 21 03:40 \u001B[1;34m.\u001B[0m\r\n" +
                    "\u001B[1;34mdrwxr-xr-x\u001B[0m 14 root root 4096 Aug 10 12:00 \u001B[1;34m..\u001B[0m\r\n" +
                    "\u001B[1;32m-rwxr-xr-x\u001B[0m  1 root root  842 Aug 21 02:15 \u001B[1;32mdeploy.sh\u001B[0m\r\n" +
                    "-rw-r--r--  1 root root 1284 Aug 20 18:22 docker-compose.yml\r\n" +
                    "-rw-r--r--  1 root root  491 Aug 19 09:12 nginx.conf\r\n" +
                    "-rw-r--r--  1 root root  384 Aug 21 00:01 .env\r\n" +
                    "\u001B[1;34mdrwxr-xr-x\u001B[0m  4 root root 4096 Aug 21 01:30 \u001B[1;34mcerts\u001B[0m\r\n" +
                    "\u001B[1;34mdrwxr-xr-x\u001B[0m  6 root root 4096 Aug 21 03:00 \u001B[1;34mlogs\u001B[0m\r\n" +
                    "-rw-r--r--  1 root root 4892 Aug 21 03:39 README.md\r\n\r\n"
                )
            }
            "cat" -> {
                val target = parts.getOrNull(1) ?: ""
                when {
                    target.contains("nginx") -> {
                        appendOutput(
                            "\u001B[36mserver {\r\n" +
                            "    listen 80;\r\n" +
                            "    server_name binbox.io;\r\n" +
                            "    location / {\r\n" +
                            "        proxy_pass http://127.0.0.1:3000;\r\n" +
                            "        proxy_set_header Host \$host;\r\n" +
                            "        proxy_set_header X-Real-IP \$remote_addr;\r\n" +
                            "    }\r\n" +
                            "}\u001B[0m\r\n\r\n"
                        )
                    }
                    target.contains("deploy") -> {
                        appendOutput(
                            "\u001B[32m#!/bin/bash\r\n" +
                            "set -e\r\n" +
                            "echo '==> Deploying Bin Box Node v2.4...'\r\n" +
                            "git pull origin main\r\n" +
                            "docker-compose pull && docker-compose up -d --remove-orphans\r\n" +
                            "echo '==> Deployment complete!'\u001B[0m\r\n\r\n"
                        )
                    }
                    target.contains("README") -> {
                        appendOutput(
                            "# Bin Box Remote Host Instance\r\n" +
                            "High performance Linux server console with SSH2, Docker, and terminal emulator.\r\n\r\n"
                        )
                    }
                    else -> {
                        appendOutput("cat: $target: No such file or directory (try 'cat nginx.conf' or 'cat deploy.sh')\r\n")
                    }
                }
            }
            "pwd" -> appendOutput("$currentDir\r\n")
            "cd" -> {
                val dir = parts.getOrNull(1) ?: "~"
                currentDir = if (dir == "~" || dir == "") "/root" else dir
            }
            "uname" -> {
                appendOutput("Linux $hostname 6.8.0-45-generic #45-Ubuntu SMP PREEMPT_DYNAMIC x86_64 GNU/Linux\r\n")
            }
            "whoami" -> appendOutput("$username\r\n")
            "uptime" -> {
                appendOutput(" 04:00:22 up 48 days, 14:12,  2 users,  load average: 0.38, 0.45, 0.52\r\n")
            }
            "free" -> {
                appendOutput(
                    "               total        used        free      shared  buff/cache   available\r\n" +
                    "Mem:        16384Mi     4210Mi      8420Mi       210Mi      3754Mi     11964Mi\r\n" +
                    "Swap:        4096Mi        0Mi      4096Mi\r\n\r\n"
                )
            }
            "df" -> {
                appendOutput(
                    "Filesystem      Size  Used Avail Use% Mounted on\r\n" +
                    "/dev/sda1       120G   42G   73G  37% /\r\n" +
                    "tmpfs           8.0G     0  8.0G   0% /dev/shm\r\n" +
                    "/dev/sdb1       500G  180G  295G  38% /data\r\n\r\n"
                )
            }
            "ping" -> {
                val target = parts.getOrNull(1) ?: "1.1.1.1"
                appendOutput(
                    "PING $target ($target) 56(84) bytes of data.\r\n" +
                    "64 bytes from $target: icmp_seq=1 ttl=58 time=12.4 ms\r\n" +
                    "64 bytes from $target: icmp_seq=2 ttl=58 time=11.8 ms\r\n" +
                    "64 bytes from $target: icmp_seq=3 ttl=58 time=12.1 ms\r\n" +
                    "--- $target ping statistics ---\r\n" +
                    "3 packets transmitted, 3 received, 0% packet loss, time 2004ms\r\n" +
                    "rtt min/avg/max = 11.8/12.1/12.4 ms\r\n\r\n"
                )
            }
            "date" -> {
                val df = SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US)
                appendOutput("${df.format(Date())}\r\n")
            }
            "systemctl" -> {
                appendOutput(
                    "\u001B[32m●\u001B[0m docker.service - Docker Application Container Engine\r\n" +
                    "     Loaded: loaded (/lib/systemd/system/docker.service; enabled; preset: enabled)\r\n" +
                    "     Active: \u001B[1;32mactive (running)\u001B[0m since Sat 2026-08-01 02:14:10 UTC; 20 days ago\r\n" +
                    "   Main PID: 1042 (dockerd)\r\n" +
                    "      Tasks: 64\r\n" +
                    "     Memory: 419.1M\r\n" +
                    "        CPU: 1h 42min 18s\r\n\r\n"
                )
            }
            else -> {
                appendOutput("bash: $root: command not found (type '\u001B[33mhelp\u001B[0m' for list of simulated tools)\r\n")
            }
        }
    }

    private fun getFastFetchBanner(): String {
        return "\u001B[1;36m       _,met\$\$\$\$\$gg.\u001B[0m          \u001B[1;32mroot\u001B[0m@\u001B[1;36m$hostname\u001B[0m\r\n" +
               "\u001B[1;36m    ,g\$\$\$\$\$\$\$\$\$\$\$\$\$\$\$P.\u001B[0m       -------------------\r\n" +
               "\u001B[1;36m  ,g\$\$P\"\"       \"\"\"Y\$\$.\".\u001B[0m     \u001B[1;33mOS:\u001B[0m Ubuntu 24.04.1 LTS x86_64\r\n" +
               "\u001B[1;36m ,\$\$P'              `\$\$\$.\u001B[0m    \u001B[1;33mHost:\u001B[0m KVM Cloud VPS (4 vCPUs)\r\n" +
               "\u001B[1;36m',\$\$P       ,ggs.     `\$\$b:\u001B[0m  \u001B[1;33mKernel:\u001B[0m Linux 6.8.0-45-generic\r\n" +
               "\u001B[1;36m`d\$\$'     ,\$P\"'   .    \$\$\$k\u001B[0m  \u001B[1;33mUptime:\u001B[0m 48 days, 14 hours\r\n" +
               "\u001B[1;36m \$\$P      d\$\$'     ,    \$\$\$\u001B[0m   \u001B[1;33mPackages:\u001B[0m 1,482 (dpkg), 4 (docker)\r\n" +
               "\u001B[1;36m \$\$:      \$\$.   -    ,d\$\$' \u001B[0m  \u001B[1;33mShell:\u001B[0m bash 5.2.21\r\n" +
               "\u001B[1;36m \$\$;      Y\$b._   _,d\$P'   \u001B[0m  \u001B[1;33mTerminal:\u001B[0m Bin Box VT100 / Xterm\r\n" +
               "\u001B[1;36m Y\$\$.    `.`\"Y\$\$\$\$P\"'      \u001B[0m  \u001B[1;33mCPU:\u001B[0m AMD EPYC 7763 (4) @ 3.24GHz\r\n" +
               "\u001B[1;36m  `\$\$b      \"-.__          \u001B[0m  \u001B[1;33mMemory:\u001B[0m 4,210MiB / 16,384MiB (25%)\r\n" +
               "\u001B[1;36m   `Y\$\$.                   \u001B[0m  \u001B[1;33mDisk (/):\u001B[0m 42GiB / 120GiB (37%)\r\n" +
               "                           \u001B[40m   \u001B[41m   \u001B[42m   \u001B[43m   \u001B[44m   \u001B[45m   \u001B[46m   \u001B[47m   \u001B[0m\r\n\r\n"
    }

    private fun appendPrompt() {
        appendOutput("\u001B[1;32m$username@$hostname\u001B[0m:\u001B[1;34m$currentDir\u001B[0m# ")
    }

    override fun sendSpecialKey(key: TerminalKey) {
        when (key) {
            TerminalKey.CTRL_C -> {
                appendOutput("^C\r\n")
                appendPrompt()
            }
            TerminalKey.CTRL_L -> {
                clear()
                appendPrompt()
            }
            TerminalKey.CTRL_D -> {
                disconnect()
            }
            else -> {}
        }
    }

    override fun sendRawBytes(bytes: ByteArray) {}

    override fun clear() {
        ansiParser.clear()
        _lines.value = emptyList()
    }

    override fun disconnect() {
        _state.value = SessionState.Disconnected
        appendOutput("\r\n\u001B[33m[Connection to $hostLabel closed by user]\u001B[0m\r\n")
    }

    override fun updateTheme(theme: TerminalThemePreset) {
        ansiParser.updateTheme(theme)
        _lines.value = ansiParser.getLines()
    }

    private fun appendOutput(chunk: String) {
        logBuffer.append(chunk)
        ansiParser.feed(chunk)
        _lines.value = ansiParser.getLines()
    }
}
