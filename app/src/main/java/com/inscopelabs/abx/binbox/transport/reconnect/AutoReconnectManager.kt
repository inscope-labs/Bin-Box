package com.inscopelabs.abx.binbox.transport.reconnect

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AutoReconnectState {
    data object Idle : AutoReconnectState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int, val nextDelayMs: Long) : AutoReconnectState()
    data class Failed(val attemptsMade: Int) : AutoReconnectState()
}

/**
 * Manages automatic reconnection attempts with backoff and cancellation.
 * (Phase 3 — Session & Transport Framework)
 */
class AutoReconnectManager(
    private val scope: CoroutineScope,
    private val policy: ReconnectionPolicy = ExponentialBackoffPolicy(),
    private val reconnectAction: suspend () -> Unit
) {

    private val _state = MutableStateFlow<AutoReconnectState>(AutoReconnectState.Idle)
    val state: StateFlow<AutoReconnectState> = _state.asStateFlow()

    private var currentAttempt = 0
    private var reconnectJob: Job? = null

    val isReconnecting: Boolean
        get() = _state.value is AutoReconnectState.Reconnecting

    fun triggerReconnect(sessionTag: String = "Session") {
        if (isReconnecting) {
            BinBoxLogger.d("AutoReconnectManager", "Reconnect already in progress for $sessionTag")
            return
        }

        val nextDelay = policy.getNextDelayMs(currentAttempt)
        if (nextDelay == null) {
            BinBoxLogger.w("AutoReconnectManager", "Max reconnect attempts ($currentAttempt) reached for $sessionTag")
            _state.value = AutoReconnectState.Failed(currentAttempt)
            return
        }

        val attemptNum = currentAttempt + 1
        _state.value = AutoReconnectState.Reconnecting(attemptNum, policy.maxAttempts, nextDelay)
        BinBoxLogger.i("AutoReconnectManager", "Scheduling reconnect attempt $attemptNum/${policy.maxAttempts} in ${nextDelay}ms for $sessionTag")

        reconnectJob = scope.launch {
            try {
                delay(nextDelay)
                currentAttempt++
                BinBoxLogger.i("AutoReconnectManager", "Executing reconnect attempt $currentAttempt for $sessionTag")
                _state.value = AutoReconnectState.Idle
                reconnectAction()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Normal job cancellation - preserve reset state
            } catch (e: Exception) {
                BinBoxLogger.e("AutoReconnectManager", "Reconnect attempt $currentAttempt failed for $sessionTag", e)
                _state.value = AutoReconnectState.Failed(currentAttempt)
            }
        }
    }

    fun onConnectedSuccess() {
        if (currentAttempt > 0 || isReconnecting) {
            BinBoxLogger.i("AutoReconnectManager", "Session connection established. Resetting reconnect attempts (was $currentAttempt)")
        }
        reset()
    }

    fun cancel(reason: String = "User requested") {
        if (reconnectJob?.isActive == true) {
            BinBoxLogger.d("AutoReconnectManager", "Cancelling pending reconnect: $reason")
            reconnectJob?.cancel()
        }
        reset()
    }

    fun reset() {
        reconnectJob?.cancel()
        reconnectJob = null
        currentAttempt = 0
        _state.value = AutoReconnectState.Idle
    }
}
