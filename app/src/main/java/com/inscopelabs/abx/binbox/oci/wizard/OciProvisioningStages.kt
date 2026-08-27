package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
fun ProvisioningProgressStage(
    stage: OciOnboardingStage,
    provisioningState: OciProvisioningState?,
    error: String?,
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
fun HostRegistrationStage(publicIp: String?, error: String?) {
    StageHeader("Registering the host", "Adding the new VM to Bin Box's host list so you can connect from the Terminal tab.")

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

@Composable
fun ShellReadyStage(publicIp: String?, onFinish: () -> Unit) {
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
            "It's been added to your hosts list. If the first connection attempt times out, give the instance another minute to finish initial cloud-init boot.",
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
