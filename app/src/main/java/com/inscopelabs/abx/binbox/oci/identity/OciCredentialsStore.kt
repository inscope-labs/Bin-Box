package com.inscopelabs.abx.binbox.oci.identity

import android.content.Context
import android.content.SharedPreferences
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.security.CredentialCrypto
import com.inscopelabs.abx.binbox.security.SecureStorageService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Secure persistence for OCI configuration (OCI provisioning doc §7,
 * "identity/OciCredentialsStore.kt").
 *
 * Note what this does and doesn't store: [OciCredentials] itself contains
 * no private key material — the signing key never leaves AndroidKeyStore
 * (see [OciKeyManager]) and this store only ever holds a reference
 * ([OciCredentials.keyAlias]) to it. What's persisted here (tenancy/user
 * OCIDs, fingerprint, region, key alias) is account-identifying but not a
 * bearer secret; it is still routed through [CredentialCrypto] /
 * [SecureStorageService] for defense in depth and to keep the storage
 * pattern consistent with SSH host credentials (Phase 9).
 *
 * Deliberately backed by SharedPreferences rather than a new Room table:
 * this is a single active OCI profile, not a relational record with
 * foreign keys into hosts/sessions, so a Room migration isn't warranted
 * yet. §7's "support multiple future OCI profiles if required" is left as
 * a follow-up — this store is single-profile for now, documented as such.
 */
class OciCredentialsStore(
    context: Context,
    private val secureStorage: SecureStorageService
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(StoredCredentials::class.java)

    fun save(credentials: OciCredentials): AppResult<Unit> {
        return try {
            val stored = StoredCredentials(
                tenancyOcid = credentials.tenancyOcid,
                userOcid = credentials.userOcid,
                fingerprint = credentials.fingerprint.value,
                region = credentials.region,
                keyAlias = credentials.keyAlias
            )
            val json = adapter.toJson(stored)
            val encrypted = CredentialCrypto.encryptField(secureStorage, json)
                ?: return AppResult.Error(AppError.CryptoError("Failed to encrypt OCI credentials"))
            prefs.edit().putString(KEY_BLOB, encrypted).apply()
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("OciCredentialsStore", "Failed to save OCI credentials", e)
            AppResult.Error(AppError.IoError("Failed to save OCI credentials", e))
        }
    }

    fun load(): AppResult<OciCredentials?> {
        return try {
            val encrypted = prefs.getString(KEY_BLOB, null)
                ?: return AppResult.Success(null)
            val json = CredentialCrypto.decryptField(secureStorage, encrypted)
                ?: return AppResult.Error(AppError.CryptoError("Failed to decrypt stored OCI credentials"))
            val stored = adapter.fromJson(json)
                ?: return AppResult.Error(AppError.IoError("Stored OCI credentials are corrupted"))
            val fingerprint = OciFingerprint.parseOrNull(stored.fingerprint)
                ?: return AppResult.Error(AppError.IoError("Stored OCI fingerprint is invalid"))
            AppResult.Success(
                OciCredentials(
                    tenancyOcid = stored.tenancyOcid,
                    userOcid = stored.userOcid,
                    fingerprint = fingerprint,
                    region = stored.region,
                    keyAlias = stored.keyAlias
                )
            )
        } catch (e: Throwable) {
            BinBoxLogger.e("OciCredentialsStore", "Failed to load OCI credentials", e)
            AppResult.Error(AppError.IoError("Failed to load OCI credentials", e))
        }
    }

    fun isConfigured(): Boolean = prefs.contains(KEY_BLOB)

    /** Clears stored credentials. Does NOT delete the signing key itself —
     * callers that want full teardown must also call [OciKeyManager.deleteKey]. */
    fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private data class StoredCredentials(
        val tenancyOcid: String,
        val userOcid: String,
        val fingerprint: String,
        val region: String,
        val keyAlias: String
    )

    companion object {
        private const val PREFS_NAME = "oci_credentials_store"
        private const val KEY_BLOB = "credentials_blob"
    }
}
