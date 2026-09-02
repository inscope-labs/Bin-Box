package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.oci.wizard.LocalOciWizardLauncher
import com.inscopelabs.abx.binbox.ui.components.hosts.AddEditHostDialog
import com.inscopelabs.abx.binbox.ui.components.hosts.HostCard
import com.inscopelabs.abx.binbox.ui.components.hosts.QuickConnectCard
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostsScreen(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier
) {
    val hosts by viewModel.hosts.collectAsStateWithLifecycle()
    val keys by viewModel.keys.collectAsStateWithLifecycle()
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val launchOci = LocalOciWizardLauncher.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("All") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var hostToEdit by remember { mutableStateOf<HostEntity?>(null) }
    var hostToDelete by remember { mutableStateOf<HostEntity?>(null) }
    var showQuickConnect by remember { mutableStateOf(false) }
    var showPackagesSheet by remember { mutableStateOf(false) }

    if (showPackagesSheet) {
        LocalShellModulesSheet(onDismiss = { showPackagesSheet = false })
    }

    val groupTags = listOf("All", "Favorites", "Workspace (${activeWorkspace.name})", "Cloud", "HomeLab", "Production", "Local", "IoT")

    val filteredHosts = hosts.filter { host ->
        val matchesQuery = host.label.contains(searchQuery, ignoreCase = true) ||
                host.host.contains(searchQuery, ignoreCase = true) ||
                host.username.contains(searchQuery, ignoreCase = true)

        val matchesGroup = when {
            selectedGroupFilter == "All" -> true
            selectedGroupFilter == "Favorites" -> host.isFavorite
            selectedGroupFilter.startsWith("Workspace") -> activeWorkspace.hostProfileIds.contains(host.id)
            else -> host.groupTag.equals(selectedGroupFilter, ignoreCase = true)
        }

        matchesQuery && matchesGroup
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { launchOci() },
                    containerColor = Slate800,
                    contentColor = CyanGlow,
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Free Oracle VM", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("add_oci_host_fab")
                )
                FloatingActionButton(
                    onClick = {
                        hostToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = CyanAccent,
                    contentColor = Slate950,
                    modifier = Modifier.testTag("add_host_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add host")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Screen Header & Quick Connect Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Host Connections",
                        color = Slate100,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${hosts.size} configured hosts",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { showPackagesSheet = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate800)
                    ) {
                        Icon(Icons.Default.Extension, "Local Packages", tint = CyanGlow, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { viewModel.pingAllHosts() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate800)
                    ) {
                        Icon(Icons.Outlined.NetworkPing, "Ping all", tint = CyanGlow, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { showQuickConnect = !showQuickConnect },
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showQuickConnect) CyanAccent else Slate800)
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            "Quick Connect",
                            tint = if (showQuickConnect) Slate950 else AmberGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Connect Drawer
            AnimatedVisibility(visible = showQuickConnect) {
                QuickConnectCard(
                    onConnect = { user, host, port ->
                        viewModel.connectToHost(
                            HostEntity(
                                label = "Quick: $host",
                                host = host,
                                port = port,
                                protocol = "SSH",
                                username = user,
                                authType = "PASSWORDLESS",
                                groupTag = "Cloud"
                            )
                        )
                        showQuickConnect = false
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Search Field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search hosts by name, IP, or user...", color = Slate600, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Group Tags Filter
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(groupTags) { tag ->
                    val isSelected = tag == selectedGroupFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroupFilter = tag },
                        label = {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanAccent,
                            selectedLabelColor = Slate950,
                            containerColor = Slate900,
                            labelColor = Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Slate800,
                            selectedBorderColor = CyanAccent
                        )
                    )
                }
            }

            // If a workspace filter or active workspace has members, provide a quick launch action
            if (activeWorkspace.hostProfileIds.isNotEmpty()) {
                val workspaceHostCount = hosts.count { activeWorkspace.hostProfileIds.contains(it.id) }
                if (workspaceHostCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ImmersivePrimary.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.launchAllInWorkspace(activeWorkspace)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        "Workspace: ${activeWorkspace.name}",
                                        color = ImmersiveTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$workspaceHostCount configured host${if (workspaceHostCount > 1) "s" else ""}",
                                        color = ImmersiveTextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ImmersivePrimary,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
                                    Text(
                                        "Launch All",
                                        color = ImmersiveOnPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Hosts List
            if (filteredHosts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No host connections match your filter", color = Slate400, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(24.dp))
                    com.inscopelabs.abx.binbox.oci.wizard.OciFreeTierPromoCard(
                        onLaunchWizard = { launchOci() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredHosts, key = { it.id }) { host ->
                        HostCard(
                            host = host,
                            onConnect = { viewModel.connectToHost(host) },
                            onPing = { viewModel.pingHost(host) },
                            onToggleFavorite = { viewModel.toggleHostFavorite(host) },
                            onEdit = {
                                hostToEdit = host
                                showAddEditDialog = true
                            },
                            onDelete = { hostToDelete = host }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Host Dialog
    if (showAddEditDialog) {
        AddEditHostDialog(
            initialHost = hostToEdit,
            savedKeys = keys,
            onDismiss = { showAddEditDialog = false },
            onSave = { host ->
                viewModel.saveHost(host)
                showAddEditDialog = false
            },
            onLaunchOciWizard = {
                showAddEditDialog = false
                launchOci()
            }
        )
    }

    // Irrevocable Host Deletion Confirmation Dialog
    hostToDelete?.let { host ->
        AlertDialog(
            onDismissRequest = { hostToDelete = null },
            containerColor = ImmersiveSurface,
            title = {
                Text("Delete Host", color = ImmersiveTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to delete '${host.label}'? This action cannot be undone.", color = ImmersiveTextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHost(host)
                        hostToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveStatusRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { hostToDelete = null }) {
                    Text("Cancel", color = ImmersiveTextSecondary)
                }
            }
        )
    }
}
