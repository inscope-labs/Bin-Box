package com.inscopelabs.abx.binbox.oci.auth

import android.util.Base64
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import java.security.MessageDigest
import java.security.Signature
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Produces OCI's RSA-SHA256 request signature and the resulting
 * `Authorization` header (OCI provisioning doc §8).
 *
 * The API client (§8: "The API client must not implement signing logic
 * itself") must call this rather than sign requests inline. Signing is
 * performed via [Signature] against the [OciKeyManager]-held private key
 * handle — raw private key bytes are never read into this class or logged.
 *
 * Header set follows OCI's documented scheme:
 *  - GET / DELETE / HEAD:  (request-target) host date
 *  - POST / PUT / PATCH:   (request-target) host date x-content-sha256 content-length content-type
 */
object OciRequestSigner {

    private val RFC_1123 = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    /**
     * @param method HTTP method, uppercase (GET/POST/PUT/PATCH/DELETE)
     * @param requestTargetPath path + query, e.g. "/20160918/instances?compartmentId=..."
     * @param host request host, e.g. "iaas.us-ashburn-1.oraclecloud.com"
     * @param body raw request body bytes; required (may be empty) for POST/PUT/PATCH, ignored otherwise
     */
    fun sign(
        credentials: OciCredentials,
        method: String,
        requestTargetPath: String,
        host: String,
        body: ByteArray? = null
    ): AppResult<OciSignatureHeaders> {
        val upperMethod = method.uppercase()
        val bodySigned = upperMethod in BODY_SIGNED_METHODS

        val headers = LinkedHashMap<String, String>()
        headers["date"] = synchronized(RFC_1123) { RFC_1123.format(Date()) }
        headers["host"] = host

        if (bodySigned) {
            val bodyBytes = body ?: ByteArray(0)
            val digest = MessageDigest.getInstance("SHA-256").digest(bodyBytes)
            headers["x-content-sha256"] = Base64.encodeToString(digest, Base64.NO_WRAP)
            headers["content-length"] = bodyBytes.size.toString()
            headers["content-type"] = "application/json"
        }

        val signedHeaderNames = buildList {
            add("(request-target)")
            add("host")
            add("date")
            if (bodySigned) {
                add("x-content-sha256")
                add("content-length")
                add("content-type")
            }
        }

        val requestTargetLine = "${upperMethod.lowercase()} $requestTargetPath"
        val signingString = signedHeaderNames.joinToString("\n") { name ->
            if (name == "(request-target)") "(request-target): $requestTargetLine"
            else "$name: ${headers[name]}"
        }

        val keyHandleResult = OciKeyManager.getSigningKeyHandle(credentials.keyAlias)
        val privateKey = when (keyHandleResult) {
            is AppResult.Success -> keyHandleResult.data
            is AppResult.Error -> return AppResult.Error(keyHandleResult.error)
            AppResult.Loading -> return AppResult.Error(AppError.UnexpectedError("Unexpected loading state resolving OCI signing key"))
        }

        val signatureBytes = try {
            Signature.getInstance("SHA256withRSA").run {
                initSign(privateKey)
                update(signingString.toByteArray(Charsets.UTF_8))
                sign()
            }
        } catch (e: Throwable) {
            BinBoxLogger.e("OciRequestSigner", "RSA-SHA256 signing failed", e)
            return AppResult.Error(AppError.AuthError.OciAuthenticationFailed("request signing failed", e))
        }

        val signatureB64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
        val keyId = "${credentials.tenancyOcid}/${credentials.userOcid}/${credentials.fingerprint.value}"
        val authHeader = buildString {
            append("Signature version=\"1\",")
            append("keyId=\"$keyId\",")
            append("algorithm=\"rsa-sha256\",")
            append("headers=\"${signedHeaderNames.joinToString(" ")}\",")
            append("signature=\"$signatureB64\"")
        }

        return AppResult.Success(
            OciSignatureHeaders(
                allHeaders = headers,
                signedHeaderNames = signedHeaderNames,
                authorizationHeaderValue = authHeader
            )
        )
    }

    private val BODY_SIGNED_METHODS = setOf("POST", "PUT", "PATCH")
}
