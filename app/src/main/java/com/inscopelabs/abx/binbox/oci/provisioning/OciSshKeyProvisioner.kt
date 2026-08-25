package com.inscopelabs.abx.binbox.oci.provisioning

import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.repository.IKeyRepository

/**
 * Generates the SSH key pair that will be attached to the provisioned VM
 * (doc §20) — distinct from [com.inscopelabs.abx.binbox.oci.identity.OciKeyManager],
 * which generates the OCI *API* signing key.
 *
 * This is fully buildable now, unlike the rest of §15-26: it doesn't touch
 * OCI's API at all. Deliberately reuses [IKeyRepository] (Phase 9's existing,
 * already-encrypted SSH key storage) rather than inventing a parallel
 * OCI-specific key store — §20's "private key protected via Keystore" and
 * "public key extracted for the instance-metadata request" requirements are
 * already satisfied by what `KeyRepositoryImpl` does for every other SSH
 * key in the app. The one piece this can't finish yet is actually attaching
 * the public key to instance metadata, since that's an OCI API call (§21,
 * deferred — see the Phase A agent report).
 */
class OciSshKeyProvisioner(
    private val keyRepository: IKeyRepository
) {
    /**
     * Generates a new RSA-2048 key pair titled for this session and
     * persists it via [IKeyRepository]. Returns the new key's row id —
     * callers store that id (as a string) in
     * [OciProvisioningSession.sshKeyAlias] and pass it as `keyId` to
     * [com.inscopelabs.abx.binbox.oci.terminal.OciShellHost.toConnectionProfile].
     *
     * Note: [OciProvisioningSession.sshKeyAlias] is named to parallel
     * [com.inscopelabs.abx.binbox.oci.identity.OciCredentials.keyAlias], but
     * holds a different kind of reference — an [IKeyRepository] row id
     * (Long, stringified), not an AndroidKeystore alias. The OCI API
     * signing key and the VM SSH key use two different storage schemes
     * (Keystore vs. encrypted Room row) because they have different
     * requirements: the signing key never needs its raw bytes and stays in
     * hardware; the VM key must hand raw PEM to JSch and so is stored the
     * same way every other SSH host key in this app is.
     */
    suspend fun generateForSession(sessionId: String): AppResult<SshKey> {
        val title = "Oracle Cloud VM ($sessionId)"
        return when (val result = keyRepository.generateRsaKeyPair(title)) {
            is AppResult.Success -> result
            is AppResult.Error -> result
            AppResult.Loading -> AppResult.Error(
                AppError.UnexpectedError("Unexpected loading state generating VM SSH key")
            )
        }
    }
}
