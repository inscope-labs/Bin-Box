package com.inscopelabs.abx.binbox.oci.provisioning

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Generic poll-until-done helper for §22's instance state machine (and any
 * other eventually-consistent OCI resource this package needs to wait on).
 * Not OCI-specific itself — [fetch]/[isDone]/[isFailed] carry all
 * OCI-specific knowledge.
 */
object ProvisioningPoller {

    suspend fun <T> poll(
        interval: Duration = 5.seconds,
        timeout: Duration = 5.minutes,
        isDone: (T) -> Boolean,
        isFailed: (T) -> Boolean,
        fetch: suspend () -> OciResult<T>
    ): OciResult<T> {
        val start = TimeSource.Monotonic.markNow()
        while (true) {
            when (val result = fetch()) {
                is OciResult.Error -> return result
                is OciResult.Success -> {
                    val value = result.data
                    if (isFailed(value)) {
                        return OciResult.Error(
                            OciProvisioningError(
                                category = OciErrorCategory.UNKNOWN_ERROR,
                                whatHappened = "Resource reached a failure state while waiting.",
                                retryable = false
                            )
                        )
                    }
                    if (isDone(value)) return result
                }
            }
            if (start.elapsedNow() >= timeout) {
                return OciResult.Error(
                    OciProvisioningError(
                        category = OciErrorCategory.TIMEOUT_ERROR,
                        whatHappened = "Timed out waiting for resource (${timeout}).",
                        retryable = true
                    )
                )
            }
            delay(interval)
        }
    }
}
