package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.transport.reconnect.AutoReconnectManager
import com.inscopelabs.abx.binbox.transport.reconnect.AutoReconnectState
import com.inscopelabs.abx.binbox.transport.reconnect.FixedIntervalPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoReconnectManagerTest {

    @Test
    fun autoReconnectManager_executesReconnectActionAfterDelay() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var reconnectCallCount = 0
        val manager = AutoReconnectManager(
            scope = testScope,
            policy = FixedIntervalPolicy(intervalMs = 1000L, maxAttempts = 2),
            reconnectAction = { reconnectCallCount++ }
        )

        assertEquals(AutoReconnectState.Idle, manager.state.value)

        // Trigger first reconnect
        manager.triggerReconnect("TestSession")
        assertTrue(manager.isReconnecting)
        val state1 = manager.state.value as AutoReconnectState.Reconnecting
        assertEquals(1, state1.attempt)
        assertEquals(1000L, state1.nextDelayMs)

        // Advance before delay
        testScheduler.advanceTimeBy(500)
        assertEquals(0, reconnectCallCount)

        // Advance past delay
        testScheduler.advanceTimeBy(600)
        assertEquals(1, reconnectCallCount)

        // Reset on success
        manager.onConnectedSuccess()
        assertEquals(AutoReconnectState.Idle, manager.state.value)
        assertFalse(manager.isReconnecting)
    }

    @Test
    fun autoReconnectManager_reachesFailedStateWhenMaxAttemptsExceeded() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var reconnectCallCount = 0
        val manager = AutoReconnectManager(
            scope = testScope,
            policy = FixedIntervalPolicy(intervalMs = 500L, maxAttempts = 1),
            reconnectAction = { reconnectCallCount++ }
        )

        // Trigger attempt 1
        manager.triggerReconnect("TestSession")
        testScheduler.advanceTimeBy(600)
        assertEquals(1, reconnectCallCount)

        // Trigger attempt 2 (should fail since max is 1)
        manager.triggerReconnect("TestSession")
        assertTrue(manager.state.value is AutoReconnectState.Failed)
        val failed = manager.state.value as AutoReconnectState.Failed
        assertEquals(1, failed.attemptsMade)
    }

    @Test
    fun autoReconnectManager_cancelsPendingReconnect() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        var reconnectCallCount = 0
        val manager = AutoReconnectManager(
            scope = testScope,
            policy = FixedIntervalPolicy(intervalMs = 2000L, maxAttempts = 3),
            reconnectAction = { reconnectCallCount++ }
        )

        manager.triggerReconnect("TestSession")
        assertTrue(manager.isReconnecting)

        testScheduler.advanceTimeBy(500)
        manager.cancel("Manual cancel")

        testScheduler.advanceTimeBy(2000)
        assertEquals(0, reconnectCallCount)
        assertEquals(AutoReconnectState.Idle, manager.state.value)
    }
}
