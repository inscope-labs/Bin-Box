package com.inscopelabs.abx.binbox.oci.api

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.oci.auth.OciRequestSigner
import com.inscopelabs.abx.binbox.oci.diagnostics.OciStepContext
import com.inscopelabs.abx.binbox.oci.diagnostics.OciTraceRecorder
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.time.Instant
import java.util.UUID

/**
 * Signs every outgoing OCI API request per Oracle's request-signing spec
 * and records raw unredacted call traces into [OciCallTraceStore].
 */
class OciSigningInterceptor(
    private val credentialsProvider: () -> OciCredentials?
) : Interceptor {

    private val tag = "OciSigningInterceptor"
    private val maxPeekBytes = 1024L * 1024L // 1 MB bounded peek

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val timestampUtc = Instant.now().toString()
        val traceId = UUID.randomUUID().toString()
        val (stageId, stepId) = OciStepContext.currentOrUnknown()
        val method = original.method
        val url = original.url.toString()

        val bodyBytes: ByteArray? = original.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
        }
        val requestBodyText = bodyBytes?.let { String(it, Charsets.UTF_8) }

        val credentials = try {
            credentialsProvider() ?: throw IOException("No OCI credentials available to sign this request")
        } catch (e: Throwable) {
            OciTraceRecorder.recordFailure(
                traceId, timestampUtc, stageId, stepId, method, url,
                OciTraceRecorder.extractHeaders(original.headers), requestBodyText, e, 0L
            )
            throw if (e is IOException) e else IOException(e.message, e)
        }

        val requestTarget = original.url.encodedPath +
            if (original.url.encodedQuery != null) "?${original.url.encodedQuery}" else ""

        val signResult = OciRequestSigner.sign(
            credentials = credentials,
            method = original.method,
            requestTargetPath = requestTarget,
            host = original.url.host,
            body = bodyBytes
        )

        val signed = when (signResult) {
            is AppResult.Success -> signResult.data
            is AppResult.Error -> {
                val ex = IOException(signResult.error.userMessage)
                OciTraceRecorder.recordFailure(
                    traceId, timestampUtc, stageId, stepId, method, url,
                    OciTraceRecorder.extractHeaders(original.headers), requestBodyText, ex, 0L
                )
                throw ex
            }
            AppResult.Loading -> {
                val ex = IOException("Unexpected loading state signing OCI request")
                OciTraceRecorder.recordFailure(
                    traceId, timestampUtc, stageId, stepId, method, url,
                    OciTraceRecorder.extractHeaders(original.headers), requestBodyText, ex, 0L
                )
                throw ex
            }
        }

        val requestBuilder = original.newBuilder()
        signed.allHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }
        requestBuilder.header("Authorization", signed.authorizationHeaderValue)
        val signedRequest = requestBuilder.build()
        val finalRequestHeaders = OciTraceRecorder.extractHeaders(signedRequest.headers)

        val startTime = System.currentTimeMillis()
        BinBoxLogger.d(tag, "Dispatching [$stageId/$stepId] $method $url")

        val response = try {
            chain.proceed(signedRequest)
        } catch (e: Throwable) {
            val durationMs = System.currentTimeMillis() - startTime
            OciTraceRecorder.recordFailure(
                traceId, timestampUtc, stageId, stepId, method, url,
                finalRequestHeaders, requestBodyText, e, durationMs
            )
            throw if (e is IOException) e else IOException(e.message, e)
        }

        val durationMs = System.currentTimeMillis() - startTime
        val rawResponseBody = runCatching {
            response.peekBody(maxPeekBytes).string()
        }.getOrNull()

        OciTraceRecorder.recordSuccess(
            traceId = traceId,
            timestampUtc = timestampUtc,
            stageId = stageId,
            stepId = stepId,
            method = method,
            url = url,
            requestHeaders = finalRequestHeaders,
            requestBody = requestBodyText,
            response = response,
            rawResponseBody = rawResponseBody,
            durationMs = durationMs
        )

        return response
    }
}


