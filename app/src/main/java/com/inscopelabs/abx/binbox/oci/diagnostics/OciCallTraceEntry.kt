package com.inscopelabs.abx.binbox.oci.diagnostics

/**
 * Structured, raw, unredacted record of an individual OCI API request/response interaction.
 */
data class OciCallTraceEntry(
    val id: String,
    val timestampUtc: String,
    val stageId: String,
    val stepId: String,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val httpStatusCode: Int?,
    val responseHeaders: Map<String, String>?,
    val responseBody: String?,
    val ociErrorCode: String?,
    val ociErrorMessage: String?,
    val exceptionClass: String?,
    val exceptionMessage: String?,
    val durationMs: Long
)
