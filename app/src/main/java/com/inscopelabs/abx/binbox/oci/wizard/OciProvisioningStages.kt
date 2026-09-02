package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.oci.provisioning.OciFreeTierShapes
import com.inscopelabs.abx.binbox.oci.provisioning.OciProvisioningState
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun HostConfigurationStage(
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

    uiState.error?.let {
        ErrorBanner(it)
        if (uiState.diagnostics != null) {
            Spacer(Modifier.height(14.dp))
            OciDiagnosticsCard(uiState.diagnostics)
        }
    }

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
fun ProvisioningProgressStage(
    stage: OciOnboardingStage,
    provisioningState: OciProvisioningState?,
    error: String?,
    diagnostics: OciVerificationDiagnostics? = null,
    onStart: () -> Unit
) {
    StageHeader("Provisioning your VM", "This creates real Oracle Cloud infrastructure: a VCN, subnet, internet gateway, and compute instance. Usually takes 1-2 minutes.")

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
        if (diagnostics != null) {
            Spacer(Modifier.height(14.dp))
            OciDiagnosticsCard(diagnostics)
        }
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

@Composable
fun HostRegistrationStage(publicIp: String?, error: String?, onRetry: (() -> Unit)? = null) {
    StageHeader("Registering the host", "Adding the new VM to BinBox's host list so you can connect from the Terminal tab.")

    publicIp?.let {
        InfoCard(icon = Icons.Default.Dns, title = "Instance is up", body = "Public IP: $it")
        Spacer(Modifier.height(16.dp))
    }

    if (error != null) {
        ErrorBanner(error)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Retry", onClick = onRetry, modifier = Modifier.testTag("oci_retry_registration"))
        }
    } else {
        LoadingRow("Registering…")
    }
}

@Composable
fun ShellReadyStage(
    publicIp: String?,
    username: String? = "opc",
    privateKey: String? = null,
    onFinish: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copiedKey by remember { mutableStateOf(false) }
    var copiedDetails by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ImmersiveStatusGreen)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = ImmersiveOnPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Your VM is ready", color = ImmersiveTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "It's been added to your hosts list. If the first connection attempt times out, give the instance another minute to finish initial cloud-init boot.",
            color = ImmersiveTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )

        publicIp?.let { ip ->
            Spacer(Modifier.height(14.dp))
            InfoCard(icon = Icons.Default.Dns, title = "Public IP", body = ip)
        }

        Spacer(Modifier.height(14.dp))

        // Save Authentication Info Card
        Surface(
            color = ImmersiveSurfaceElevated,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save your authentication info", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "If you ever clear app data or reinstall BinBox, the local SSH private key will be deleted. Save these credentials in a safe place so you can reconnect anytime from '+ New Host' on the Hosts tab.",
                    color = ImmersiveTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Username: ${username ?: "opc"} • Port: 22",
                    color = ImmersiveTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (privateKey != null) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(privateKey))
                                copiedKey = true
                            },
                            modifier = Modifier.weight(1f).testTag("oci_copy_private_key_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextPrimary)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (copiedKey) "Key Copied!" else "Copy Private Key", fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val details = buildString {
                                appendLine("Host: ${publicIp ?: "unknown"}")
                                appendLine("User: ${username ?: "opc"}")
                                appendLine("Port: 22")
                                if (privateKey != null) {
                                    appendLine("Private Key:")
                                    appendLine(privateKey)
                                }
                            }
                            clipboardManager.setText(AnnotatedString(details))
                            copiedDetails = true
                        },
                        modifier = Modifier.weight(1f).testTag("oci_copy_details_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextPrimary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (copiedDetails) "Details Copied!" else "Copy All Details", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Go to hosts", onClick = onFinish, modifier = Modifier.testTag("oci_finish"))
    }
}
