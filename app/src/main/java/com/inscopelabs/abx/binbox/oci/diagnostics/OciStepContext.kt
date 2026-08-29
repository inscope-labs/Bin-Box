package com.inscopelabs.abx.binbox.oci.diagnostics

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import java.util.concurrent.atomic.AtomicReference

/**
 * Context holder to correlate asynchronous/synchronous OCI provisioning steps
 * with low-level OkHttp/Retrofit HTTP calls and call trace records.
 */
object OciStepContext {
    const val UNKNOWN_STAGE = "UNKNOWN"
    const val UNKNOWN_STEP = "unknown"
    @PublishedApi internal const val TAG = "OciStepContext"

    @PublishedApi
    internal val activeContext = AtomicReference<Pair<String, String>?>(null)

    /**
     * Executes the given [block] within the scope of the provided [stageId] and [stepId].
     * Restores the previous context upon block completion or thrown exception.
     */
    inline fun <T> withStep(stageId: String, stepId: String, block: () -> T): T {
        val previous = activeContext.get()
        val current = stageId to stepId
        activeContext.set(current)
        BinBoxLogger.d(TAG, "Entering step context: [$stageId / $stepId]")
        try {
            return block()
        } finally {
            activeContext.set(previous)
            BinBoxLogger.d(TAG, "Exiting step context: [$stageId / $stepId] (restored: ${previous?.first ?: UNKNOWN_STAGE}/${previous?.second ?: UNKNOWN_STEP})")
        }
    }

    /**
     * Retrieves the active (stageId, stepId) pair,
     * or ("UNKNOWN", "unknown") if no context has been set.
     */
    fun currentOrUnknown(): Pair<String, String> {
        return activeContext.get() ?: (UNKNOWN_STAGE to UNKNOWN_STEP)
    }

    /**
     * Explicitly sets the active step context.
     */
    fun set(stageId: String, stepId: String) {
        activeContext.set(stageId to stepId)
        BinBoxLogger.d(TAG, "Explicitly set step context: [$stageId / $stepId]")
    }

    /**
     * Clears the active step context.
     */
    fun clear() {
        activeContext.set(null)
        BinBoxLogger.d(TAG, "Cleared step context")
    }
}
