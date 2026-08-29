package com.inscopelabs.abx.binbox.oci.diagnostics

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.oci.provisioning.OciApiErrorMapper
import okhttp3.Headers
import okhttp3.Response

/**
 * Helper module to record success and failure traces into [OciCallTraceStore].
 */
object OciTraceRecorder {
    private const val TAG = "OciTraceRecorder"

    fun extractHeaders(headers: Headers?): Map<String, String> {
        if (headers == null) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (i in 0 until headers.size) {
            map[headers.name(i)] = headers.value(i)
        }
        return map
    }

    fun recordSuccess(
        traceId: String,
        timestampUtc: String,
        stageId: String,
        stepId: String,
        method: String,
        url: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        response: Response,
        rawResponseBody: String?,
        durationMs: Long
    ) {
        val (ociErrorCode, ociErrorMessage) = OciApiErrorMapper.parseError(rawResponseBody)
        val responseHeaders = extractHeaders(response.headers)

        val entry = OciCallTraceEntry(
            id = traceId,
            timestampUtc = timestampUtc,
            stageId = stageId,
            stepId = stepId,
            method = method,
            url = url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            httpStatusCode = response.code,
            responseHeaders = responseHeaders,
            responseBody = rawResponseBody,
            ociErrorCode = ociErrorCode,
            ociErrorMessage = ociErrorMessage,
            exceptionClass = null,
            exceptionMessage = null,
            durationMs = durationMs
        )
        OciCallTraceStore.record(entry)
        BinBoxLogger.d(TAG, "Recorded success [$stageId/$stepId] $method $url -> HTTP ${response.code} (${durationMs}ms)")
    }

    fun recordFailure(
        traceId: String,
        timestampUtc: String,
        stageId: String,
        stepId: String,
        method: String,
        url: String,
        requestHeaders: Map<String, String>,
        requestBody: String?,
        throwable: Throwable,
        durationMs: Long
    ) {
        BinBoxLogger.e(TAG, "Recorded failure [$stageId/$stepId] $method $url: ${throwable.message}", throwable)
        val entry = OciCallTraceEntry(
            id = traceId,
            timestampUtc = timestampUtc,
            stageId = stageId,
            stepId = stepId,
            method = method,
            url = url,
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            httpStatusCode = null,
            responseHeaders = null,
            responseBody = null,
            ociErrorCode = null,
            ociErrorMessage = null,
            exceptionClass = throwable.javaClass.name,
            exceptionMessage = throwable.message ?: throwable.toString(),
            durationMs = durationMs
        )
        OciCallTraceStore.record(entry)
    }
}
