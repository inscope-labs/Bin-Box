package com.inscopelabs.abx.binbox.security

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult

/**
 * Encrypt/decrypt boundary for individual credential fields (SSH private
 * keys, host passwords, key passphrases) persisted via Room — Phase 9
 * (Security, Reliability & Device Integration).
 *
 * Room's schema is untouched by this: the relevant columns stay plain TEXT.
 * What changes is what gets written into them — ciphertext produced by
 * [SecureStorageService] (AES-256-GCM, AndroidKeyStore-backed) instead of
 * plaintext.
 *
 * Ciphertext is tagged with [CIPHERTEXT_PREFIX] so [decryptField] can tell
 * "encrypted by this code" apart from "plaintext written before this existed"
 * without guessing. Untagged values are returned unchanged rather than
 * discarded — they're transparently upgraded to ciphertext the next time the
 * owning record is saved.
 */
object CredentialCrypto {

    private const val CIPHERTEXT_PREFIX = "ENC1:"

    /** Encrypts [plaintext] for storage. Null/empty pass through unchanged (no credential to protect). */
    fun encryptField(storage: SecureStorageService, plaintext: String?): String? {
        if (plaintext.isNullOrEmpty()) return plaintext
        return when (val result = storage.encrypt(plaintext.toByteArray(Charsets.UTF_8))) {
            is AppResult.Success -> CIPHERTEXT_PREFIX + result.data
            else -> {
                // Encryption failing is a real problem, but silently dropping the
                // credential is worse than persisting it in plaintext once and
                // logging loudly about it — surfaced here rather than swallowed.
                BinBoxLogger.e("CredentialCrypto", "Field encryption failed; persisting value unencrypted")
                plaintext
            }
        }
    }

    /**
     * Decrypts a value previously produced by [encryptField]. A value with no
     * [CIPHERTEXT_PREFIX] is treated as a legacy plaintext row and returned as-is.
     * A tagged value that fails to decrypt (corruption, key rotation, etc.)
     * returns null rather than surfacing raw ciphertext as if it were the secret.
     */
    fun decryptField(storage: SecureStorageService, stored: String?): String? {
        if (stored.isNullOrEmpty()) return stored
        if (!stored.startsWith(CIPHERTEXT_PREFIX)) return stored

        val ciphertext = stored.removePrefix(CIPHERTEXT_PREFIX)
        return when (val result = storage.decrypt(ciphertext)) {
            is AppResult.Success -> String(result.data, Charsets.UTF_8)
            else -> {
                BinBoxLogger.e("CredentialCrypto", "Field decryption failed; value may be corrupted or key rotated")
                null
            }
        }
    }
}
