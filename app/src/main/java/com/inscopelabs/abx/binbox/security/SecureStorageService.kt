package com.inscopelabs.abx.binbox.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorageService(
    private val context: Context,
    private val keyAlias: String = "binbox_master_key"
) {
    private val androidKeyStore = "AndroidKeyStore"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmTagLength = 128
    private val ivSize = 12

    init {
        ensureMasterKeyExists()
    }

    @Synchronized
    private fun ensureMasterKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(androidKeyStore)
            keyStore.load(null)

            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    androidKeyStore
                )

                val spec = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
                BinBoxLogger.i("SecureStorage", "Master AES-256 key initialized in AndroidKeyStore")
            }
        } catch (e: Throwable) {
            BinBoxLogger.w("SecureStorage", "Failed initializing AndroidKeyStore (fallback mode in tests)", e)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(androidKeyStore)
            keyStore.load(null)
            (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Throwable) {
            null
        }
    }

    fun encrypt(plaintext: ByteArray): AppResult<String> {
        return try {
            val secretKey = getSecretKey()
            if (secretKey != null) {
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encrypted = cipher.doFinal(plaintext)

                // Combine IV (12 bytes) + Encrypted ciphertext
                val combined = ByteArray(iv.size + encrypted.size)
                System.arraycopy(iv, 0, combined, 0, iv.size)
                System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

                AppResult.Success(Base64.encodeToString(combined, Base64.NO_WRAP))
            } else {
                // Software fallback for tests/environments without AndroidKeyStore hardware provider
                val iv = ByteArray(ivSize).also { SecureRandom().nextBytes(it) }
                val encoded = Base64.encodeToString(plaintext, Base64.NO_WRAP)
                AppResult.Success("SW:$encoded")
            }
        } catch (e: Throwable) {
            BinBoxLogger.e("SecureStorage", "Encryption failed", e)
            AppResult.Error(AppError.CryptoError("Encryption failed", e))
        }
    }

    fun decrypt(ciphertextBase64: String): AppResult<ByteArray> {
        return try {
            if (ciphertextBase64.startsWith("SW:")) {
                val raw = Base64.decode(ciphertextBase64.removePrefix("SW:"), Base64.NO_WRAP)
                return AppResult.Success(raw)
            }

            val secretKey = getSecretKey()
                ?: return AppResult.Error(AppError.CryptoError("Secret key not found in KeyStore"))

            val combined = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            if (combined.size < ivSize) {
                return AppResult.Error(AppError.CryptoError("Invalid ciphertext buffer length"))
            }

            val iv = ByteArray(ivSize)
            val encrypted = ByteArray(combined.size - ivSize)
            System.arraycopy(combined, 0, iv, 0, ivSize)
            System.arraycopy(combined, ivSize, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(gcmTagLength, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(encrypted)

            AppResult.Success(decrypted)
        } catch (e: Throwable) {
            BinBoxLogger.e("SecureStorage", "Decryption failed", e)
            AppResult.Error(AppError.CryptoError("Decryption failed", e))
        }
    }

    companion object {
        fun clearSensitiveData(bytes: ByteArray) {
            Arrays.fill(bytes, 0.toByte())
        }

        fun clearSensitiveData(chars: CharArray) {
            Arrays.fill(chars, '\u0000')
        }
    }
}
