package com.inscopelabs.abx.binbox.oci.wizard

import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciCredentialsStore
import com.inscopelabs.abx.binbox.oci.identity.OciFingerprint
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager

/**
 * Module responsible for OCI account info validation, key pair generation, and fingerprint registration.
 */
class OciAccountConfigHandler {

    fun parseAndApplyConfig(raw: String, current: OciOnboardingUiState): OciOnboardingUiState {
        val parsed = OciConfigParser.parse(raw)
        BinBoxLogger.i(TAG, "Imported OCI config: tenancy=${parsed.tenancyOcid != null}, user=${parsed.userOcid != null}")
        return current.copy(
            tenancyOcid = parsed.tenancyOcid ?: current.tenancyOcid,
            userOcid = parsed.userOcid ?: current.userOcid,
            region = parsed.region ?: current.region,
            pendingFingerprint = parsed.fingerprint ?: current.pendingFingerprint,
            error = null
        )
    }

    fun normalizeAccountInfo(tenancy: String, user: String, region: String, current: OciOnboardingUiState): OciOnboardingUiState {
        val normalizedRegion = OciRegionHelper.normalizeRegion(region)
        BinBoxLogger.i(TAG, "Submitted account info: region normalized to $normalizedRegion")
        return current.copy(tenancyOcid = tenancy, userOcid = user, region = normalizedRegion, error = null)
    }

    fun generateApiKey(
        sessionId: String,
        onSuccess: (alias: String, publicKeyPem: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val alias = "oci_api_signing_$sessionId"
        BinBoxLogger.i(TAG, "Generating API signing key with alias $alias")
        when (val result = OciKeyManager.ensureSigningKey(alias)) {
            is AppResult.Success -> {
                BinBoxLogger.i(TAG, "API signing key generated successfully")
                onSuccess(alias, result.data)
            }
            is AppResult.Error -> {
                BinBoxLogger.e(TAG, "Failed generating signing key: ${result.error.userMessage}")
                onError(result.error.userMessage)
            }
            AppResult.Loading -> Unit
        }
    }

    fun submitFingerprint(
        raw: String,
        state: OciOnboardingUiState,
        sessionId: String,
        credentialsStore: OciCredentialsStore,
        onSuccess: (OciCredentials, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val fingerprint = OciFingerprint.parseOrNull(raw) ?: run {
            onError("Invalid fingerprint. Format: aa:bb:cc:...:zz")
            return
        }
        val alias = state.pendingKeyAlias ?: state.credentials?.keyAlias ?: "oci_api_signing_$sessionId"
        if (state.tenancyOcid == null || state.userOcid == null || state.region == null) {
            onError("Missing account info or key — return to step 1.")
            return
        }

        val credentials = OciCredentials(state.tenancyOcid, state.userOcid, fingerprint, state.region, alias)
        BinBoxLogger.i(TAG, "Saving OCI credentials with fingerprint ${fingerprint.value}")
        when (val result = credentialsStore.save(credentials)) {
            is AppResult.Success -> onSuccess(credentials, fingerprint.value)
            is AppResult.Error -> {
                BinBoxLogger.e(TAG, "Failed saving credentials: ${result.error.userMessage}")
                onError(result.error.userMessage)
            }
            AppResult.Loading -> Unit
        }
    }

    companion object {
        private const val TAG = "OciAccountConfigHandler"
    }
}
