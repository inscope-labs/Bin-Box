package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.terminal.model.SessionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Provider-independent transport abstraction (Phase 3 — Session & Transport
 * Framework).
 *
 * A transport owns exactly one connection to a single execution target — an
 * SSH host, a local process, a raw socket, a WebSocket relay, and so on.
 * Terminal-facing code (ShellSession implementations, TerminalSessionManager,
 * the emulator/UI layer) depends only on this interface, never on a concrete
 * transport.
 *
 * Architectural rule carried over from Phase 1: the terminal core must not
 * know whether it is talking to Oracle, Termux, SSH, WebSocket, or any other
 * provider. Everything provider-specific belongs inside an ITransport
 * implementation and nowhere else.
 *
 * Deliberately reuses [SessionState] (terminal.model) rather than a parallel
 * "TransportState" — one connection-lifecycle model for the whole app.
 */
interface ITransport {

    /** Connection lifecycle state. */
    val state: StateFlow<SessionState>

    /** Cumulative bytes moved in each direction, for telemetry/UX. */
    val bytesReceived: StateFlow<Long>
    val bytesSent: StateFlow<Long>

    /**
     * Register the listener that receives raw output bytes and terminal
     * lifecycle events. Implementations should tolerate this being set
     * before [connect] is called, and should no-op safely if it is never
     * set at all.
     */
    fun setListener(listener: TransportListener)

    /**
     * Establish the connection. Suspends until the connection is fully
     * established or has definitively failed — it does not suspend for the
     * lifetime of the session. Ongoing I/O is reported via [TransportListener]
     * and [state], not via this call's return.
     */
    suspend fun connect()

    /**
     * Write raw bytes to the remote/local process. Fire-and-forget from the
     * caller's perspective; write failures surface via
     * [TransportListener.onError], not as a thrown exception here.
     */
    fun sendData(data: ByteArray)

    /**
     * Propagate a PTY/window resize, in character cells and — where the
     * transport supports it — pixels. Transports that have no concept of a
     * PTY (e.g. a future WebSocket relay that only forwards structured
     * resize messages) should treat this as a best-effort hint, not a
     * required capability.
     */
    fun resize(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0)

    /**
     * Tear down the connection and release all transport resources. Must be
     * safe to call multiple times and safe to call on a transport that never
     * successfully connected.
     */
    fun disconnect()
}

/**
 * Event callbacks a transport emits during its lifetime. Kept separate from
 * [ITransport.state] because terminal output is a stream of events, not a
 * state — a transport can emit many [onDataReceived] calls per state.
 */
interface TransportListener {

    /** Raw bytes received from the remote/local process, in arrival order. */
    fun onDataReceived(data: ByteArray)

    /**
     * Connection closed — either the remote end closed it, a local process
     * exited, or [ITransport.disconnect] was called. Not itself an error;
     * see [onError] for failures.
     */
    fun onClosed(reason: String? = null)

    /**
     * Connection failed to establish, or failed after being connected.
     * Implementations should also move [ITransport.state] to
     * [SessionState.Error] before or alongside invoking this.
     */
    fun onError(message: String, cause: Throwable? = null)
}
