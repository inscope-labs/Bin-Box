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
    val requestBody: String? = null,
    val httpStatusCode: Int? = null,
    val responseHeaders: Map<String, String>? = null,
    val responseBody: String? = null,
    val ociErrorCode: String? = null,
    val ociErrorMessage: String? = null,
    val exceptionClass: String? = null,
    val exceptionMessage: String? = null,
    val durationMs: Long
)
