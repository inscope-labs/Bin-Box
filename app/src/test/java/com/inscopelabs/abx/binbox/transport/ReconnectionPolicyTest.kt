package com.inscopelabs.abx.binbox.transport

import com.inscopelabs.abx.binbox.transport.reconnect.ExponentialBackoffPolicy
import com.inscopelabs.abx.binbox.transport.reconnect.FixedIntervalPolicy
import com.inscopelabs.abx.binbox.transport.reconnect.NoReconnectPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectionPolicyTest {

    @Test
    fun exponentialBackoffPolicy_computesDelaysCorrectly() {
        val policy = ExponentialBackoffPolicy(
            initialDelayMs = 1000L,
            maxDelayMs = 8000L,
            multiplier = 2.0,
            maxAttempts = 4,
            jitterFactor = 0.0
        )

        assertTrue(policy.canRetry(0))
        assertEquals(1000L, policy.getNextDelayMs(0))

        assertTrue(policy.canRetry(1))
        assertEquals(2000L, policy.getNextDelayMs(1))

        assertTrue(policy.canRetry(2))
        assertEquals(4000L, policy.getNextDelayMs(2))

        assertTrue(policy.canRetry(3))
        assertEquals(8000L, policy.getNextDelayMs(3))

        // Exceeded max attempts
        assertFalse(policy.canRetry(4))
        assertNull(policy.getNextDelayMs(4))
    }

    @Test
    fun exponentialBackoffPolicy_capsAtMaxDelay() {
        val policy = ExponentialBackoffPolicy(
            initialDelayMs = 5000L,
            maxDelayMs = 10000L,
            multiplier = 3.0,
            maxAttempts = 5,
            jitterFactor = 0.0
        )

        assertEquals(5000L, policy.getNextDelayMs(0))
        assertEquals(10000L, policy.getNextDelayMs(1)) // 15000 capped to 10000
    }

    @Test
    fun fixedIntervalPolicy_returnsConstantDelayUntilMax() {
        val policy = FixedIntervalPolicy(intervalMs = 2500L, maxAttempts = 3)

        assertTrue(policy.canRetry(0))
        assertEquals(2500L, policy.getNextDelayMs(0))
        assertTrue(policy.canRetry(1))
        assertEquals(2500L, policy.getNextDelayMs(1))
        assertTrue(policy.canRetry(2))
        assertEquals(2500L, policy.getNextDelayMs(2))

        assertFalse(policy.canRetry(3))
        assertNull(policy.getNextDelayMs(3))
    }

    @Test
    fun noReconnectPolicy_neverRetries() {
        val policy = NoReconnectPolicy
        assertFalse(policy.canRetry(0))
        assertNull(policy.getNextDelayMs(0))
        assertEquals(0, policy.maxAttempts)
    }
}
