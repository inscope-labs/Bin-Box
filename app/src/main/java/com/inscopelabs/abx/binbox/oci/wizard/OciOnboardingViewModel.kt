package com.inscopelabs.abx.binbox.oci.wizard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciCredentialsStore
import com.inscopelabs.abx.binbox.oci.identity.OciFingerprint
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningRepository
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState
import com.inscopelabs.abx.binbox.security.SecureStorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates the OCI onboarding wizard (§9 stage sequence over the §31
 * state machine, persisted via [OciProvisioningRepository] for §32
 * resumability).
 *
 * SCOPE OF THIS PHASE: stages through [OciOnboardingStage.CONNECTION_VERIFICATION]
 * are real — key generation, credential persistence, and the account-info
 * form are all backed by working code below. [OciOnboardingStage.OCI_CONTEXT_DISCOVERY]
 * onward requires the OCI Compute/Network/Identity REST APIs (§15-26),
 * which this phase deliberately does not implement: those endpoints' exact
 * request/response shapes and pagination/versioning behavior aren't
 * something to guess at from a spec doc, and a wrong implementation here
 * would be worse than an honest "not yet built." [advanceContextDiscovery]
 * and everything after it throw [NotImplementedError] until that API
 * client exists as a follow-up phase.
 */
class OciOnboardingViewModel(
    application: Application,
    private val secureStorage: SecureStorageService = SecureStorageService(application)
) : AndroidViewModel(application) {

    private val credentialsStore = OciCredentialsStore(application, secureStorage)
    private val provisioningRepository = OciProvisioningRepository(application)

    private val _stage = MutableStateFlow(OciOnboardingStage.WELCOME)
    val stage: StateFlow<OciOnboardingStage> = _stage.asStateFlow()

    private val _uiState = MutableStateFlow(OciOnboardingUiState())
    val uiState: StateFlow<OciOnboardingUiState> = _uiState.asStateFlow()

    private var session = provisioningRepository.loadOrCreate()

    init {
        // Resume mid-wizard rather than restarting at WELCOME (§32).
        _stage.value = stageFor(session.state)
        credentialsStore.load().let { result ->
            if (result is AppResult.Success && result.data != null) {
                _uiState.update { it.copy(credentials = result.data) }
            }
        }
    }

    fun onEvent(event: OciOnboardingEvent) {
        when (event) {
            is OciOnboardingEvent.GetStarted -> advanceTo(OciOnboardingStage.ACCOUNT_INFORMATION)

            is OciOnboardingEvent.SubmitAccountInfo -> {
                _uiState.update {
                    it.copy(
                        tenancyOcid = event.tenancyOcid,
                        userOcid = event.userOcid,
                        region = event.region
                    )
                }
                advanceTo(OciOnboardingStage.API_KEY_GENERATION)
                persistSessionState(OciProvisioningState.ACCOUNT_REQUIRED)
            }

            is OciOnboardingEvent.GenerateApiKey -> generateApiKey()

            is OciOnboardingEvent.SubmitFingerprint -> submitFingerprint(event.fingerprint)

            is OciOnboardingEvent.VerifyConnection -> verifyConnection()

            OciOnboardingEvent.Cancel -> {
                persistSessionState(OciProvisioningState.CANCELLED)
            }
        }
    }

    private fun generateApiKey() {
        val alias = "oci_api_signing_${session.sessionId}"
        when (val result = OciKeyManager.ensureSigningKey(alias)) {
            is AppResult.Success -> {
                _uiState.update { it.copy(pendingKeyAlias = alias, publicKeyPem = result.data, error = null) }
                advanceTo(OciOnboardingStage.API_KEY_REGISTRATION)
                persistSessionState(OciProvisioningState.API_KEY_REQUIRED)
            }
            is AppResult.Error -> {
                _uiState.update { it.copy(error = result.error.userMessage) }
                BinBoxLogger.e("OciOnboardingViewModel", "API key generation failed: ${result.error.userMessage}")
            }
            AppResult.Loading -> Unit
        }
    }

    private fun submitFingerprint(raw: String) {
        val fingerprint = OciFingerprint.parseOrNull(raw)
        if (fingerprint == null) {
            _uiState.update { it.copy(error = "That doesn't look like a valid OCI fingerprint.") }
            return
        }
        val state = _uiState.value
        val alias = state.pendingKeyAlias
        if (state.tenancyOcid == null || state.userOcid == null || state.region == null || alias == null) {
            _uiState.update { it.copy(error = "Missing account info or key — go back and fill those in first.") }
            return
        }

        val credentials = OciCredentials(
            tenancyOcid = state.tenancyOcid,
            userOcid = state.userOcid,
            fingerprint = fingerprint,
            region = state.region,
            keyAlias = alias
        )

        when (val result = credentialsStore.save(credentials)) {
            is AppResult.Success -> {
                _uiState.update { it.copy(credentials = credentials, error = null) }
                advanceTo(OciOnboardingStage.CONNECTION_VERIFICATION)
                persistSessionState(OciProvisioningState.API_KEY_REGISTERED)
            }
            is AppResult.Error -> _uiState.update { it.copy(error = result.error.userMessage) }
            AppResult.Loading -> Unit
        }
    }

    private fun verifyConnection() {
        val credentials = _uiState.value.credentials
        if (credentials == null) {
            _uiState.update { it.copy(error = "No OCI credentials to verify.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, error = null) }
            // TODO(oci-api-phase-b): call a harmless authenticated OCI request
            // (§14 — e.g. GET /identity/20160918/users/{userOcid}) via the
            // not-yet-built api/OciClient, using OciRequestSigner for the
            // Authorization header. Left unimplemented pending that client
            // — see class kdoc.
            _uiState.update {
                it.copy(
                    isVerifying = false,
                    error = "Connection verification requires the OCI API client (not yet built in this phase)."
                )
            }
        }
    }

    private fun advanceTo(stage: OciOnboardingStage) {
        _stage.value = stage
    }

    private fun persistSessionState(state: OciProvisioningState) {
        session = session.advance(state)
        provisioningRepository.save(session)
    }

    private fun stageFor(state: OciProvisioningState): OciOnboardingStage = when (state) {
        OciProvisioningState.NOT_STARTED -> OciOnboardingStage.WELCOME
        OciProvisioningState.ACCOUNT_REQUIRED -> OciOnboardingStage.API_KEY_GENERATION
        OciProvisioningState.API_KEY_REQUIRED -> OciOnboardingStage.API_KEY_REGISTRATION
        OciProvisioningState.API_KEY_REGISTERED -> OciOnboardingStage.CONNECTION_VERIFICATION
        OciProvisioningState.AUTHENTICATION_VERIFIED -> OciOnboardingStage.OCI_CONTEXT_DISCOVERY
        else -> OciOnboardingStage.OCI_CONTEXT_DISCOVERY // everything past this point is unbuilt (see kdoc)
    }
}

sealed class OciOnboardingEvent {
    data object GetStarted : OciOnboardingEvent()
    data class SubmitAccountInfo(val tenancyOcid: String, val userOcid: String, val region: String) : OciOnboardingEvent()
    data object GenerateApiKey : OciOnboardingEvent()
    data class SubmitFingerprint(val fingerprint: String) : OciOnboardingEvent()
    data object VerifyConnection : OciOnboardingEvent()
    data object Cancel : OciOnboardingEvent()
}

data class OciOnboardingUiState(
    val tenancyOcid: String? = null,
    val userOcid: String? = null,
    val region: String? = null,
    val pendingKeyAlias: String? = null,
    val publicKeyPem: String? = null,
    val credentials: OciCredentials? = null,
    val isVerifying: Boolean = false,
    val error: String? = null
)

private fun MutableStateFlow<OciOnboardingUiState>.update(transform: (OciOnboardingUiState) -> OciOnboardingUiState) {
    value = transform(value)
}
