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
    private val resumeHandler = OciResumeHandler(discoveryHandler, keyRepository)
    private val hostConfigHandler = OciHostConfigSelectionHandler()

    private val _stage = MutableStateFlow(OciOnboardingStage.WELCOME)
    val stage: StateFlow<OciOnboardingStage> = _stage.asStateFlow()

    private val _uiState = MutableStateFlow(OciOnboardingUiState())
    val uiState: StateFlow<OciOnboardingUiState> = _uiState.asStateFlow()

    private var session = provisioningRepository.loadOrCreate()

    init {
        BinBoxLogger.i(TAG, "Initializing OciOnboardingViewModel, session=${session.sessionId}, state=${session.state}")
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
        // Don't silently jump into a resumed stage — a session with real progress (network,
        // instance, etc. may already exist in OCI) needs the user's explicit go-ahead, since
        // "resume" and "start fresh" are meaningfully different actions here.
        if (session.state == OciProvisioningState.NOT_STARTED) {
            _stage.value = OciOnboardingStage.WELCOME
        } else {
            BinBoxLogger.i(TAG, "Found resumable session at state=${session.state}, prompting user")
            _uiState.update { it.copy(pendingResumeStage = OciStageMapper.stageFor(session.state)) }
        }
    }

    fun onEvent(event: OciOnboardingEvent) {
        BinBoxLogger.d(TAG, "onEvent: ${event.javaClass.simpleName}")
        when (event) {
            is OciOnboardingEvent.GetStarted -> advanceTo(OciOnboardingStage.ACCOUNT_INFORMATION)
            is OciOnboardingEvent.ImportConfig -> _uiState.update { accountHandler.parseAndApplyConfig(event.rawConfig, it) }
            is OciOnboardingEvent.SubmitAccountInfo -> {
                val accountChanged = _uiState.value.tenancyOcid != event.tenancyOcid ||
                    _uiState.value.userOcid != event.userOcid ||
                    _uiState.value.region != event.region
                _uiState.update { accountHandler.normalizeAccountInfo(event.tenancyOcid, event.userOcid, event.region, it) }
                if (accountChanged) invalidateFromAccountInfo()
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
            is OciOnboardingEvent.SelectCompartment -> selectCompartment(event.compartmentOcid)
            is OciOnboardingEvent.SelectAvailabilityDomain -> selectAvailabilityDomain(event.availabilityDomain)
            is OciOnboardingEvent.SelectShape -> selectShape(event.shape)
            is OciOnboardingEvent.SelectImage -> selectImage(event.imageOcid)
            OciOnboardingEvent.StartProvisioning -> startProvisioning()
            OciOnboardingEvent.GoBack -> handleGoBack()
            OciOnboardingEvent.EditAccountInfo -> advanceTo(OciOnboardingStage.ACCOUNT_INFORMATION)
            OciOnboardingEvent.StartOver -> handleStartOver()
            OciOnboardingEvent.Cancel -> persistSessionState(OciProvisioningState.CANCELLED)
            OciOnboardingEvent.ContinueToKeyRegistration -> advanceTo(OciOnboardingStage.API_KEY_REGISTRATION)
            OciOnboardingEvent.ResumeSession -> resumeSession()
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

    private fun selectCompartment(compartmentOcid: String) {
        val (newUiState, newSession) = hostConfigHandler.selectCompartment(compartmentOcid, _uiState.value, session) ?: return
        applySelection(newUiState, newSession)
    }

    private fun selectAvailabilityDomain(ad: String) {
        val (newUiState, newSession) = hostConfigHandler.selectAvailabilityDomain(ad, _uiState.value, session)
        applySelection(newUiState, newSession)
        fetchShapesFor(ad)
    }

    private fun selectShape(shape: String) {
        val (newUiState, newSession) = hostConfigHandler.selectShape(shape, _uiState.value, session)
        applySelection(newUiState, newSession)
        fetchImagesFor(shape)
    }

    private fun selectImage(imageOcid: String) {
        val (newUiState, newSession) = hostConfigHandler.selectImage(imageOcid, _uiState.value, session) ?: return
        applySelection(newUiState, newSession)
    }

    private fun applySelection(newUiState: OciOnboardingUiState, newSession: OciProvisioningSession) {
        _uiState.value = newUiState
        if (newSession !== session) {
            BinBoxLogger.i(TAG, "Host configuration changed — invalidating downstream provisioning progress")
            session = newSession
            provisioningRepository.save(session)
        }
    }

    private fun fetchShapesFor(ad: String) {
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

    private fun fetchImagesFor(shape: String) {
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

    private fun invalidateFromAccountInfo() {
        BinBoxLogger.i(TAG, "Account info changed — invalidating discovered environment and provisioning progress")
        val (newUiState, newSession) = OciProvisioningInvalidation.clearFromAccountChange(_uiState.value, session)
        _uiState.value = newUiState
        session = newSession
    }

    private fun generateVmSshKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingVmSshKey = true, error = null) }
            when (val result = provisioningRunner.generateVmSshKey(session.sessionId)) {
                is AppResult.Success -> {
                    session = session.copy(sshKeyAlias = result.data.id.toString())
                    _uiState.update { it.copy(isGeneratingVmSshKey = false, vmSshPublicKey = result.data.publicKey, error = null) }
                    advanceTo(OciOnboardingStage.NETWORK_PROVISIONING)
                    persistSessionState(OciProvisioningState.SSH_KEY_READY)
                    // Actually kick off provisioning — reaching this stage previously just
                    // displayed "Starting…" forever because nothing called startProvisioning().
                    startProvisioning()
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
            _uiState.update { it.copy(isProvisioning = true, error = null) }
            val result = provisioningRunner.runProvisioning(session, context, _uiState.value.vmSshPublicKey, client) { updated ->
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

    /** Restores wizard UI state from the persisted [session] and continues from wherever it left
     * off, via [OciResumeHandler] (module role — see that file's kdoc for why re-provisioning on
     * resume is safe). */
    private fun resumeSession() {
        val targetStage = OciStageMapper.stageFor(session.state)
        BinBoxLogger.i(TAG, "Resuming session ${session.sessionId} at state=${session.state} -> stage=$targetStage")
        _uiState.update {
            it.copy(
                pendingResumeStage = null,
                isResuming = true,
                context = session.context,
                provisionedPublicIp = session.publicIp,
                provisioningState = session.state.takeIf { s -> s in resumeHandler.inProgressStates }
            )
        }
        _stage.value = targetStage

        viewModelScope.launch {
            val creds = _uiState.value.credentials
            resumeHandler.resume(
                session = session,
                credentials = creds,
                currentPublicKeyPem = _uiState.value.publicKeyPem,
                targetStage = targetStage,
                updateUiState = { transform -> _uiState.update(transform) },
                onProvisioningInProgress = { startProvisioning() },
                onProvisioningComplete = { creds?.let { registerHost(it, session) } }
            )
            _uiState.update { it.copy(isResuming = false) }
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
