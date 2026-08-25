package com.inscopelabs.abx.binbox.oci.api

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.oci.auth.OciRequestSigner
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.IOException

/**
 * Signs every outgoing OCI API request per Oracle's request-signing spec
 * (confirmed against docs.oracle.com/en-us/iaas/Content/API/Concepts/signingrequests.htm
 * — see [OciRequestSigner] for the implementation this delegates to).
 *
 * Buffers the request body (if any) to compute `x-content-sha256` /
 * `content-length` for the signing string, then overwrites those headers
 * (plus `content-type`, `host`, `date`) on the outgoing request so the
 * headers Oracle actually receives are byte-for-byte what was signed —
 * a mismatch here (e.g. a converter-set `content-type` charset suffix
 * that differs from what was signed) would make OCI reject the signature
 * server-side even though the request looks fine locally.
 */
class OciSigningInterceptor(
    private val credentialsProvider: () -> OciCredentials?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val credentials = credentialsProvider()
            ?: throw IOException("No OCI credentials available to sign this request")

        val bodyBytes: ByteArray? = original.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
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
            is AppResult.Error -> throw IOException(signResult.error.userMessage)
            AppResult.Loading -> throw IOException("Unexpected loading state signing OCI request")
        }

        val requestBuilder = original.newBuilder()
        signed.allHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }
        requestBuilder.header("Authorization", signed.authorizationHeaderValue)

        return chain.proceed(requestBuilder.build())
    }
}
