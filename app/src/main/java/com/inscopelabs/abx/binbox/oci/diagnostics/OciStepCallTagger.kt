package com.inscopelabs.abx.binbox.oci.diagnostics

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import okhttp3.Call
import okhttp3.EventListener
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges [OciStepContext]'s calling-thread-known step info onto the specific
 * [okhttp3.Call] it belongs to.
 *
 * Why this exists, not a thread-local or coroutine-context read directly in the
 * interceptor: Retrofit's `suspend fun ... : Response<T>` support (used by every
 * API interface in this module) is built on `Call.enqueue()` — its callback fires
 * on OkHttp's own pooled `Dispatcher` executor threads, which are plain background
 * worker threads OkHttp manages internally and are never a coroutine-resumption
 * point. That means [OciSigningInterceptor], which runs as part of that same
 * dispatcher-thread execution, cannot see either a plain `ThreadLocal` set on the
 * original calling thread, or a kotlinx.coroutines `ThreadContextElement`/
 * `asContextElement` value (that machinery only updates thread-locals at coroutine
 * resumption points, and the interceptor chain is not one — it's invoked from a
 * plain `Runnable` OkHttp submits to its own `ExecutorService`).
 *
 * The one thing immune to that hop is the [okhttp3.Request]/[okhttp3.Call] object
 * itself, since it's the same immutable instance passed through regardless of which
 * thread eventually processes it. [EventListener.Factory.create] is the correct,
 * stable, public OkHttp hook for this: it fires synchronously on whatever thread
 * calls `OkHttpClient.newCall(request)` — which for a Retrofit suspend call happens
 * before any suspension/enqueue — so reading [OciStepContext]'s per-thread value
 * here is guaranteed correct for the specific call being created, with no shared
 * mutable state and no risk of one concurrent call's context leaking into another's.
 */
object OciStepCallTagger : EventListener.Factory {

    private const val TAG = "OciStepCallTagger"
    private val tagsByCall = ConcurrentHashMap<Call, Pair<String, String>>()

    override fun create(call: Call): EventListener {
        val step = OciStepContext.currentThreadValue()
        tagsByCall[call] = step
        BinBoxLogger.d(TAG, "Tagged call ${call.request().url} with [${step.first}/${step.second}]")
        return object : EventListener() {
            override fun callEnd(call: Call) {
                tagsByCall.remove(call)
            }

            override fun callFailed(call: Call, ioe: IOException) {
                tagsByCall.remove(call)
            }
        }
    }

    /**
     * Looks up the step info tagged for [call] at creation time. Safe to call from
     * any thread — this is the correct accessor for [OciSigningInterceptor] and any
     * other interceptor, regardless of which thread OkHttp is currently running on.
     */
    fun stepFor(call: Call): Pair<String, String> =
        tagsByCall[call] ?: (OciStepContext.UNKNOWN_STAGE to OciStepContext.UNKNOWN_STEP)
}
