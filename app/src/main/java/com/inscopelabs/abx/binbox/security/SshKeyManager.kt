package com.inscopelabs.abx.binbox.security

import android.util.Base64
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.SshKey
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec

object SshKeyManager {

    fun generateRsaKey(title: String, keySize: Int = 2048): AppResult<SshKey> {
        return try {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(keySize)
            val kp = kpg.generateKeyPair()

            val privateKeyPem = buildString {
                append("-----BEGIN RSA PRIVATE KEY-----\n")
                append(Base64.encodeToString(pkcs8ToPkcs1(kp.private.encoded), Base64.DEFAULT).trim())
                append("\n-----END RSA PRIVATE KEY-----\n")
            }

            val rsaPub = kp.public as RSAPublicKey
            val byteOs = ByteArrayOutputStream()
            val dos = DataOutputStream(byteOs)
            val typeBytes = "ssh-rsa".toByteArray(Charsets.UTF_8)
            dos.writeInt(typeBytes.size)
            dos.write(typeBytes)

            val exp = rsaPub.publicExponent.toByteArray()
            dos.writeInt(exp.size)
            dos.write(exp)

            val mod = rsaPub.modulus.toByteArray()
            dos.writeInt(mod.size)
            dos.write(mod)
            dos.close()

            val pubKeyBytes = byteOs.toByteArray()
            val pubKeyBase64 = Base64.encodeToString(pubKeyBytes, Base64.NO_WRAP)
            val fullPublicKey = "ssh-rsa $pubKeyBase64 binbox@device"

            val fingerprint = computeFingerprint(pubKeyBytes)

            val formattedTitle = title.ifBlank { "id_rsa_${keySize}_" + System.currentTimeMillis().toString().takeLast(4) }
            val key = SshKey(
                title = formattedTitle,
                keyType = "RSA $keySize-bit",
                publicKey = fullPublicKey,
                privateKey = privateKeyPem,
                fingerprint = fingerprint,
                createdAt = System.currentTimeMillis()
            )

            BinBoxLogger.i("SshKeyManager", "Generated RSA Key: $formattedTitle ($keySize-bit)")
            AppResult.Success(key)
        } catch (e: Throwable) {
            BinBoxLogger.e("SshKeyManager", "RSA Key generation failed", e)
            AppResult.Error(AppError.CryptoError("RSA key generation failed", e))
        }
    }

    fun generateEcKey(title: String, curveName: String = "secp256r1"): AppResult<SshKey> {
        return try {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec(curveName))
            val kp = kpg.generateKeyPair()

            val privateKeyPem = buildString {
                append("-----BEGIN PRIVATE KEY-----\n")
                append(Base64.encodeToString(kp.private.encoded, Base64.DEFAULT).trim())
                append("\n-----END PRIVATE KEY-----\n")
            }

            val ecPub = kp.public as ECPublicKey
            val w = ecPub.w
            val x = w.affineX.toByteArray()
            val y = w.affineY.toByteArray()

            // OpenSSH ecdsa-sha2-nistp256 format
            val byteOs = ByteArrayOutputStream()
            val dos = DataOutputStream(byteOs)
            val typeBytes = "ecdsa-sha2-nistp256".toByteArray(Charsets.UTF_8)
            val curveBytes = "nistp256".toByteArray(Charsets.UTF_8)

            dos.writeInt(typeBytes.size)
            dos.write(typeBytes)
            dos.writeInt(curveBytes.size)
            dos.write(curveBytes)

            // Q point format: 0x04 + 32-byte X + 32-byte Y
            val qPoint = ByteArrayOutputStream()
            qPoint.write(0x04)
            qPoint.write(padToLength(x, 32))
            qPoint.write(padToLength(y, 32))

            val qBytes = qPoint.toByteArray()
            dos.writeInt(qBytes.size)
            dos.write(qBytes)
            dos.close()

            val pubKeyBytes = byteOs.toByteArray()
            val pubKeyBase64 = Base64.encodeToString(pubKeyBytes, Base64.NO_WRAP)
            val fullPublicKey = "ecdsa-sha2-nistp256 $pubKeyBase64 binbox@device"

            val fingerprint = computeFingerprint(pubKeyBytes)

            val formattedTitle = title.ifBlank { "id_ecdsa_" + System.currentTimeMillis().toString().takeLast(4) }
            val key = SshKey(
                title = formattedTitle,
                keyType = "ECDSA 256-bit",
                publicKey = fullPublicKey,
                privateKey = privateKeyPem,
                fingerprint = fingerprint,
                createdAt = System.currentTimeMillis()
            )

            BinBoxLogger.i("SshKeyManager", "Generated ECDSA Key: $formattedTitle")
            AppResult.Success(key)
        } catch (e: Throwable) {
            BinBoxLogger.e("SshKeyManager", "ECDSA Key generation failed", e)
            AppResult.Error(AppError.CryptoError("ECDSA key generation failed", e))
        }
    }

    private fun padToLength(array: ByteArray, targetLength: Int): ByteArray {
        if (array.size == targetLength) return array
        if (array.size > targetLength) {
            // Trim leading sign zero if present
            return array.copyOfRange(array.size - targetLength, array.size)
        }
        val padded = ByteArray(targetLength)
        System.arraycopy(array, 0, padded, targetLength - array.size, array.size)
        return padded
    }

    fun computeFingerprint(keyBytes: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(keyBytes)
            "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP).trimEnd('=')
        } catch (_: Throwable) {
            "SHA256:unknown"
        }
    }

    /**
     * Extracts the traditional PKCS#1 RSAPrivateKey DER bytes from a PKCS#8-encoded
     * key (what JCA's PrivateKey.getEncoded() always returns for RSA). The PKCS#8
     * structure is SEQUENCE(version INTEGER, AlgorithmIdentifier SEQUENCE, OCTET
     * STRING(<the PKCS#1 DER, verbatim>)) — this walks that structure and returns
     * the OCTET STRING content, which is byte-for-byte what belongs under a
     * "-----BEGIN RSA PRIVATE KEY-----" header.
     */
    private fun pkcs8ToPkcs1(pkcs8: ByteArray): ByteArray {
        var pos = 0
        fun readLen(): Int {
            val first = pkcs8[pos++].toInt() and 0xFF
            if (first < 0x80) return first
            var len = 0
            repeat(first and 0x7F) { len = (len shl 8) or (pkcs8[pos++].toInt() and 0xFF) }
            return len
        }
        fun skipValue() {
            val len = readLen()
            pos += len
        }
        require((pkcs8[pos++].toInt() and 0xFF) == 0x30) { "PKCS#8: expected outer SEQUENCE" }
        readLen()
        require((pkcs8[pos++].toInt() and 0xFF) == 0x02) { "PKCS#8: expected version INTEGER" }
        skipValue()
        require((pkcs8[pos++].toInt() and 0xFF) == 0x30) { "PKCS#8: expected AlgorithmIdentifier SEQUENCE" }
        skipValue()
        require((pkcs8[pos++].toInt() and 0xFF) == 0x04) { "PKCS#8: expected OCTET STRING" }
        val len = readLen()
        return pkcs8.copyOfRange(pos, pos + len)
    }
}
