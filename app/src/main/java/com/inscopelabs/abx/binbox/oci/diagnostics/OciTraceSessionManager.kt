package com.inscopelabs.abx.binbox.oci.diagnostics

import android.content.Context
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Metadata descriptor for a persisted OCI call trace session file on disk.
 */
data class OciTraceSessionInfo(
    val sessionId: String,
    val file: File,
    val lastModifiedMs: Long,
    val sizeBytes: Long,
    val lineCount: Int
)

/**
 * Module managing disk retention, listing, reading, and cleanup for OCI trace JSONL files.
 */
object OciTraceSessionManager {
    private const val TAG = "OciTraceSessionManager"
    const val TRACE_DIR_NAME = "oci-trace"
    private const val DEFAULT_MAX_SAVED_SESSIONS = 10

    fun getTraceDir(context: Context): File {
        val dir = File(context.filesDir, TRACE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listSavedSessions(context: Context): List<OciTraceSessionInfo> {
        val dir = getTraceDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".jsonl") } ?: return emptyList()
        return files.map { file ->
            val sessionId = file.name.removeSuffix(".jsonl")
            val lineCount = try {
                file.useLines { it.count() }
            } catch (_: Exception) {
                0
            }
            OciTraceSessionInfo(
                sessionId = sessionId,
                file = file,
                lastModifiedMs = file.lastModified(),
                sizeBytes = file.length(),
                lineCount = lineCount
            )
        }.sortedByDescending { it.lastModifiedMs }
    }

    fun loadSessionEntries(file: File): List<OciCallTraceEntry> {
        if (!file.exists()) return emptyList()
        val entries = mutableListOf<OciCallTraceEntry>()
        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        parseJsonLine(line)?.let { entries.add(it) }
                    }
                    line = reader.readLine()
                }
            }
            BinBoxLogger.d(TAG, "Loaded ${entries.size} trace entries from ${file.name}")
        } catch (e: Exception) {
            BinBoxLogger.e(TAG, "Failed to load trace entries from file ${file.name}", e)
        }
        return entries
    }

    fun pruneOldSessions(context: Context, maxSessions: Int = DEFAULT_MAX_SAVED_SESSIONS) {
        try {
            val sessions = listSavedSessions(context)
            if (sessions.size > maxSessions) {
                val toDelete = sessions.drop(maxSessions)
                toDelete.forEach { session ->
                    val deleted = session.file.delete()
                    BinBoxLogger.i(TAG, "Pruned old trace file ${session.file.name} (success=$deleted)")
                }
            }
        } catch (e: Exception) {
            BinBoxLogger.e(TAG, "Failed during trace retention pruning", e)
        }
    }

    fun deleteSession(context: Context, sessionId: String): Boolean {
        return try {
            val file = File(getTraceDir(context), "$sessionId.jsonl")
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            BinBoxLogger.e(TAG, "Failed to delete trace session: $sessionId", e)
            false
        }
    }

    fun parseJsonLine(jsonString: String): OciCallTraceEntry? {
        return try {
            val obj = JSONObject(jsonString)
            val reqHeadersObj = obj.optJSONObject("requestHeaders")
            val reqHeaders = mutableMapOf<String, String>()
            reqHeadersObj?.keys()?.forEach { k -> reqHeaders[k] = reqHeadersObj.getString(k) }

            val resHeadersObj = obj.optJSONObject("responseHeaders")
            val resHeaders = resHeadersObj?.let { resObj ->
                val map = mutableMapOf<String, String>()
                resObj.keys().forEach { k -> map[k] = resObj.getString(k) }
                map
            }

            OciCallTraceEntry(
                id = obj.getString("id"),
                timestampUtc = obj.getString("timestampUtc"),
                stageId = obj.getString("stageId"),
                stepId = obj.getString("stepId"),
                method = obj.getString("method"),
                url = obj.getString("url"),
                requestHeaders = reqHeaders,
                requestBody = if (obj.isNull("requestBody")) null else obj.optString("requestBody"),
                httpStatusCode = if (obj.isNull("httpStatusCode")) null else obj.optInt("httpStatusCode"),
                responseHeaders = resHeaders,
                responseBody = if (obj.isNull("responseBody")) null else obj.optString("responseBody"),
                ociErrorCode = if (obj.isNull("ociErrorCode")) null else obj.optString("ociErrorCode"),
                ociErrorMessage = if (obj.isNull("ociErrorMessage")) null else obj.optString("ociErrorMessage"),
                exceptionClass = if (obj.isNull("exceptionClass")) null else obj.optString("exceptionClass"),
                exceptionMessage = if (obj.isNull("exceptionMessage")) null else obj.optString("exceptionMessage"),
                durationMs = obj.optLong("durationMs", 0L)
            )
        } catch (e: Exception) {
            BinBoxLogger.w(TAG, "Failed to parse trace JSON line: ${e.message}")
            null
        }
    }
}
