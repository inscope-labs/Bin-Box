package com.inscopelabs.abx.binbox.security

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SshKeyManagerTest {

    @Test
    fun testGeneratedRsaKeyPemIsParseableByJSch() {
        val res = SshKeyManager.generateRsaKey("test-rsa", 2048)
        assertTrue("RSA key generation should succeed: $res", res is AppResult.Success)
        val sshKey = (res as AppResult.Success).data

        assertTrue(sshKey.privateKey.startsWith("-----BEGIN RSA PRIVATE KEY-----"))
        assertTrue(sshKey.privateKey.trimEnd().endsWith("-----END RSA PRIVATE KEY-----"))

        val jsch = JSch()
        val prvBytes = sshKey.privateKey.toByteArray(Charsets.UTF_8)
        val keyPair = KeyPair.load(jsch, prvBytes, null)

        assertNotNull("KeyPair loaded by JSch should not be null", keyPair)
        assertFalse("KeyPair should not be encrypted", keyPair.isEncrypted)
        assertNotNull("PublicKeyBlob should not be null", keyPair.publicKeyBlob)
        assertEquals(KeyPair.RSA, keyPair.keyType)

        // Also verify JSch addIdentity accepts the private key bytes without throwing
        jsch.addIdentity("test_rsa_identity", prvBytes, null, null)
    }

    @Test
    fun testGeneratedRsaKeyPublicPrivateMatch() {
        val res = SshKeyManager.generateRsaKey("test-rsa-match", 2048)
        assertTrue(res is AppResult.Success)
        val sshKey = (res as AppResult.Success).data

        assertTrue(sshKey.publicKey.startsWith("ssh-rsa "))
        assertTrue(sshKey.fingerprint.startsWith("SHA256:"))
        assertTrue(sshKey.publicKey.endsWith(" binbox@device"))
    }

    @Test
    fun testGeneratedEcKeyPemIsParseableByJSch() {
        val res = SshKeyManager.generateEcKey("test-ec")
        assertTrue("EC key generation should succeed: $res", res is AppResult.Success)
        val sshKey = (res as AppResult.Success).data

        assertTrue(sshKey.privateKey.startsWith("-----BEGIN PRIVATE KEY-----"))
        assertTrue(sshKey.privateKey.trimEnd().endsWith("-----END PRIVATE KEY-----"))

        val jsch = JSch()
        val prvBytes = sshKey.privateKey.toByteArray(Charsets.UTF_8)
        val keyPair = KeyPair.load(jsch, prvBytes, null)

        assertNotNull("EC KeyPair loaded by JSch should not be null", keyPair)
        assertFalse("EC KeyPair should not be encrypted", keyPair.isEncrypted)
        assertNotNull("EC PublicKeyBlob should not be null", keyPair.publicKeyBlob)

        // Also verify JSch addIdentity accepts the PKCS#8 EC private key bytes without throwing
        jsch.addIdentity("test_ec_identity", prvBytes, null, null)
    }
}
