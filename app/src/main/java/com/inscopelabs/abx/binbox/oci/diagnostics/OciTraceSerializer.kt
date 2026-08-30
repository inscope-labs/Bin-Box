package com.inscopelabs.abx.binbox.oci.diagnostics

import org.json.JSONObject

/**
 * Serialization and plain-text export formatting module for OCI call trace records.
 */
object OciTraceSerializer {

    fun entryToJson(entry: OciCallTraceEntry): String {
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

    fun exportToFormattedText(sessionId: String, entries: List<OciCallTraceEntry>): String {
        if (entries.isEmpty()) {
            return "No OCI API call traces recorded for session: $sessionId"
        }

        val sb = StringBuilder()
        sb.append("=== OCI CALL TRACE DUMP (Session: $sessionId, Total: ${entries.size}) ===\n\n")
        entries.forEachIndexed { index, entry ->
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
}
