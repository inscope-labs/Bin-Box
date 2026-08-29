package com.inscopelabs.abx.binbox.oci.diagnostics

import android.content.Context
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.UUID

/**
 * Thread-safe append-only store for OCI API call traces.
 * Maintains an in-memory StateFlow and persists newline-delimited JSON (JSONL) to disk.
 */
object OciCallTraceStore {
    private const val TAG = "OciCallTraceStore"
    private const val TRACE_DIR_NAME = "oci-trace"

    private val lock = Any()
    private var appContext: Context? = null
    private var currentSessionId: String = UUID.randomUUID().toString()

    private val _entries = MutableStateFlow<List<OciCallTraceEntry>>(emptyList())
    val entries: StateFlow<List<OciCallTraceEntry>> = _entries.asStateFlow()

    fun initialize(context: Context, sessionId: String? = null) {
        synchronized(lock) {
            appContext = context.applicationContext
            if (sessionId != null) {
                currentSessionId = sessionId
            }
            BinBoxLogger.i(TAG, "Initialized trace store for session: $currentSessionId")
        }
    }

    fun startNewSession(sessionId: String = UUID.randomUUID().toString()) {
        synchronized(lock) {
            currentSessionId = sessionId
            _entries.value = emptyList()
            BinBoxLogger.i(TAG, "Started new trace session: $currentSessionId")
        }
    }

    fun record(entry: OciCallTraceEntry) {
        val sessionId: String
        synchronized(lock) {
            sessionId = currentSessionId
            val updated = _entries.value + entry
            _entries.value = updated
        }

        BinBoxLogger.d(
            TAG,
            "Recorded trace [${entry.stageId}/${entry.stepId}] ${entry.method} ${entry.url} -> HTTP ${entry.httpStatusCode ?: "ERR"}"
        )

        writeToFile(entry, sessionId)
    }

    fun currentSessionEntries(): List<OciCallTraceEntry> {
        synchronized(lock) {
            return _entries.value
        }
    }

    fun clear(newSessionId: String? = null) {
        synchronized(lock) {
            currentSessionId = newSessionId ?: UUID.randomUUID().toString()
            _entries.value = emptyList()
            BinBoxLogger.i(TAG, "Cleared in-memory trace store; active session: $currentSessionId")
        }
    }

    fun exportAsText(): String {
        val current = currentSessionEntries()
        if (current.isEmpty()) {
            return "No OCI API call traces recorded for session: $currentSessionId"
        }

        val sb = StringBuilder()
        sb.append("=== OCI CALL TRACE DUMP (Session: $currentSessionId, Total: ${current.size}) ===\n\n")
        current.forEachIndexed { index, entry ->
            sb.append("--- [#${index + 1}] ${entry.method} ${entry.url} ---\n")
            sb.append("ID: ${entry.id}\n")
            sb.append("Timestamp: ${entry.timestampUtc}\n")
            sb.append("Stage: ${entry.stageId} | Step: ${entry.stepId}\n")
            sb.append("Duration: ${entry.durationMs}ms\n")
            sb.append("Status: ${entry.httpStatusCode ?: "No HTTP response"}\n")
            if (entry.ociErrorCode != null || entry.ociErrorMessage != null) {
                sb.append("OCI Error: [${entry.ociErrorCode}] ${entry.ociErrorMessage}\n")
            }
            if (entry.exceptionClass != null || entry.exceptionMessage != null) {
                sb.append("Exception: ${entry.exceptionClass}: ${entry.exceptionMessage}\n")
            }
            sb.append("\n[Request Headers]\n")
            entry.requestHeaders.forEach { (k, v) -> sb.append("  $k: $v\n") }
            if (entry.requestBody != null) {
                sb.append("\n[Request Body]\n${entry.requestBody}\n")
            }
            if (entry.responseHeaders != null) {
                sb.append("\n[Response Headers]\n")
                entry.responseHeaders.forEach { (k, v) -> sb.append("  $k: $v\n") }
            }
            if (entry.responseBody != null) {
                sb.append("\n[Response Body]\n${entry.responseBody}\n")
            }
            sb.append("\n------------------------------------------------------------\n\n")
        }
        return sb.toString()
    }

    private fun writeToFile(entry: OciCallTraceEntry, sessionId: String) {
        val context = appContext ?: return
        try {
            val traceDir = File(context.filesDir, TRACE_DIR_NAME)
            if (!traceDir.exists()) {
                val created = traceDir.mkdirs()
                BinBoxLogger.d(TAG, "Created trace directory: ${traceDir.absolutePath} (result=$created)")
            }
            val traceFile = File(traceDir, "$sessionId.jsonl")
            val jsonLine = entryToJson(entry)
            FileWriter(traceFile, true).use { writer ->
                writer.write(jsonLine)
                writer.write("\n")
            }
        } catch (e: Exception) {
            BinBoxLogger.e(TAG, "Failed writing trace entry to JSONL file", e)
        }
    }

    private fun entryToJson(entry: OciCallTraceEntry): String {
        val obj = JSONObject()
        obj.put("id", entry.id)
        obj.put("timestampUtc", entry.timestampUtc)
        obj.put("stageId", entry.stageId)
        obj.put("stepId", entry.stepId)
        obj.put("method", entry.method)
        obj.put("url", entry.url)
        obj.put("requestHeaders", JSONObject(entry.requestHeaders))
        obj.put("requestBody", entry.requestBody ?: JSONObject.NULL)
        obj.put("httpStatusCode", entry.httpStatusCode ?: JSONObject.NULL)
        obj.put("responseHeaders", entry.responseHeaders?.let { JSONObject(it) } ?: JSONObject.NULL)
        obj.put("responseBody", entry.responseBody ?: JSONObject.NULL)
        obj.put("ociErrorCode", entry.ociErrorCode ?: JSONObject.NULL)
        obj.put("ociErrorMessage", entry.ociErrorMessage ?: JSONObject.NULL)
        obj.put("exceptionClass", entry.exceptionClass ?: JSONObject.NULL)
        obj.put("exceptionMessage", entry.exceptionMessage ?: JSONObject.NULL)
        obj.put("durationMs", entry.durationMs)
        return obj.toString()
    }
}
