package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Top-level Orchestrator UI for the 12-step Oracle Cloud onboarding wizard.
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
    var showStartOverConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(stage) {
        if (stage == OciOnboardingStage.SHELL_READY) onShellReady()
    }

    val canGoBack = stage != OciOnboardingStage.WELCOME && stage != OciOnboardingStage.SHELL_READY

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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (canGoBack) {
                                IconButton(
                                    onClick = { viewModel.onEvent(OciOnboardingEvent.GoBack) },
                                    modifier = Modifier.testTag("oci_wizard_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ImmersiveTextPrimary)
                                }
                            } else {
                                IconButton(onClick = {
                                    if (stage != OciOnboardingStage.SHELL_READY) {
                                        viewModel.onEvent(OciOnboardingEvent.Cancel)
                                    }
                                    onDismiss()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextSecondary)
                                }
                            }
                            Column {
                                Text(
                                    "Oracle Cloud Setup",
                                    color = ImmersiveTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    stageLabel(stage),
                                    color = ImmersiveTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (stage != OciOnboardingStage.WELCOME && stage != OciOnboardingStage.SHELL_READY) {
                                IconButton(
                                    onClick = { showStartOverConfirm = true },
                                    modifier = Modifier.testTag("oci_wizard_start_over_icon")
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Start Over", tint = ImmersiveTextSecondary)
                                }
                            }
                            IconButton(onClick = {
                                if (stage != OciOnboardingStage.SHELL_READY) {
                                    viewModel.onEvent(OciOnboardingEvent.Cancel)
                                }
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextSecondary)
                            }
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
                            tenancyOcidPrefill = uiState.tenancyOcid,
                            userOcidPrefill = uiState.userOcid,
                            regionPrefill = uiState.region,
                            error = uiState.error,
                            onImportConfig = { viewModel.onEvent(OciOnboardingEvent.ImportConfig(it)) },
                            onSubmit = { tenancy, user, region ->
                                viewModel.onEvent(OciOnboardingEvent.SubmitAccountInfo(tenancy, user, region))
                            }
                        )
                        OciOnboardingStage.API_KEY_GENERATION -> ApiKeyGenerationStage(
                            publicKeyPem = uiState.publicKeyPem,
                            error = uiState.error,
                            onGenerate = { viewModel.onEvent(OciOnboardingEvent.GenerateApiKey) },
                            onContinue = { viewModel.onEvent(OciOnboardingEvent.ContinueToKeyRegistration) }
                        )
                        OciOnboardingStage.API_KEY_REGISTRATION -> ApiKeyRegistrationStage(
                            publicKeyPem = uiState.publicKeyPem,
                            pendingFingerprint = uiState.pendingFingerprint,
                            error = uiState.error,
                            onSubmit = { fp -> viewModel.onEvent(OciOnboardingEvent.SubmitFingerprint(fp)) }
                        )
                        OciOnboardingStage.CONNECTION_VERIFICATION -> ConnectionVerificationStage(
                            isVerifying = uiState.isVerifying,
                            isDiscovering = uiState.isDiscovering,
                            error = uiState.error,
                            diagnostics = uiState.diagnostics,
                            onVerify = { viewModel.onEvent(OciOnboardingEvent.VerifyConnection) },
                            onContinue = { viewModel.onEvent(OciOnboardingEvent.DiscoverContext) },
                            onEditAccountInfo = { viewModel.onEvent(OciOnboardingEvent.EditAccountInfo) },
                            onStartOver = { viewModel.onEvent(OciOnboardingEvent.StartOver) }
                        )
                        OciOnboardingStage.OCI_CONTEXT_DISCOVERY -> ContextDiscoveryStage(
                            isDiscovering = uiState.isDiscovering,
                            error = uiState.error,
                            diagnostics = uiState.diagnostics,
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
                            diagnostics = uiState.diagnostics,
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

    if (showStartOverConfirm) {
        AlertDialog(
            onDismissRequest = { showStartOverConfirm = false },
            title = { Text("Start over setup?", color = ImmersiveTextPrimary) },
            text = { Text("This will reset your wizard progress back to the beginning.", color = ImmersiveTextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showStartOverConfirm = false
                    viewModel.onEvent(OciOnboardingEvent.StartOver)
                }) {
                    Text("Start Over", color = ImmersiveStatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartOverConfirm = false }) {
                    Text("Cancel", color = ImmersiveTextSecondary)
                }
            },
            containerColor = ImmersiveSurface
        )
    }

    uiState.pendingResumeStage?.let { resumeStage ->
        AlertDialog(
            onDismissRequest = { /* must choose explicitly */ },
            title = { Text("Continue setup?", color = ImmersiveTextPrimary) },
            text = {
                Text(
                    "You have an Oracle Cloud setup in progress, last at ${stageLabel(resumeStage)}. " +
                        "Continue where you left off, or start over from scratch?",
                    color = ImmersiveTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(OciOnboardingEvent.ResumeSession) },
                    enabled = !uiState.isResuming
                ) {
                    Text(if (uiState.isResuming) "Resuming…" else "Continue", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.onEvent(OciOnboardingEvent.StartOver) },
                    enabled = !uiState.isResuming
                ) {
                    Text("Start Over", color = ImmersiveStatusRed)
                }
            },
            containerColor = ImmersiveSurface
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
