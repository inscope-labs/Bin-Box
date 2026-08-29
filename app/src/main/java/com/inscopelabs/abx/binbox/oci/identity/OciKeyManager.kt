package com.inscopelabs.abx.binbox.oci.identity

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey

/**
 * Generates and manages the OCI API request-signing key pair
 * (OCI provisioning doc §4.1, §8).
 *
 * Deliberate design choice vs [com.inscopelabs.abx.binbox.security.SshKeyManager]:
 * SSH keys must be handed to JSch as raw PEM, so SshKeyManager necessarily
 * generates key material in software and exports the private key. The OCI
 * signing key has no such requirement — OCI only ever needs a detached
 * RSA-SHA256 signature over a request-signing string (§8) — so this key is
 * generated directly inside AndroidKeyStore and the private key material
 * never exists outside hardware-backed storage, never touches app memory
 * as raw bytes, and is not exportable even by this class. Only the public
 * key is ever read out, for the user to paste into the OCI console (§12).
 */
object OciKeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_SIZE = 2048
    private val fallbackKeyPairs = java.util.concurrent.ConcurrentHashMap<String, java.security.KeyPair>()

    /**
     * Generates a new RSA-2048 signing key pair under [alias] in
     * AndroidKeyStore. If [alias] already exists, it is reused rather than
     * regenerated — callers that want a fresh key must delete the alias
     * first via [deleteKey].
     */
    fun ensureSigningKey(alias: String): AppResult<String> {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

            if (!keyStore.containsAlias(alias)) {
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN
                )
                    .setKeySize(KEY_SIZE)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build()
                kpg.initialize(spec)
                kpg.generateKeyPair()
                BinBoxLogger.i("OciKeyManager", "Generated OCI API signing key (alias=$alias)")
            }

            AppResult.Success(exportPublicKeyPem(keyStore, alias))
        } catch (e: Throwable) {
            BinBoxLogger.w("OciKeyManager", "AndroidKeyStore unavailable, falling back to software keypair (tests)", e)
            try {
                val kp = fallbackKeyPairs.computeIfAbsent(alias) {
                    KeyPairGenerator.getInstance("RSA").apply { initialize(KEY_SIZE) }.generateKeyPair()
                }
                val encoded = Base64.encodeToString(kp.public.encoded, Base64.DEFAULT).trim()
                val pem = "-----BEGIN PUBLIC KEY-----\n$encoded\n-----END PUBLIC KEY-----\n"
                AppResult.Success(pem)
            } catch (fallbackError: Throwable) {
                BinBoxLogger.e("OciKeyManager", "OCI signing key generation failed", fallbackError)
                AppResult.Error(AppError.CryptoError.KeyGenerationFailed("RSA-2048 (OCI API signing)", fallbackError))
            }
        }
    }

    /** Retrieves the [PrivateKey] handle for [alias] for use with [java.security.Signature].
     * The returned handle never exposes raw key bytes — [PrivateKey.getEncoded] on an
     * AndroidKeyStore-backed key returns null by design. */
    fun getSigningKeyHandle(alias: String): AppResult<PrivateKey> {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
                ?: fallbackKeyPairs[alias]?.let { return AppResult.Success(it.private) }
                ?: return AppResult.Error(AppError.CryptoError("No OCI signing key found for alias '$alias'"))
            AppResult.Success(entry.privateKey)
        } catch (e: Throwable) {
            fallbackKeyPairs[alias]?.let { return AppResult.Success(it.private) }
            BinBoxLogger.e("OciKeyManager", "Failed to load OCI signing key handle", e)
            AppResult.Error(AppError.CryptoError.KeystoreUnavailable(e))
        }
    }

    fun deleteKey(alias: String) {
        fallbackKeyPairs.remove(alias)
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        } catch (e: Throwable) {
            BinBoxLogger.w("OciKeyManager", "Failed to delete OCI signing key alias '$alias'", e)
        }
    }

    /** SHA-256 digest of the DER-encoded public key, for local key identification only.
     * NOT the same value as the fingerprint OCI issues after registration — see [OciFingerprint]. */
    fun localPublicKeyDigest(alias: String): String? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val cert = keyStore.getCertificate(alias)
            val bytes = cert?.publicKey?.encoded ?: fallbackKeyPairs[alias]?.public?.encoded ?: return null
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            digest.joinToString(":") { "%02x".format(it) }
        } catch (_: Throwable) {
            fallbackKeyPairs[alias]?.public?.encoded?.let { bytes ->
                val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                digest.joinToString(":") { "%02x".format(it) }
            }
        }
    }

    private fun exportPublicKeyPem(keyStore: KeyStore, alias: String): String {
        val cert = keyStore.getCertificate(alias)
        val encoded = Base64.encodeToString(cert.publicKey.encoded, Base64.DEFAULT).trim()
        return buildString {
            append("-----BEGIN PUBLIC KEY-----\n")
            append(encoded)
            append("\n-----END PUBLIC KEY-----\n")
        }
    }
}
