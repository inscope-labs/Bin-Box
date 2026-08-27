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
                _uiState.update {
                    it.copy(
                        credentials = creds,
                        tenancyOcid = creds.tenancyOcid,
                        userOcid = creds.userOcid,
                        region = creds.region,
                        pendingFingerprint = creds.fingerprint.value
                    )
                }
            }
        }
    }

    fun onEvent(event: OciOnboardingEvent) {
        BinBoxLogger.d(TAG, "onEvent: ${event.javaClass.simpleName}")
        when (event) {
            is OciOnboardingEvent.GetStarted -> advanceTo(OciOnboardingStage.ACCOUNT_INFORMATION)
            is OciOnboardingEvent.ImportConfig -> handleImportConfig(event.rawConfig)
            is OciOnboardingEvent.SubmitAccountInfo -> handleSubmitAccountInfo(event.tenancyOcid, event.userOcid, event.region)
            is OciOnboardingEvent.GenerateApiKey -> generateApiKey()
            is OciOnboardingEvent.SubmitFingerprint -> submitFingerprint(event.fingerprint)
            is OciOnboardingEvent.VerifyConnection -> verifyConnection()
            OciOnboardingEvent.GenerateVmSshKey -> generateVmSshKey()
            OciOnboardingEvent.DiscoverContext -> discoverContext()
            is OciOnboardingEvent.SelectCompartment -> selectCompartment(event.compartmentOcid)
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

    private fun handleImportConfig(raw: String) {
        val parsed = OciConfigParser.parse(raw)
        _uiState.update { current ->
            current.copy(
                tenancyOcid = parsed.tenancyOcid ?: current.tenancyOcid,
                userOcid = parsed.userOcid ?: current.userOcid,
                region = parsed.region ?: current.region,
                pendingFingerprint = parsed.fingerprint ?: current.pendingFingerprint,
                error = null
            )
        }
        BinBoxLogger.i(TAG, "Imported OCI config: tenancy=${parsed.tenancyOcid != null}, user=${parsed.userOcid != null}")
    }

    private fun handleSubmitAccountInfo(tenancy: String, user: String, region: String) {
        val normalizedRegion = OciRegionHelper.normalizeRegion(region)
        _uiState.update { it.copy(tenancyOcid = tenancy, userOcid = user, region = normalizedRegion, error = null) }
        advanceTo(OciOnboardingStage.API_KEY_GENERATION)
        persistSessionState(OciProvisioningState.ACCOUNT_REQUIRED)
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

    private fun generateApiKey() {
        val alias = "oci_api_signing_${session.sessionId}"
        when (val result = OciKeyManager.ensureSigningKey(alias)) {
            is AppResult.Success -> {
                _uiState.update { it.copy(pendingKeyAlias = alias, publicKeyPem = result.data, error = null) }
                advanceTo(OciOnboardingStage.API_KEY_REGISTRATION)
                persistSessionState(OciProvisioningState.API_KEY_REQUIRED)
            }
            is AppResult.Error -> _uiState.update { it.copy(error = result.error.userMessage) }
            AppResult.Loading -> Unit
        }
    }

    private fun submitFingerprint(raw: String) {
        val fingerprint = OciFingerprint.parseOrNull(raw) ?: run {
            _uiState.update { it.copy(error = "Invalid fingerprint. Format: aa:bb:cc:...:zz") }
            return
        }
        val state = _uiState.value
        val alias = state.pendingKeyAlias
        if (state.tenancyOcid == null || state.userOcid == null || state.region == null || alias == null) {
            _uiState.update { it.copy(error = "Missing account info or key — return to step 1.") }
            return
        }

        val credentials = OciCredentials(state.tenancyOcid, state.userOcid, fingerprint, state.region, alias)
        when (val result = credentialsStore.save(credentials)) {
            is AppResult.Success -> {
                _uiState.update { it.copy(credentials = credentials, pendingFingerprint = fingerprint.value, error = null) }
                advanceTo(OciOnboardingStage.CONNECTION_VERIFICATION)
                persistSessionState(OciProvisioningState.API_KEY_REGISTERED)
            }
            is AppResult.Error -> _uiState.update { it.copy(error = result.error.userMessage) }
            AppResult.Loading -> Unit
        }
    }

    private fun verifyConnection() {
        val credentials = _uiState.value.credentials ?: run {
            _uiState.update { it.copy(error = "No OCI credentials to verify.") }
            return
        }
        val endpointUrl = "${OciApiConfig.identityBaseUrl(credentials.region)}20160918/users/${credentials.userOcid}"

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, error = null) }
            try {
                val client = OciClient(credentials.region) { _uiState.value.credentials }
                val response = client.identityApi.getUser(credentials.userOcid)
                val opcReqId = response.headers()["opc-request-id"]

                if (response.isSuccessful) {
                    val diagnostics = OciVerificationDiagnostics(endpointUrl, "GET", credentials.region, credentials.tenancyOcid, credentials.userOcid, credentials.fingerprint.value, credentials.keyAlias, response.code(), opcRequestId = opcReqId)
                    _uiState.update { it.copy(isVerifying = false, error = null, diagnostics = diagnostics) }
                    persistSessionState(OciProvisioningState.AUTHENTICATION_VERIFIED)
                } else {
                    val apiError = OciApiErrorMapper.fromErrorResponse(response)
                    val diagnostics = OciVerificationDiagnostics(endpointUrl, "GET", credentials.region, credentials.tenancyOcid, credentials.userOcid, credentials.fingerprint.value, credentials.keyAlias, response.code(), apiError.whyItHappened, apiError.whatHappened, opcRequestId = opcReqId)
                    _uiState.update { it.copy(isVerifying = false, error = apiError.whatHappened, diagnostics = diagnostics) }
                    session = session.fail(apiError, OciProvisioningState.AUTH_FAILED)
                    provisioningRepository.save(session)
                }
            } catch (e: Exception) {
                BinBoxLogger.e(TAG, "Connection verification exception", e)
                val isHostError = e.javaClass.simpleName.contains("UnknownHost", ignoreCase = true) || e.message?.contains("Unable to resolve host", ignoreCase = true) == true
                val userMsg = if (isHostError) "Unable to reach Oracle Cloud (${credentials.region}). Check your network connection and region name." else "Couldn't reach OCI: ${e.localizedMessage ?: e.javaClass.simpleName}"
                val diagnostics = OciVerificationDiagnostics(endpointUrl, "GET", credentials.region, credentials.tenancyOcid, credentials.userOcid, credentials.fingerprint.value, credentials.keyAlias, exceptionClass = e.javaClass.name, rawExceptionMessage = e.message ?: e.toString())
                _uiState.update { it.copy(isVerifying = false, error = userMsg, diagnostics = diagnostics) }
            }
        }
    }

    private fun generateVmSshKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingVmSshKey = true, error = null) }
            when (val result = provisioningRunner.generateVmSshKey(session.sessionId)) {
                is AppResult.Success -> {
                    session = session.copy(sshKeyAlias = result.data.id.toString(), updatedAtMillis = System.currentTimeMillis())
                    provisioningRepository.save(session)
                    _uiState.update { it.copy(isGeneratingVmSshKey = false, vmSshPublicKey = result.data.publicKey, error = null) }
                    persistSessionState(OciProvisioningState.SSH_KEY_READY)
                }
                is AppResult.Error -> _uiState.update { it.copy(isGeneratingVmSshKey = false, error = result.error.userMessage) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun requireClient(): OciClient? =
        _uiState.value.credentials?.let { creds -> OciClient(creds.region) { _uiState.value.credentials } }

    private fun discoverContext() {
        val credentials = _uiState.value.credentials
        val client = requireClient()
        if (credentials == null || client == null) {
            _uiState.update { it.copy(error = "No verified OCI connection yet.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, error = null) }
            when (val res = provisioningRunner.discoverContext(client, credentials.tenancyOcid)) {
                is AppResult.Success -> {
                    val (compartments, ads) = res.data
                    _uiState.update {
                        it.copy(
                            isDiscovering = false,
                            discoveredCompartments = compartments,
                            discoveredAvailabilityDomains = ads,
                            context = it.context.copy(
                                availableCompartmentOcids = compartments.map { c -> c.id },
                                selectedCompartmentOcid = it.context.selectedCompartmentOcid ?: credentials.tenancyOcid,
                                availabilityDomains = ads
                            ),
                            error = null
                        )
                    }
                    advanceTo(OciOnboardingStage.HOST_CONFIGURATION)
                    persistSessionState(OciProvisioningState.CONTEXT_DISCOVERED)
                }
                is AppResult.Error -> _uiState.update { it.copy(isDiscovering = false, error = res.error.userMessage) }
                AppResult.Loading -> Unit
            }
        }
    }

    private fun selectCompartment(compartmentOcid: String) {
        _uiState.update { it.copy(context = it.context.copy(selectedCompartmentOcid = compartmentOcid)) }
    }

    private fun selectAvailabilityDomain(availabilityDomain: String) {
        _uiState.update { it.copy(context = it.context.copy(selectedAvailabilityDomain = availabilityDomain)) }
        val client = requireClient() ?: return
        val compartmentId = _uiState.value.context.selectedCompartmentOcid ?: return
        viewModelScope.launch {
            when (val result = OciContextDiscovery(client).fetchEligibleShapes(compartmentId, availabilityDomain)) {
                is OciResult.Success -> _uiState.update { it.copy(discoveredShapes = result.data, error = null) }
                is OciResult.Error -> _uiState.update { it.copy(error = result.error.whatHappened) }
            }
        }
    }

    private fun selectShape(shape: String) {
        _uiState.update { it.copy(context = it.context.copy(selectedShapeName = shape)) }
        val client = requireClient() ?: return
        val compartmentId = _uiState.value.context.selectedCompartmentOcid ?: return
        viewModelScope.launch {
            when (val result = OciContextDiscovery(client).fetchImages(compartmentId, shape)) {
                is OciResult.Success -> _uiState.update { it.copy(discoveredImages = result.data, error = null) }
                is OciResult.Error -> _uiState.update { it.copy(error = result.error.whatHappened) }
            }
        }
    }

    private fun startProvisioning() {
        val credentials = _uiState.value.credentials
        val client = requireClient()
        val context = _uiState.value.context
        if (credentials == null || client == null) return

        viewModelScope.launch {
            val sshPublicKey = _uiState.value.vmSshPublicKey
                ?: session.sshKeyAlias?.toLongOrNull()?.let { keyRepository.getKeyById(it)?.publicKey }
            if (sshPublicKey == null) {
                _uiState.update { it.copy(error = "No VM SSH key yet — generate one first.") }
                return@launch
            }

            _uiState.update { it.copy(isProvisioning = true, error = null) }
            val provisioner = OciProvisioner(client)
            val result = provisioner.provision(session, context, sshPublicKey) { updated ->
                session = updated
                provisioningRepository.save(updated)
                _uiState.update { it.copy(provisioningState = updated.state) }
            }

            when (result) {
                is OciResult.Success -> {
                    session = result.data
                    _uiState.update { it.copy(isProvisioning = false, error = null, provisionedPublicIp = result.data.publicIp) }
                    advanceTo(OciOnboardingStage.SSH_VERIFICATION)
                    registerHost(credentials, result.data)
                }
                is OciResult.Error -> _uiState.update { it.copy(isProvisioning = false, error = result.error.whatHappened) }
            }
        }
    }

    private suspend fun registerHost(credentials: OciCredentials, provisioned: OciProvisioningSession) {
        when (val res = provisioningRunner.registerHost(credentials, provisioned, _uiState.value.discoveredImages)) {
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
