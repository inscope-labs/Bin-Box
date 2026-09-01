package com.inscopelabs.abx.binbox.transport.reconnect

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Strategy interface defining reconnect retry rules and delays.
 * Part of Phase 3 — Session & Transport Framework.
 */
interface ReconnectionPolicy {
    val maxAttempts: Int
    fun getNextDelayMs(attempt: Int): Long?
    fun canRetry(attempt: Int): Boolean = attempt < maxAttempts
}

/**
 * Standard exponential backoff policy with configurable jitter.
 */
data class ExponentialBackoffPolicy(
    val initialDelayMs: Long = 1000L,
    val maxDelayMs: Long = 30000L,
    val multiplier: Double = 2.0,
    override val maxAttempts: Int = 5,
    val jitterFactor: Double = 0.1
) : ReconnectionPolicy {

    override fun getNextDelayMs(attempt: Int): Long? {
        if (!canRetry(attempt)) return null
        val baseDelay = (initialDelayMs * multiplier.pow(attempt.toDouble())).toLong()
        val cappedDelay = min(baseDelay, maxDelayMs)
        val jitterRange = (cappedDelay * jitterFactor).toLong()
        val jitter = if (jitterRange > 0) Random.nextLong(-jitterRange, jitterRange + 1) else 0L
        return (cappedDelay + jitter).coerceAtLeast(0L)
    }
}

/**
 * Fixed interval reconnect policy.
 */
data class FixedIntervalPolicy(
    val intervalMs: Long = 3000L,
    override val maxAttempts: Int = 3
) : ReconnectionPolicy {

    override fun getNextDelayMs(attempt: Int): Long? {
        return if (canRetry(attempt)) intervalMs else null
    }
}

/**
 * Policy indicating auto-reconnection is disabled.
 */
object NoReconnectPolicy : ReconnectionPolicy {
    override val maxAttempts: Int = 0
    override fun getNextDelayMs(attempt: Int): Long? = null
}
