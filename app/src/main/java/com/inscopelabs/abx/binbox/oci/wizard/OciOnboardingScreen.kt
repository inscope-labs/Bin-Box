package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inscopelabs.abx.binbox.oci.api.compartments.Compartment
import com.inscopelabs.abx.binbox.oci.api.compute.Image
import com.inscopelabs.abx.binbox.oci.provisioning.OciFreeTierShapes
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Compose UI for the wizard [OciOnboardingViewModel] already drives. One
 * composable per [OciOnboardingStage], switched via [AnimatedContent] over
 * [OciOnboardingViewModel.stage] — this file is purely presentational, all
 * logic lives in the ViewModel already.
 *
 * Entry point: shown full-screen from [com.inscopelabs.abx.binbox.ui.components.HostsScreen]
 * when the user picks "Provision on Oracle Cloud". [onDismiss] closes the
 * wizard at any point (cancels via [OciOnboardingEvent.Cancel] when
 * mid-flow); [onShellReady] fires once [OciOnboardingStage.SHELL_READY] is
 * reached so the caller can drop back to the hosts list, where the new host
 * shows up on its own since it's already been written to the same
 * IHostRepository-backed table the hosts list reads from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OciOnboardingScreen(
    onDismiss: () -> Unit,
    onShellReady: () -> Unit,
    viewModel: OciOnboardingViewModel = viewModel()
) {
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(stage) {
        if (stage == OciOnboardingStage.SHELL_READY) onShellReady()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ImmersiveBg,
        topBar = {
            Surface(
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (stage != OciOnboardingStage.SHELL_READY) {
                                    viewModel.onEvent(OciOnboardingEvent.Cancel)
                                }
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextSecondary)
                            }
                            Column {
                                Text(
                                    "Oracle Cloud Setup",
                                    color = ImmersiveTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    stageLabel(stage),
                                    color = ImmersiveTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ImmersiveComponent)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Always Free tier",
                                color = ImmersiveTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    WizardProgressBar(stage)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    (slideInHorizontally(tween(220)) { it / 4 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { -it / 4 } + fadeOut(tween(220)))
                },
                label = "oci_wizard_stage"
            ) { targetStage ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    when (targetStage) {
                        OciOnboardingStage.WELCOME -> WelcomeStage(
                            onGetStarted = { viewModel.onEvent(OciOnboardingEvent.GetStarted) }
                        )
                        OciOnboardingStage.ACCOUNT_INFORMATION -> AccountInfoStage(
                            error = uiState.error,
                            onSubmit = { tenancy, user, region ->
                                viewModel.onEvent(OciOnboardingEvent.SubmitAccountInfo(tenancy, user, region))
                            }
                        )
                        OciOnboardingStage.API_KEY_GENERATION -> ApiKeyGenerationStage(
                            publicKeyPem = uiState.publicKeyPem,
                            error = uiState.error,
                            onGenerate = { viewModel.onEvent(OciOnboardingEvent.GenerateApiKey) }
                        )
                        OciOnboardingStage.API_KEY_REGISTRATION -> ApiKeyRegistrationStage(
                            publicKeyPem = uiState.publicKeyPem,
                            error = uiState.error,
                            onSubmit = { fp -> viewModel.onEvent(OciOnboardingEvent.SubmitFingerprint(fp)) }
                        )
                        OciOnboardingStage.CONNECTION_VERIFICATION -> ConnectionVerificationStage(
                            isVerifying = uiState.isVerifying,
                            isDiscovering = uiState.isDiscovering,
                            error = uiState.error,
                            onVerify = { viewModel.onEvent(OciOnboardingEvent.VerifyConnection) },
                            onContinue = { viewModel.onEvent(OciOnboardingEvent.DiscoverContext) }
                        )
                        OciOnboardingStage.OCI_CONTEXT_DISCOVERY -> ContextDiscoveryStage(
                            isDiscovering = uiState.isDiscovering,
                            error = uiState.error,
                            onDiscover = { viewModel.onEvent(OciOnboardingEvent.DiscoverContext) }
                        )
                        OciOnboardingStage.HOST_CONFIGURATION -> HostConfigurationStage(
                            uiState = uiState,
                            onSelectCompartment = { viewModel.onEvent(OciOnboardingEvent.SelectCompartment(it)) },
                            onSelectAd = { viewModel.onEvent(OciOnboardingEvent.SelectAvailabilityDomain(it)) },
                            onSelectShape = { viewModel.onEvent(OciOnboardingEvent.SelectShape(it)) },
                            onSelectImage = { viewModel.onEvent(OciOnboardingEvent.SelectImage(it)) },
                            onContinue = {
                                if (uiState.vmSshPublicKey != null) {
                                    viewModel.onEvent(OciOnboardingEvent.StartProvisioning)
                                } else {
                                    viewModel.onEvent(OciOnboardingEvent.GenerateVmSshKey)
                                }
                            }
                        )
                        OciOnboardingStage.NETWORK_PROVISIONING,
                        OciOnboardingStage.SSH_KEY_GENERATION,
                        OciOnboardingStage.INSTANCE_PROVISIONING,
                        OciOnboardingStage.SSH_VERIFICATION -> ProvisioningProgressStage(
                            stage = targetStage,
                            provisioningState = uiState.provisioningState,
                            error = uiState.error,
                            onStart = { viewModel.onEvent(OciOnboardingEvent.StartProvisioning) }
                        )
                        OciOnboardingStage.HOST_REGISTRATION -> HostRegistrationStage(
                            publicIp = uiState.provisionedPublicIp,
                            error = uiState.error
                        )
                        OciOnboardingStage.SHELL_READY -> ShellReadyStage(
                            publicIp = uiState.provisionedPublicIp,
                            onFinish = onShellReady
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Progress bar
// ---------------------------------------------------------------------------

private val ORDERED_STAGES = OciOnboardingStage.values().toList()

@Composable
private fun WizardProgressBar(stage: OciOnboardingStage) {
    val index = ORDERED_STAGES.indexOf(stage).coerceAtLeast(0)
    val progress = (index + 1).toFloat() / ORDERED_STAGES.size
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "oci_wizard_progress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(ImmersiveComponent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(ImmersivePrimary)
        )
    }
}

private fun stageLabel(stage: OciOnboardingStage): String = when (stage) {
    OciOnboardingStage.WELCOME -> "Get started"
    OciOnboardingStage.ACCOUNT_INFORMATION -> "Step 1 of 12 — Account info"
    OciOnboardingStage.API_KEY_GENERATION -> "Step 2 of 12 — Generate API key"
    OciOnboardingStage.API_KEY_REGISTRATION -> "Step 3 of 12 — Register key"
    OciOnboardingStage.CONNECTION_VERIFICATION -> "Step 4 of 12 — Verify connection"
    OciOnboardingStage.OCI_CONTEXT_DISCOVERY -> "Step 5 of 12 — Discover environment"
    OciOnboardingStage.HOST_CONFIGURATION -> "Step 6 of 12 — Configure host"
    OciOnboardingStage.NETWORK_PROVISIONING -> "Step 7 of 12 — Networking"
    OciOnboardingStage.SSH_KEY_GENERATION -> "Step 8 of 12 — VM SSH key"
    OciOnboardingStage.INSTANCE_PROVISIONING -> "Step 9 of 12 — Creating instance"
    OciOnboardingStage.SSH_VERIFICATION -> "Step 10 of 12 — Verifying reachability"
    OciOnboardingStage.HOST_REGISTRATION -> "Step 11 of 12 — Registering host"
    OciOnboardingStage.SHELL_READY -> "Step 12 of 12 — Ready"
}

// ---------------------------------------------------------------------------
// Stage 0: Welcome
// ---------------------------------------------------------------------------

@Composable
private fun WelcomeStage(onGetStarted: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ImmersivePrimary)
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = ImmersiveOnPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Provision a free Oracle Cloud VM",
            color = ImmersiveTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This walks through creating a VM on Oracle's Always Free tier and adding it straight to Bin Box as a host — no separate SSH setup needed.",
            color = ImmersiveTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        InfoCard(
            icon = Icons.Default.Info,
            title = "You'll need",
            body = "An Oracle Cloud account, its tenancy and user OCIDs, and your home region — all available from the OCI console under Profile → Tenancy."
        )
        Spacer(Modifier.height(12.dp))
        InfoCard(
            icon = Icons.Default.Lock,
            title = "Nothing leaves this device unnecessarily",
            body = "The API signing key is generated on-device and stored in the Android Keystore. Only its public half is ever sent to Oracle."
        )
        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Get started", onClick = onGetStarted, modifier = Modifier.testTag("oci_get_started"))
    }
}

// ---------------------------------------------------------------------------
// Stage 1: Account information
// ---------------------------------------------------------------------------

@Composable
private fun AccountInfoStage(
    error: String?,
    onSubmit: (tenancyOcid: String, userOcid: String, region: String) -> Unit
) {
    var tenancyOcid by remember { mutableStateOf("") }
    var userOcid by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }

    StageHeader("Account information", "Find these in the OCI console under your profile menu → Tenancy, and the region selector top-right.")

    LabeledField("Tenancy OCID", tenancyOcid, { tenancyOcid = it }, placeholder = "ocid1.tenancy.oc1..aaaa...")
    Spacer(Modifier.height(12.dp))
    LabeledField("User OCID", userOcid, { userOcid = it }, placeholder = "ocid1.user.oc1..aaaa...")
    Spacer(Modifier.height(12.dp))
    LabeledField("Home region", region, { region = it }, placeholder = "e.g. us-ashburn-1")

    error?.let { ErrorBanner(it) }

    Spacer(Modifier.height(24.dp))
    val valid = tenancyOcid.isNotBlank() && userOcid.isNotBlank() && region.isNotBlank()
    PrimaryButton(
        text = "Continue",
        enabled = valid,
        onClick = { onSubmit(tenancyOcid.trim(), userOcid.trim(), region.trim()) },
        modifier = Modifier.testTag("oci_submit_account_info")
    )
}

// ---------------------------------------------------------------------------
// Stage 2: API key generation
// ---------------------------------------------------------------------------

@Composable
private fun ApiKeyGenerationStage(
    publicKeyPem: String?,
    error: String?,
    onGenerate: () -> Unit
) {
    StageHeader("Generate an API signing key", "This key pair is created on-device and the private half never leaves the Android Keystore.")

    if (publicKeyPem == null) {
        InfoCard(
            icon = Icons.Default.VpnKey,
            title = "Ready to generate",
            body = "Tapping below creates a 2048-bit RSA key pair backed by Android's hardware keystore where available."
        )
        error?.let { ErrorBanner(it) }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Generate key", onClick = onGenerate, modifier = Modifier.testTag("oci_generate_api_key"))
    } else {
        Text("Public key generated", color = ImmersiveStatusGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        CopyableCodeBlock(publicKeyPem)
        Spacer(Modifier.height(16.dp))
        Text(
            "This moves you to the next step automatically. Copy the key above before continuing — you'll paste it into the OCI console there.",
            color = ImmersiveTextSecondary,
            fontSize = 12.sp
        )
    }
}

// ---------------------------------------------------------------------------
// Stage 3: API key registration (paste fingerprint back)
// ---------------------------------------------------------------------------

@Composable
private fun ApiKeyRegistrationStage(
    publicKeyPem: String?,
    error: String?,
    onSubmit: (String) -> Unit
) {
    var fingerprint by remember { mutableStateOf("") }

    StageHeader("Register the key with Oracle", "In the OCI console: Profile → My profile → API keys → Add API key → Paste public key, then paste the public key below.")

    publicKeyPem?.let {
        CopyableCodeBlock(it)
        Spacer(Modifier.height(16.dp))
    }

    Text(
        "Once added, Oracle shows a fingerprint like aa:bb:cc:...:zz. Paste it here.",
        color = ImmersiveTextSecondary,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(8.dp))
    LabeledField(
        "Fingerprint",
        fingerprint,
        { fingerprint = it },
        placeholder = "aa:bb:cc:dd:ee:ff:...:zz",
        monospace = true
    )

    error?.let { ErrorBanner(it) }

    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = "Continue",
        enabled = fingerprint.isNotBlank(),
        onClick = { onSubmit(fingerprint.trim()) },
        modifier = Modifier.testTag("oci_submit_fingerprint")
    )
}

// ---------------------------------------------------------------------------
// Stage 4: Connection verification
// ---------------------------------------------------------------------------

@Composable
private fun ConnectionVerificationStage(
    isVerifying: Boolean,
    isDiscovering: Boolean,
    error: String?,
    onVerify: () -> Unit,
    onContinue: () -> Unit
) {
    // No dedicated "verified" flag on OciOnboardingUiState — verifyConnection()
    // persists success to the session, not the UI state. Track it locally:
    // a completed attempt (isVerifying dropped) with no error means success.
    var attempted by remember { mutableStateOf(false) }
    val verified = attempted && !isVerifying && error == null

    StageHeader("Verify the connection", "A single harmless authenticated request — confirms signing and account info are both correct before anything gets created.")

    when {
        isVerifying -> LoadingRow("Verifying with Oracle Cloud…")
        isDiscovering -> LoadingRow("Loading your OCI environment…")
        error != null -> {
            ErrorBanner(error)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = "Retry",
                onClick = { attempted = true; onVerify() },
                modifier = Modifier.testTag("oci_retry_verify")
            )
        }
        verified -> {
            InfoCard(icon = Icons.Default.CheckCircle, title = "Connection verified", body = "Signing and account info both check out.")
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.testTag("oci_continue_after_verify"))
        }
        else -> PrimaryButton(
            text = "Verify connection",
            onClick = { attempted = true; onVerify() },
            modifier = Modifier.testTag("oci_verify_connection")
        )
    }
}

// ---------------------------------------------------------------------------
// Stage 5: Context discovery
// ---------------------------------------------------------------------------

@Composable
private fun ContextDiscoveryStage(
    isDiscovering: Boolean,
    error: String?,
    onDiscover: () -> Unit
) {
    LaunchedEffect(Unit) { onDiscover() }

    StageHeader("Discovering your environment", "Loading compartments and availability domains from your tenancy.")

    when {
        isDiscovering -> LoadingRow("Fetching compartments and availability domains…")
        error != null -> {
            ErrorBanner(error)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Retry", onClick = onDiscover, modifier = Modifier.testTag("oci_retry_discovery"))
        }
        else -> LoadingRow("Almost there…")
    }
}

// ---------------------------------------------------------------------------
// Stage 6: Host configuration (compartment / AD / shape / image)
// ---------------------------------------------------------------------------

@Composable
private fun HostConfigurationStage(
    uiState: OciOnboardingUiState,
    onSelectCompartment: (String) -> Unit,
    onSelectAd: (String) -> Unit,
    onSelectShape: (String) -> Unit,
    onSelectImage: (String) -> Unit,
    onContinue: () -> Unit
) {
    StageHeader("Configure your host", "Pick where the VM lives and what it runs. Compartment defaults to your tenancy root.")

    SelectionSection(
        title = "Compartment",
        items = uiState.discoveredCompartments,
        selectedId = uiState.context.selectedCompartmentOcid,
        idOf = { it.id },
        labelOf = { it.name },
        onSelect = { onSelectCompartment(it.id) }
    )

    Spacer(Modifier.height(16.dp))
    SelectionSection(
        title = "Availability domain",
        items = uiState.discoveredAvailabilityDomains,
        selectedId = uiState.context.selectedAvailabilityDomain,
        idOf = { it },
        labelOf = { it },
        onSelect = { onSelectAd(it) }
    )

    if (uiState.context.selectedAvailabilityDomain != null) {
        Spacer(Modifier.height(16.dp))
        SelectionSection(
            title = "Shape (Always Free)",
            items = uiState.discoveredShapes,
            selectedId = uiState.context.selectedShapeName,
            idOf = { it },
            labelOf = { shape ->
                when (shape) {
                    OciFreeTierShapes.AMPERE_A1_FLEX -> "$shape (Ampere, flexible)"
                    OciFreeTierShapes.E2_MICRO -> "$shape (AMD, fixed)"
                    else -> shape
                }
            },
            onSelect = { onSelectShape(it) }
        )
    }

    if (uiState.context.selectedShapeName != null) {
        Spacer(Modifier.height(16.dp))
        SelectionSection(
            title = "Image",
            items = uiState.discoveredImages,
            selectedId = uiState.context.selectedImageOcid,
            idOf = { it.id },
            labelOf = { "${it.operatingSystem} ${it.operatingSystemVersion}" },
            onSelect = { onSelectImage(it.id) }
        )
    }

    uiState.error?.let { ErrorBanner(it) }

    Spacer(Modifier.height(24.dp))
    val ready = uiState.context.selectedCompartmentOcid != null &&
        uiState.context.selectedAvailabilityDomain != null &&
        uiState.context.selectedShapeName != null &&
        uiState.context.selectedImageOcid != null
    PrimaryButton(
        text = if (uiState.vmSshPublicKey != null) "Start provisioning" else "Continue",
        enabled = ready && !uiState.isGeneratingVmSshKey,
        onClick = onContinue,
        modifier = Modifier.testTag("oci_start_provisioning")
    )
    if (uiState.isGeneratingVmSshKey) {
        Spacer(Modifier.height(12.dp))
        LoadingRow("Generating VM SSH key…")
    }
}

@Composable
private fun <T> SelectionSection(
    title: String,
    items: List<T>,
    selectedId: String?,
    idOf: (T) -> String,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(title, color = ImmersiveTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    if (items.isEmpty()) {
        Text("None available yet.", color = ImmersiveTextMuted, fontSize = 12.sp)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveSurface)
            .border(1.dp, ImmersiveBorderVerySubtle, RoundedCornerShape(10.dp))
    ) {
        items.forEachIndexed { index, item ->
            val selected = idOf(item) == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .background(if (selected) ImmersiveComponent else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    labelOf(item),
                    color = if (selected) ImmersivePrimary else ImmersiveTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                }
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = ImmersiveBorderVerySubtle, thickness = 1.dp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Stages 7-10: provisioning progress (network / ssh key / instance / ssh verify)
// ---------------------------------------------------------------------------

@Composable
private fun ProvisioningProgressStage(
    stage: OciOnboardingStage,
    provisioningState: OciProvisioningState?,
    error: String?,
    onStart: () -> Unit
) {
    StageHeader("Provisioning your VM", "This creates real Oracle Cloud infrastructure: a VCN, subnet, internet gateway, and the compute instance itself. Usually takes a couple of minutes.")

    val steps = listOf(
        "Network" to setOf(OciProvisioningState.NETWORK_DISCOVERED, OciProvisioningState.NETWORK_CREATING, OciProvisioningState.NETWORK_READY),
        "SSH key" to setOf(OciProvisioningState.SHAPE_SELECTED, OciProvisioningState.SSH_KEY_READY),
        "Instance" to setOf(OciProvisioningState.INSTANCE_CREATING, OciProvisioningState.INSTANCE_PROVISIONING, OciProvisioningState.INSTANCE_RUNNING),
        "Public IP" to setOf(OciProvisioningState.PUBLIC_IP_DISCOVERED)
    )
    val currentIndex = steps.indexOfFirst { (_, states) -> provisioningState in states }
        .let { if (it == -1) 0 else it }

    Column(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, (label, _) ->
            val done = index < currentIndex || provisioningState == OciProvisioningState.PUBLIC_IP_DISCOVERED
            val active = index == currentIndex && !done
            ProvisioningStepRow(label = label, done = done, active = active)
        }
    }

    error?.let {
        Spacer(Modifier.height(16.dp))
        ErrorBanner(it)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "Retry", onClick = onStart, modifier = Modifier.testTag("oci_retry_provision"))
    }

    if (error == null && provisioningState == null) {
        Spacer(Modifier.height(16.dp))
        LoadingRow("Starting…")
    }
}

@Composable
private fun ProvisioningStepRow(label: String, done: Boolean, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> ImmersiveStatusGreen
                        active -> ImmersivePrimary
                        else -> ImmersiveComponent
                    }
                )
        ) {
            if (done) {
                Icon(Icons.Default.Check, contentDescription = null, tint = ImmersiveOnPrimary, modifier = Modifier.size(14.dp))
            } else if (active) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = ImmersiveOnPrimary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = if (done || active) ImmersiveTextPrimary else ImmersiveTextMuted,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ---------------------------------------------------------------------------
// Stage 11: host registration
// ---------------------------------------------------------------------------

@Composable
private fun HostRegistrationStage(publicIp: String?, error: String?) {
    StageHeader("Registering the host", "Adding the new VM to Bin Box's host list so you can connect from the Terminal tab like any other server.")

    publicIp?.let {
        InfoCard(icon = Icons.Default.Dns, title = "Instance is up", body = "Public IP: $it")
        Spacer(Modifier.height(16.dp))
    }

    if (error != null) {
        ErrorBanner(error)
    } else {
        LoadingRow("Registering…")
    }
}

// ---------------------------------------------------------------------------
// Stage 12: shell ready
// ---------------------------------------------------------------------------

@Composable
private fun ShellReadyStage(publicIp: String?, onFinish: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ImmersiveStatusGreen)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = ImmersiveOnPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Your VM is ready", color = ImmersiveTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "It's been added to your hosts. Note: SSH reachability isn't pre-verified yet — if the first connection attempt fails, give the instance a minute to finish booting and try again.",
            color = ImmersiveTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        publicIp?.let {
            Spacer(Modifier.height(16.dp))
            InfoCard(icon = Icons.Default.Dns, title = "Public IP", body = it)
        }
        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Go to hosts", onClick = onFinish, modifier = Modifier.testTag("oci_finish"))
    }
}

// ---------------------------------------------------------------------------
// Shared building blocks
// ---------------------------------------------------------------------------

@Composable
private fun StageHeader(title: String, subtitle: String) {
    Text(title, color = ImmersiveTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = ImmersiveTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    monospace: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = ImmersiveTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = ImmersiveTextMuted, fontSize = 13.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = ImmersiveTextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ImmersiveSurface,
                unfocusedContainerColor = ImmersiveSurface,
                focusedBorderColor = ImmersivePrimary,
                unfocusedBorderColor = ImmersiveBorderSubtle,
                cursorColor = ImmersivePrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ImmersivePrimary,
            contentColor = ImmersiveOnPrimary,
            disabledContainerColor = ImmersiveComponent,
            disabledContentColor = ImmersiveTextMuted
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ImmersiveSurface)
            .border(1.dp, ImmersiveBorderVerySubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = ImmersiveTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(body, color = ImmersiveTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveStatusRed.copy(alpha = 0.12f))
            .border(1.dp, ImmersiveStatusRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = ImmersiveStatusRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = ImmersiveTextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun LoadingRow(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ImmersivePrimary)
        Spacer(Modifier.width(12.dp))
        Text(message, color = ImmersiveTextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun CopyableCodeBlock(text: String) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveTerminalCardBg)
            .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Public key", color = ImmersiveTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(text)) },
                modifier = Modifier.size(28.dp).testTag("oci_copy_public_key")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            color = ImmersiveTextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )
    }
}

