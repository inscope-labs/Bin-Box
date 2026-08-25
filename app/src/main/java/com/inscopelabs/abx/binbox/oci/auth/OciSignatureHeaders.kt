package com.inscopelabs.abx.binbox.oci.auth

/**
 * The HTTP headers OCI's request-signing scheme requires for a given
 * method, plus the resulting Authorization header value.
 *
 * OCI signs a fixed set of pseudo/real headers depending on method:
 *  - GET / DELETE:      (request-target) host date
 *  - POST / PUT / PATCH: (request-target) host date x-content-sha256 content-length content-type
 *
 * [signedHeaderNames] lists exactly which of [allHeaders] were included in
 * the signature, in signing order — this must match the `headers=` field
 * emitted in the Authorization header, or OCI will reject the request.
 */
data class OciSignatureHeaders(
    val allHeaders: Map<String, String>,
    val signedHeaderNames: List<String>,
    val authorizationHeaderValue: String
)
