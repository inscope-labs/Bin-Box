package com.inscopelabs.abx.binbox.oci.diagnostics

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger

/**
 * Context holder to correlate the calling code's known OCI provisioning
 * step with the [okhttp3.Call] it's about to create.
 *
 * Backed by a genuine per-thread [ThreadLocal] — deliberately NOT a shared
 * [java.util.concurrent.atomic.AtomicReference]. This app has no re-entrancy
 * guard preventing overlapping in-flight OCI calls (e.g. two independent
 * `viewModelScope.launch` calls racing from [OciOnboardingViewModel]), so a
 * single shared value would silently misattribute one call's trace entries
 * to another call's step whenever two calls are genuinely concurrent.
 *
 * This value is only ever correct when read synchronously, on the same
 * thread, before any suspension occurs — which is exactly what
 * [OciStepCallTagger] does (see its doc for why that boundary matters and
 * how the value survives past it). Do not read [currentThreadValue] from
 * inside an OkHttp interceptor or anything else that might run on a
 * different thread than the one that called [withStep] — use
 * [OciStepCallTagger.stepFor] with the actual `Call` instead.
 */
object OciStepContext {
    const val UNKNOWN_STAGE = "UNKNOWN"
    const val UNKNOWN_STEP = "unknown"
    @PublishedApi internal const val TAG = "OciStepContext"

    @PublishedApi
    internal val threadLocal = ThreadLocal<Pair<String, String>?>()

    /**
     * Executes the given [block] within the scope of the provided [stageId] and [stepId],
     * on the calling thread. Restores the previous context upon block completion or thrown
     * exception, so nesting (e.g. a coarse resume-level step wrapping finer discovery steps)
     * behaves correctly.
     */
    inline fun <T> withStep(stageId: String, stepId: String, block: () -> T): T {
        val previous = threadLocal.get()
        threadLocal.set(stageId to stepId)
        BinBoxLogger.d(TAG, "Entering step context: [$stageId / $stepId]")
        try {
            return block()
        } finally {
            threadLocal.set(previous)
            BinBoxLogger.d(TAG, "Exiting step context: [$stageId / $stepId] (restored: ${previous?.first ?: UNKNOWN_STAGE}/${previous?.second ?: UNKNOWN_STEP})")
        }
    }

    /**
     * Reads the active (stageId, stepId) pair for the CALLING thread, or
     * ("UNKNOWN", "unknown") if none is set. Only meaningful when called
     * synchronously from the same thread as an enclosing [withStep] — see
     * [OciStepCallTagger], which is the only intended caller in production
     * code.
     */
    fun currentThreadValue(): Pair<String, String> = threadLocal.get() ?: (UNKNOWN_STAGE to UNKNOWN_STEP)

    /**
     * Alias for [currentThreadValue], kept for source compatibility with existing
     * callers/tests written against the prior design. Same same-thread-only caveat
     * applies — prefer [currentThreadValue] in new code, called only from
     * [OciStepCallTagger].
     */
    fun currentOrUnknown(): Pair<String, String> = currentThreadValue()

    /** Test-only / explicit reset hook. */
    fun clear() {
        threadLocal.set(null)
        BinBoxLogger.d(TAG, "Cleared step context")
    }
}
