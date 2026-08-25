package com.inscopelabs.abx.binbox.oci.identity

/**
 * An OCI API key fingerprint, as displayed by the OCI console after a
 * public key is registered — e.g. "aa:bb:cc:...:zz" (16 lowercase-hex
 * octets, colon-separated).
 *
 * This is produced BY Oracle after the user pastes our public key into the
 * console (OCI provisioning doc §12/§13) — it is never derived locally, and
 * must not be confused with [OciKeyManager]'s own SHA-256 public-key digest,
 * which is a different value used only for local key identification.
 */
@JvmInline
value class OciFingerprint(val value: String) {
    init {
        require(FINGERPRINT_REGEX.matches(value)) {
            "'$value' is not a valid OCI API key fingerprint (expected 16 colon-separated hex octets)"
        }
    }

    companion object {
        private val FINGERPRINT_REGEX = Regex("^([0-9a-f]{2}:){15}[0-9a-f]{2}$")

        fun parseOrNull(raw: String): OciFingerprint? {
            val trimmed = raw.trim().lowercase()
            return if (FINGERPRINT_REGEX.matches(trimmed)) OciFingerprint(trimmed) else null
        }
    }
}
