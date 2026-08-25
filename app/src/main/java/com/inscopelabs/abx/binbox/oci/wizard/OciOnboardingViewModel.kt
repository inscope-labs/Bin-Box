package com.inscopelabs.abx.binbox.oci.wizard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.database.AppDatabase
import com.inscopelabs.abx.binbox.data.repository.HostRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.KeyRepositoryImpl
import com.inscopelabs.abx.binbox.oci.api.OciClient
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.identity.OciCredentials
import com.inscopelabs.abx.binbox.oci.identity.OciCredentialsStore
import com.inscopelabs.abx.binbox.oci.identity.OciFingerprint
import com.inscopelabs.abx.binbox.oci.identity.OciKeyManager
import com.inscopelabs.abx.binbox.oci.provisioning.OciApiErrorMapper
import com.inscopelabs.abx.binbox.oci.provisioning.OciContextDiscovery
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningContext
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningRepository
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioner
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningSession
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState
import com.inscopelabs.abx.binbox.oci.provisioning.OciResult
import com.inscopelabs.abx.binbox.oci.provisioning.OciSshKeyProvisioner
import com.inscopelabs.abx.binbox.oci.terminal.OciHostRegistrar
import com.inscopelabs.abx.binbox.oci.terminal.OciShellHost
import com.inscopelabs.abx.binbox.oci.terminal.defaultSshUsernameFor
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
 * are real — key generation, credential persistence, the account-info
 * form, and [verifyConnection] itself (a real GetUser call through
 * [com.inscopelabs.abx.binbox.oci.api.OciClient]) are all backed by
 * working code below. [generateVmSshKey] (§20) is real and independent of
 * the API-dependent stages. [discoverContext]/[selectCompartment]/
 * [selectAvailabilityDomain]/[selectShape] and [startProvisioning] are also
 * real as of this pass — the full compartment/AD/shape/image discovery and
 * selection flow, then [com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioner]
 * end to end through public IP discovery, followed automatically by
 * [registerHost] (§26) — a real [com.inscopelabs.abx.binbox.domain.repository.IHostRepository]
 * entry, connectable from Bin-Box's normal terminal list once provisioning
 * succeeds. NOT yet built: SSH verification (§25) — [registerHost] runs
 * immediately after public IP discovery without first confirming the
 * instance is actually SSH-reachable (see that function's kdoc).
 */
class OciOnboardingViewModel(
    application: Application,
    private val secureStorage: SecureStorageService = SecureStorageService(application)
) : AndroidViewModel(application) {

    private val credentialsStore = OciCredentialsStore(application, secureStorage)
    private val provisioningRepository = OciProvisioningRepository(application)
    private val keyRepository = KeyRepositoryImpl(AppDatabase.getInstance(application).keyDao(), secureStorage)
    private val sshKeyProvisioner = OciSshKeyProvisioner(keyRepository)
    private val hostRegistrar = OciHostRegistrar(
        HostRepositoryImpl(AppDatabase.getInstance(application).hostDao(), secureStorage)
    )

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

            OciOnboardingEvent.GenerateVmSshKey -> generateVmSshKey()

            OciOnboardingEvent.DiscoverContext -> discoverContext()

            is OciOnboardingEvent.SelectCompartment -> selectCompartment(event.compartmentOcid)

            is OciOnboardingEvent.SelectAvailabilityDomain -> selectAvailabilityDomain(event.availabilityDomain)

            is OciOnboardingEvent.SelectShape -> selectShape(event.shape)

            is OciOnboardingEvent.SelectImage -> {
                _uiState.update { it.copy(context = it.context.copy(selectedImageOcid = event.imageOcid)) }
            }

            OciOnboardingEvent.StartProvisioning -> startProvisioning()

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

    /**
     * §14's "harmless authenticated OCI API request" — GetUser on the
     * user's own OCID. Real as of this pass: builds an [OciClient] for
     * [OciCredentials.region] and calls [com.inscopelabs.abx.binbox.oci.api.compartments.IdentityApi.getUser].
     * A 401/403 here means auth is broken specifically — not networking,
     * not compute, not SSH (§14's required distinction).
     */
    private fun verifyConnection() {
        val credentials = _uiState.value.credentials
        if (credentials == null) {
            _uiState.update { it.copy(error = "No OCI credentials to verify.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true, error = null) }
            try {
                val client = OciClient(credentials.region) { _uiState.value.credentials }
                val response = client.identityApi.getUser(credentials.userOcid)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(isVerifying = false, error = null) }
                    persistSessionState(OciProvisioningState.AUTHENTICATION_VERIFIED)
                } else {
                    val apiError = OciApiErrorMapper.fromErrorResponse(response)
                    _uiState.update { it.copy(isVerifying = false, error = apiError.whatHappened) }
                    session = session.fail(apiError, OciProvisioningState.AUTH_FAILED)
                    provisioningRepository.save(session)
                }
            } catch (e: Exception) {
                BinBoxLogger.e("OciOnboardingViewModel", "Connection verification failed", e)
                _uiState.update { it.copy(isVerifying = false, error = "Couldn't reach OCI — check your network connection.") }
            }
        }
    }

    /**
     * Generates the VM's SSH key pair (§20). Real — see [OciSshKeyProvisioner] —
     * unlike [verifyConnection] and everything reachable after it, which
     * depend on the not-yet-built OCI API client. Callable independently of
     * wizard position for now; the wizard doesn't yet reach
     * [OciOnboardingStage.SSH_KEY_GENERATION] in its normal sequence since
     * the stages between here and there aren't built.
     */
    private fun generateVmSshKey() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingVmSshKey = true, error = null) }
            when (val result = sshKeyProvisioner.generateForSession(session.sessionId)) {
                is AppResult.Success -> {
                    val key = result.data
                    session = session.copy(
                        sshKeyAlias = key.id.toString(),
                        updatedAtMillis = System.currentTimeMillis()
                    )
                    provisioningRepository.save(session)
                    _uiState.update {
                        it.copy(
                            isGeneratingVmSshKey = false,
                            vmSshPublicKey = key.publicKey,
                            error = null
                        )
                    }
                    persistSessionState(OciProvisioningState.SSH_KEY_READY)
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isGeneratingVmSshKey = false, error = result.error.userMessage) }
                    BinBoxLogger.e("OciOnboardingViewModel", "VM SSH key generation failed: ${result.error.userMessage}")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    /** Shared with [discoverContext]/[startProvisioning] — same construction pattern as [verifyConnection]. */
    private fun requireClient(): OciClient? =
        _uiState.value.credentials?.let { creds -> OciClient(creds.region) { _uiState.value.credentials } }

    /**
     * §15-17: populates compartments and availability domains once auth is
     * verified. Pre-selects the tenancy root as the compartment — visibly,
     * in [OciOnboardingUiState.context], not silently: the user can
     * override via [OciOnboardingEvent.SelectCompartment].
     */
    private fun discoverContext() {
        val credentials = _uiState.value.credentials
        val client = requireClient()
        if (credentials == null || client == null) {
            _uiState.update { it.copy(error = "No verified OCI connection yet.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscovering = true, error = null) }
            val discovery = OciContextDiscovery(client)

            val compartmentsResult = discovery.fetchCompartments(credentials.tenancyOcid)
            val adResult = discovery.fetchAvailabilityDomains(credentials.tenancyOcid)

            val compartments = (compartmentsResult as? OciResult.Success)?.data
            val ads = (adResult as? OciResult.Success)?.data

            val firstError = (compartmentsResult as? OciResult.Error)?.error
                ?: (adResult as? OciResult.Error)?.error

            if (firstError != null) {
                _uiState.update { it.copy(isDiscovering = false, error = firstError.whatHappened) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isDiscovering = false,
                    discoveredCompartments = compartments.orEmpty(),
                    discoveredAvailabilityDomains = ads.orEmpty().map { ad -> ad.name },
                    context = it.context.copy(
                        availableCompartmentOcids = compartments.orEmpty().map { c -> c.id },
                        selectedCompartmentOcid = it.context.selectedCompartmentOcid ?: credentials.tenancyOcid,
                        availabilityDomains = ads.orEmpty().map { ad -> ad.name }
                    ),
                    error = null
                )
            }
            advanceTo(OciOnboardingStage.HOST_CONFIGURATION)
            persistSessionState(OciProvisioningState.CONTEXT_DISCOVERED)
        }
    }

    private fun selectCompartment(compartmentOcid: String) {
        _uiState.update { it.copy(context = it.context.copy(selectedCompartmentOcid = compartmentOcid)) }
    }

    /** Selecting an AD is also when eligible shapes become fetchable — both compartment and AD are required for ListShapes. */
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

    /** Selecting a shape is also when compatible images become fetchable. */
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

    /**
     * Runs [OciProvisioner.provision] end to end (§18-24). Requires a fully
     * resolved [OciProvisioningContext] and a VM SSH public key — the
     * latter resolved from [OciOnboardingUiState.vmSshPublicKey] if present
     * (same wizard run), or looked up from [keyRepository] via
     * [OciProvisioningSession.sshKeyAlias] on resume (a new process, same
     * session — §32).
     */
    private fun startProvisioning() {
        val credentials = _uiState.value.credentials
        val client = requireClient()
        val context = _uiState.value.context
        if (credentials == null || client == null) {
            _uiState.update { it.copy(error = "No verified OCI connection yet.") }
            return
        }
        viewModelScope.launch {
            val sshPublicKey = _uiState.value.vmSshPublicKey
                ?: session.sshKeyAlias?.toLongOrNull()?.let { keyRepository.getKeyById(it)?.publicKey }
            if (sshPublicKey == null) {
                _uiState.update { it.copy(error = "No VM SSH key yet — generate one first.") }
                return@launch
            }

            _uiState.update { it.copy(isProvisioning = true, error = null) }
            val provisioner = OciProvisioner(client)
            val result = provisioner.provision(
                session = session,
                context = context,
                sshPublicKey = sshPublicKey
            ) { updated ->
                session = updated
                provisioningRepository.save(updated)
                _uiState.update { it.copy(provisioningState = updated.state) }
            }

            when (result) {
                is OciResult.Success -> {
                    session = result.data
                    _uiState.update {
                        it.copy(
                            isProvisioning = false,
                            error = null,
                            provisionedPublicIp = result.data.publicIp
                        )
                    }
                    advanceTo(OciOnboardingStage.SSH_VERIFICATION)
                    registerHost(credentials, result.data)
                }
                is OciResult.Error -> {
                    _uiState.update { it.copy(isProvisioning = false, error = result.error.whatHappened) }
                    BinBoxLogger.e("OciOnboardingViewModel", "Provisioning failed: ${result.error.whatHappened}")
                }
            }
        }
    }

    /**
     * §26: registers the provisioned instance as a real Bin-Box host. Runs
     * automatically once [startProvisioning] succeeds — unlike shape/image
     * selection, there's no user judgment call left to make here, so this
     * isn't a separate wizard-driven event.
     *
     * NOT §25 (SSH verification) — that still doesn't exist. This registers
     * the host as soon as the instance and its public IP exist, without
     * first confirming SSH is actually reachable. A host that's registered
     * but not yet reachable will simply fail to connect in the terminal
     * like any other host with a bad address would — not silently broken,
     * but not pre-verified either. Flagged as a known gap, not hidden.
     */
    private suspend fun registerHost(credentials: OciCredentials, provisioned: OciProvisioningSession) {
        val instanceId = provisioned.instanceOcid
        val publicIp = provisioned.publicIp
        val compartmentId = provisioned.context.selectedCompartmentOcid
        if (instanceId == null || publicIp == null || compartmentId == null) {
            _uiState.update { it.copy(error = "Provisioning succeeded but is missing data needed to register the host.") }
            return
        }

        val selectedImage = _uiState.value.discoveredImages.firstOrNull { it.id == provisioned.context.selectedImageOcid }
        val username = selectedImage?.operatingSystem?.let { defaultSshUsernameFor(it) } ?: "opc"

        val sshKeyRepositoryId = provisioned.sshKeyAlias?.toLongOrNull()

        val shellHost = OciShellHost(
            id = provisioned.sessionId,
            displayName = "Oracle Cloud (${credentials.region})",
            hostname = publicIp,
            username = username,
            sshKeyAlias = provisioned.sshKeyAlias,
            instanceOcid = instanceId,
            region = credentials.region,
            compartmentOcid = compartmentId
        )

        when (val result = hostRegistrar.register(shellHost, sshKeyRepositoryId)) {
            is AppResult.Success -> {
                session = session.copy(
                    registeredShellHostId = result.data.toString(),
                    updatedAtMillis = System.currentTimeMillis()
                )
                provisioningRepository.save(session)
                advanceTo(OciOnboardingStage.SHELL_READY)
                persistSessionState(OciProvisioningState.SHELL_READY)
            }
            is AppResult.Error -> {
                _uiState.update { it.copy(error = "Instance provisioned, but couldn't add it as a host: ${result.error.userMessage}") }
                BinBoxLogger.e("OciOnboardingViewModel", "Host registration failed: ${result.error.userMessage}")
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
        OciProvisioningState.SSH_KEY_READY -> OciOnboardingStage.SSH_KEY_GENERATION
        OciProvisioningState.CONTEXT_DISCOVERED -> OciOnboardingStage.HOST_CONFIGURATION
        OciProvisioningState.NETWORK_CREATING,
        OciProvisioningState.NETWORK_READY -> OciOnboardingStage.NETWORK_PROVISIONING
        OciProvisioningState.INSTANCE_CREATING,
        OciProvisioningState.INSTANCE_PROVISIONING,
        OciProvisioningState.INSTANCE_RUNNING -> OciOnboardingStage.INSTANCE_PROVISIONING
        OciProvisioningState.PUBLIC_IP_DISCOVERED -> OciOnboardingStage.SSH_VERIFICATION
        OciProvisioningState.HOST_REGISTERED -> OciOnboardingStage.HOST_REGISTRATION
        OciProvisioningState.SHELL_READY -> OciOnboardingStage.SHELL_READY
        else -> OciOnboardingStage.OCI_CONTEXT_DISCOVERY // failure states aren't wired into UI yet
    }
}

sealed class OciOnboardingEvent {
    data object GetStarted : OciOnboardingEvent()
    data class SubmitAccountInfo(val tenancyOcid: String, val userOcid: String, val region: String) : OciOnboardingEvent()
    data object GenerateApiKey : OciOnboardingEvent()
    data class SubmitFingerprint(val fingerprint: String) : OciOnboardingEvent()
    data object VerifyConnection : OciOnboardingEvent()
    data object GenerateVmSshKey : OciOnboardingEvent()
    data object DiscoverContext : OciOnboardingEvent()
    data class SelectCompartment(val compartmentOcid: String) : OciOnboardingEvent()
    data class SelectAvailabilityDomain(val availabilityDomain: String) : OciOnboardingEvent()
    data class SelectShape(val shape: String) : OciOnboardingEvent()
    data class SelectImage(val imageOcid: String) : OciOnboardingEvent()
    data object StartProvisioning : OciOnboardingEvent()
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
    val isGeneratingVmSshKey: Boolean = false,
    val vmSshPublicKey: String? = null,
    val context: OciProvisioningContext = OciProvisioningContext(),
    val isDiscovering: Boolean = false,
    val discoveredCompartments: List<Compartment> = emptyList(),
    val discoveredAvailabilityDomains: List<String> = emptyList(),
    val discoveredShapes: List<String> = emptyList(),
    val discoveredImages: List<Image> = emptyList(),
    val isProvisioning: Boolean = false,
    val provisioningState: OciProvisioningState? = null,
    val provisionedPublicIp: String? = null,
    val error: String? = null
)

private fun MutableStateFlow<OciOnboardingUiState>.update(transform: (OciOnboardingUiState) -> OciOnboardingUiState) {
    value = transform(value)
}
