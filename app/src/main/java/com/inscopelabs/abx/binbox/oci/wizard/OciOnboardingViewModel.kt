package com.inscopelabs.abx.binbox.oci.wizard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.database.AppDatabase
import com.inscopelabs.abx.binbox.data.repository.HostRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.KeyRepositoryImpl
import com.inscopelabs.abx.binbox.oci.api.OciApiConfig
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciCredentialsStore
import com.inscopelabs.abx.binbox.oci.identity.OciFingerprint
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import com.inscopelabs.abx.binbox.oci.provisioning.*
import com.inscopelabs.abx.binbox.security.SecureStorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Orchestrates the OCI onboarding wizard stages over the provisioning state machine.
 */
class OciOnboardingViewModel @JvmOverloads constructor(
    application: Application,
    private val secureStorage: SecureStorageService = SecureStorageService(application)
) : AndroidViewModel(application) {

    private val credentialsStore = OciCredentialsStore(application, secureStorage)
    private val provisioningRepository = OciProvisioningRepository(application)
    private val keyRepository = KeyRepositoryImpl(AppDatabase.getInstance(application).keyDao(), secureStorage)
    private val hostRepository = HostRepositoryImpl(AppDatabase.getInstance(application).hostDao(), secureStorage)
    private val provisioningRunner = OciProvisioningRunner(keyRepository, hostRepository)

    private val accountHandler = OciAccountConfigHandler()
    private val discoveryHandler = OciEnvironmentDiscoveryHandler(provisioningRunner)

    private val _stage = MutableStateFlow(OciOnboardingStage.WELCOME)
    val stage: StateFlow<OciOnboardingStage> = _stage.asStateFlow()

    private val _uiState = MutableStateFlow(OciOnboardingUiState())
    val uiState: StateFlow<OciOnboardingUiState> = _uiState.asStateFlow()

    private var session = provisioningRepository.loadOrCreate()

    init {
        BinBoxLogger.i(TAG, "Initializing OciOnboardingViewModel, session=${session.sessionId}, state=${session.state}")
        _stage.value = OciStageMapper.stageFor(session.state)
        credentialsStore.load().let { result ->
            if (result is AppResult.Success && result.data != null) {
                val creds = result.data
                val pubKeyPem = OciKeyManager.ensureSigningKey(creds.keyAlias).let { if (it is AppResult.Success) it.data else null }
                _uiState.update {
                    it.copy(
                        credentials = creds,
                        tenancyOcid = creds.tenancyOcid,
                        userOcid = creds.userOcid,
                        region = creds.region,
                        pendingFingerprint = creds.fingerprint.value,
                        pendingKeyAlias = creds.keyAlias,
                        publicKeyPem = pubKeyPem
                    )
                }
            }
        }
    }

    fun onEvent(event: OciOnboardingEvent) {
        BinBoxLogger.d(TAG, "onEvent: ${event.javaClass.simpleName}")
        when (event) {
            is OciOnboardingEvent.GetStarted -> advanceTo(OciOnboardingStage.ACCOUNT_INFORMATION)
            is OciOnboardingEvent.ImportConfig -> _uiState.update { accountHandler.parseAndApplyConfig(event.rawConfig, it) }
            is OciOnboardingEvent.SubmitAccountInfo -> {
                _uiState.update { accountHandler.normalizeAccountInfo(event.tenancyOcid, event.userOcid, event.region, it) }
                advanceTo(OciOnboardingStage.API_KEY_GENERATION)
                persistSessionState(OciProvisioningState.ACCOUNT_REQUIRED)
            }
            is OciOnboardingEvent.GenerateApiKey -> accountHandler.generateApiKey(
                session.sessionId,
                onSuccess = { alias, pem ->
                    _uiState.update { it.copy(pendingKeyAlias = alias, publicKeyPem = pem, error = null) }
                    advanceTo(OciOnboardingStage.API_KEY_REGISTRATION)
                    persistSessionState(OciProvisioningState.API_KEY_REQUIRED)
                },
                onError = { err -> _uiState.update { it.copy(error = err) } }
            )
            is OciOnboardingEvent.SubmitFingerprint -> accountHandler.submitFingerprint(
                event.fingerprint, _uiState.value, session.sessionId, credentialsStore,
                onSuccess = { creds, fp ->
                    _uiState.update { it.copy(credentials = creds, pendingFingerprint = fp, error = null) }
                    advanceTo(OciOnboardingStage.CONNECTION_VERIFICATION)
                    persistSessionState(OciProvisioningState.API_KEY_REGISTERED)
                },
                onError = { err -> _uiState.update { it.copy(error = err) } }
            )
            is OciOnboardingEvent.VerifyConnection -> verifyConnection()
            OciOnboardingEvent.GenerateVmSshKey -> generateVmSshKey()
            OciOnboardingEvent.DiscoverContext -> discoverContext()
            is OciOnboardingEvent.SelectCompartment -> _uiState.update { it.copy(context = it.context.copy(selectedCompartmentOcid = event.compartmentOcid)) }
            is OciOnboardingEvent.SelectAvailabilityDomain -> selectAvailabilityDomain(event.availabilityDomain)
            is OciOnboardingEvent.SelectShape -> selectShape(event.shape)
            is OciOnboardingEvent.SelectImage -> _uiState.update { it.copy(context = it.context.copy(selectedImageOcid = event.imageOcid)) }
            OciOnboardingEvent.StartProvisioning -> startProvisioning()
            OciOnboardingEvent.GoBack -> handleGoBack()
            OciOnboardingEvent.EditAccountInfo -> advanceTo(OciOnboardingStage.ACCOUNT_INFORMATION)
            OciOnboardingEvent.StartOver -> handleStartOver()
            OciOnboardingEvent.Cancel -> persistSessionState(OciProvisioningState.CANCELLED)
        }
    }

    private fun handleGoBack() {
        val previous = OciStageMapper.previousStageFor(_stage.value)
        BinBoxLogger.i(TAG, "Navigating back from ${_stage.value} to $previous")
        _stage.value = previous
    }

    private fun handleStartOver() {
        BinBoxLogger.i(TAG, "Starting over OCI onboarding wizard")
        provisioningRepository.clear()
        val now = System.currentTimeMillis()
        session = OciProvisioningSession(
            sessionId = UUID.randomUUID().toString(),
            state = OciProvisioningState.NOT_STARTED,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        _uiState.value = OciOnboardingUiState()
        _stage.value = OciOnboardingStage.WELCOME
    }

    private fun verifyConnection() {
        val creds = _uiState.value.credentials ?: run {
            _uiState.update { it.copy(error = "No OCI credentials to verify.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, error = null) }
            discoveryHandler.verifyConnection(
                creds, _uiState.value.publicKeyPem,
                onSuccess = { diag, pem ->
                    _uiState.update { it.copy(isVerifying = false, error = null, diagnostics = diag, publicKeyPem = pem ?: it.publicKeyPem) }
                    persistSessionState(OciProvisioningState.AUTHENTICATION_VERIFIED)
                },
                onError = { msg, diag, pem ->
                    _uiState.update { it.copy(isVerifying = false, error = msg, diagnostics = diag, publicKeyPem = pem ?: it.publicKeyPem) }
                    session = session.fail(OciProvisioningError(OciErrorCategory.AUTHENTICATION_ERROR, msg, diag.ociErrorCode ?: "AUTH_FAILED"), OciProvisioningState.AUTH_FAILED)
                    provisioningRepository.save(session)
                }
            )
        }
    }

    private fun discoverContext() {
        val creds = _uiState.value.credentials
        val client = creds?.let { OciClient(it.region) { _uiState.value.credentials } }
        if (creds == null || client == null) {
            _uiState.update { it.copy(error = "No verified OCI connection yet.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, error = null) }
            discoveryHandler.discoverContext(
                client, creds, _uiState.value.publicKeyPem,
                onSuccess = { compartments, ads ->
                    _uiState.update {
                        it.copy(
                            isDiscovering = false, discoveredCompartments = compartments, discoveredAvailabilityDomains = ads,
                            context = it.context.copy(availableCompartmentOcids = compartments.map { c -> c.id }, selectedCompartmentOcid = it.context.selectedCompartmentOcid ?: creds.tenancyOcid, availabilityDomains = ads),
                            error = null
                        )
                    }
                    advanceTo(OciOnboardingStage.HOST_CONFIGURATION)
                    persistSessionState(OciProvisioningState.CONTEXT_DISCOVERED)
                },
                onError = { err, diag -> _uiState.update { it.copy(isDiscovering = false, error = err, diagnostics = diag) } }
            )
        }
    }

    private fun selectAvailabilityDomain(ad: String) {
        _uiState.update { it.copy(context = it.context.copy(selectedAvailabilityDomain = ad)) }
        val creds = _uiState.value.credentials ?: return
        val compId = _uiState.value.context.selectedCompartmentOcid ?: return
        val client = OciClient(creds.region) { _uiState.value.credentials }
        viewModelScope.launch {
            discoveryHandler.fetchShapes(client, compId, ad,
                onSuccess = { shapes -> _uiState.update { it.copy(discoveredShapes = shapes, error = null) } },
                onError = { err -> _uiState.update { it.copy(error = err) } }
            )
        }
    }

    private fun selectShape(shape: String) {
        _uiState.update { it.copy(context = it.context.copy(selectedShapeName = shape)) }
        val creds = _uiState.value.credentials ?: return
        val compId = _uiState.value.context.selectedCompartmentOcid ?: return
        val client = OciClient(creds.region) { _uiState.value.credentials }
        viewModelScope.launch {
            discoveryHandler.fetchImages(client, compId, shape,
                onSuccess = { imgs -> _uiState.update { it.copy(discoveredImages = imgs, error = null) } },
                onError = { err -> _uiState.update { it.copy(error = err) } }
            )
        }
    }

    private fun generateVmSshKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingVmSshKey = true, error = null) }
            when (val result = provisioningRunner.generateVmSshKey(session.sessionId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isGeneratingVmSshKey = false, vmSshPublicKey = result.data.publicKey, error = null) }
                    advanceTo(OciOnboardingStage.NETWORK_PROVISIONING)
                }
                is AppResult.Error -> _uiState.update { it.copy(isGeneratingVmSshKey = false, error = result.error.userMessage) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun startProvisioning() {
        val creds = _uiState.value.credentials
        val client = creds?.let { OciClient(it.region) { _uiState.value.credentials } }
        val context = _uiState.value.context
        if (creds == null || client == null) return

        viewModelScope.launch {
            val sshPublicKey = _uiState.value.vmSshPublicKey
                ?: session.sshKeyAlias?.toLongOrNull()?.let { keyRepository.getKeyById(it)?.publicKey }
            if (sshPublicKey == null) {
                _uiState.update { it.copy(error = "No VM SSH key yet — generate one first.") }
                return@launch
            }

            _uiState.update { it.copy(isProvisioning = true, error = null) }
            val result = OciProvisioner(client).provision(session, context, sshPublicKey) { updated ->
                session = updated
                provisioningRepository.save(updated)
                _uiState.update { it.copy(provisioningState = updated.state) }
            }

            when (result) {
                is OciResult.Success -> {
                    session = result.data
                    _uiState.update { it.copy(isProvisioning = false, error = null, provisionedPublicIp = result.data.publicIp) }
                    advanceTo(OciOnboardingStage.SSH_VERIFICATION)
                    registerHost(creds, result.data)
                }
                is OciResult.Error -> _uiState.update { it.copy(isProvisioning = false, error = result.error.whatHappened) }
            }
        }
    }

    private suspend fun registerHost(creds: OciCredentials, provisioned: OciProvisioningSession) {
        when (val res = provisioningRunner.registerHost(creds, provisioned, _uiState.value.discoveredImages)) {
            is AppResult.Success -> {
                session = session.copy(registeredShellHostId = res.data.toString(), updatedAtMillis = System.currentTimeMillis())
                provisioningRepository.save(session)
                advanceTo(OciOnboardingStage.SHELL_READY)
                persistSessionState(OciProvisioningState.SHELL_READY)
            }
            is AppResult.Error -> _uiState.update { it.copy(error = "Instance provisioned, but couldn't register host: ${res.error.userMessage}") }
            AppResult.Loading -> Unit
        }
    }

    private fun advanceTo(stage: OciOnboardingStage) {
        BinBoxLogger.d(TAG, "Advancing stage to $stage")
        _stage.value = stage
    }

    private fun persistSessionState(state: OciProvisioningState) {
        session = session.advance(state)
        provisioningRepository.save(session)
    }

    companion object {
        private const val TAG = "OciOnboardingViewModel"
    }
}

private fun MutableStateFlow<OciOnboardingUiState>.update(transform: (OciOnboardingUiState) -> OciOnboardingUiState) {
    value = transform(value)
}
