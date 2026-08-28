package com.inscopelabs.abx.binbox.oci.management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Landing screen for OCI once provisioning has verifiably completed (see
 * [OciProvisioningStatus]) — replaces the onboarding wizard as the destination for every OCI
 * entry point (FAB, promo card, quick-action tile, settings/terminal launchers) from that point
 * on. Lists the provisioned VM(s), lets the user connect or ping them via the existing terminal
 * stack, and still offers provisioning another VM (the Always Free tier supports more than one
 * instance) rather than gating that off.
 */
@Composable
fun OciManagementScreen(
    hosts: List<HostEntity>,
    onConnect: (HostEntity) -> Unit,
    onPing: (HostEntity) -> Unit,
    onToggleFavorite: (HostEntity) -> Unit,
    onProvisionAnother: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("oci_management_screen"),
        containerColor = ImmersiveBg,
        topBar = {
            Surface(color = ImmersiveSurface, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Oracle Cloud VMs", color = ImmersiveTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${hosts.size} instance${if (hosts.size == 1) "" else "s"} provisioned", color = ImmersiveTextMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("oci_management_close")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextSecondary)
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onProvisionAnother,
                containerColor = ImmersiveComponent,
                contentColor = CyanAccent,
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("Provision another VM", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("oci_provision_another_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            items(hosts, key = { it.id }) { host ->
                OciManagedHostCard(host, onConnect = { onConnect(host) }, onPing = { onPing(host) }, onToggleFavorite = { onToggleFavorite(host) })
            }
        }
    }
}

@Composable
private fun OciManagedHostCard(
    host: HostEntity,
    onConnect: () -> Unit,
    onPing: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ImmersiveSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier.fillMaxWidth().testTag("oci_managed_host_${host.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(host.label, color = ImmersiveTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${host.username}@${host.host}:${host.port}", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (host.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (host.isFavorite) ImmersivePrimary else ImmersiveTextMuted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersivePrimary, contentColor = ImmersiveOnPrimary),
                    modifier = Modifier.weight(1f).testTag("oci_managed_host_connect_${host.id}")
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Connect", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(onClick = onPing, modifier = Modifier.testTag("oci_managed_host_ping_${host.id}")) {
                    val latency = host.lastLatencyMs
                    Icon(
                        Icons.Outlined.NetworkPing,
                        contentDescription = "Ping",
                        tint = if (latency != null) ImmersiveStatusGreen else ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    if (latency != null) {
                        Spacer(Modifier.width(6.dp))
                        Text("${latency}ms", fontSize = 12.sp, color = ImmersiveTextSecondary)
                    }
                }
            }
        }
    }
}
