package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.oci.api.compute.Instance
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun ExistingInstancePromptStage(
    instances: List<Instance>,
    onUseExisting: () -> Unit,
    onProvisionSecond: () -> Unit
) {
    StageHeader(
        title = "Existing VM Detected",
        subtitle = "We found ${if (instances.size == 1) "an existing VM" else "${instances.size} existing VMs"} already active in your Oracle Cloud account."
    )

    Spacer(Modifier.height(12.dp))

    // List found instances
    instances.take(3).forEach { instance ->
        Surface(
            color = ImmersiveSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ImmersivePrimary.copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(instance.displayName, color = ImmersiveTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "${instance.shape} • ${instance.lifecycleState}",
                        color = if (instance.lifecycleState == "RUNNING") ImmersiveStatusGreen else ImmersiveTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // Guidance Card
    Surface(
        color = ImmersiveSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Did you delete app data / reinstall?", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "If this VM was created in an earlier install and you saved its SSH private key or login info, you do not need to create a new VM. Simply tap 'Use Existing Host' and go to '+ New Host' on the Hosts tab to connect immediately.",
                color = ImmersiveTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Always Free Quota Limit", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Oracle Cloud limits Always Free accounts to 4 OCPUs / 24 GB RAM (Ampere A1) or 2 AMD micro VMs. If you want to create a second VM alongside your existing one, ensure you have remaining quota.",
                color = ImmersiveTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    PrimaryButton(
        text = "Use existing host (I have saved key)",
        onClick = onUseExisting,
        modifier = Modifier.testTag("oci_use_existing_host_btn")
    )

    Spacer(Modifier.height(10.dp))

    SecondaryButton(
        text = "Provision a second VM",
        onClick = onProvisionSecond,
        modifier = Modifier.testTag("oci_provision_second_vm_btn")
    )
}
