package com.inscopelabs.abx.binbox.oci.identity

/**
 * The minimum OCI API authentication context (OCI provisioning doc §7).
 *
 * Deliberately does NOT hold the private signing key. That key never leaves
 * [OciKeyManager] / the Android Keystore — this class only carries
 * [keyAlias], a reference to look it up by. This mirrors the doc's warning
 * (§34) against collapsing OCI account identity, OCI API identity, SSH
 * identity, and terminal host identity into one generic credentials object;
 * within API identity itself, the same discipline applies to the signing
 * key specifically.
 */
data class OciCredentials(
    val tenancyOcid: String,
    val userOcid: String,
    val fingerprint: OciFingerprint,
    val region: String,
    val keyAlias: String
)
