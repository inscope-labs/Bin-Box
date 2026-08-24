package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.terminal.model.ProtocolType
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel
import java.text.SimpleDateFormat
import java.util.*

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

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("All") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var hostToEdit by remember { mutableStateOf<HostEntity?>(null) }
    var showQuickConnect by remember { mutableStateOf(false) }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No host connections match your filter", color = Slate400, fontSize = 13.sp)
                    }
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
                            onDelete = { viewModel.deleteHost(host) }
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
            }
        )
    }
}

@Composable
fun HostCard(
    host: HostEntity,
    onConnect: () -> Unit,
    onPing: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Protocol Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (host.protocol) {
                                    "SSH" -> ImmersivePrimary.copy(alpha = 0.15f)
                                    "LOCAL_SHELL" -> ImmersiveStatusGreen.copy(alpha = 0.15f)
                                    "DEMO_HOST" -> ImmersiveStatusAmber.copy(alpha = 0.15f)
                                    else -> ImmersiveComponent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (host.protocol) {
                                "SSH" -> Icons.Default.Terminal
                                "LOCAL_SHELL" -> Icons.Default.PhoneAndroid
                                "DEMO_HOST" -> Icons.Default.Cloud
                                else -> Icons.Default.Dns
                            },
                            contentDescription = null,
                            tint = when (host.protocol) {
                                "SSH" -> ImmersivePrimary
                                "LOCAL_SHELL" -> ImmersiveStatusGreen
                                "DEMO_HOST" -> ImmersiveStatusAmber
                                else -> ImmersiveTextPrimary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = host.label,
                            color = ImmersiveTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (host.protocol == "LOCAL_SHELL") "Android Shell" else "${host.username}@${host.host}:${host.port}",
                                color = ImmersiveTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (host.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (host.isFavorite) ImmersiveStatusAmber else ImmersiveTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Chips Row (Protocol, Group, Latency, Auth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Protocol Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                ) {
                    Text(
                        text = host.protocol,
                        color = ImmersivePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Group Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                ) {
                    Text(
                        text = host.groupTag,
                        color = ImmersiveTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Latency Badge
                host.lastLatencyMs?.let { latency ->
                    val latencyColor = when {
                        latency < 30 -> ImmersiveStatusGreen
                        latency < 100 -> ImmersiveStatusAmber
                        else -> ImmersiveStatusRed
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(latencyColor.copy(alpha = 0.15f))
                            .border(0.5.dp, latencyColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(latencyColor)
                        )
                        Text(
                            text = "${latency}ms",
                            color = latencyColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Connect, Ping, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPing,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Outlined.NetworkPing, null, tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.Edit, "Edit", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = ImmersiveStatusRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }

                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = ImmersiveOnPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickConnectCard(
    onConnect: (username: String, host: String, port: Int) -> Unit
) {
    var quickInput by remember { mutableStateOf("root@192.168.1.100:22") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveStatusAmber.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FlashOn, null, tint = ImmersiveStatusAmber, modifier = Modifier.size(18.dp))
                Text("Quick SSH Connect", color = ImmersiveTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    placeholder = { Text("user@host:port", color = ImmersiveTextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ImmersiveComponent,
                        unfocusedContainerColor = ImmersiveComponent,
                        focusedTextColor = ImmersiveTextPrimary,
                        unfocusedTextColor = ImmersiveTextPrimary,
                        cursorColor = ImmersivePrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val parsed = parseQuickConnect(quickInput)
                        onConnect(parsed.first, parsed.second, parsed.third)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveStatusAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text("Go", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun parseQuickConnect(input: String): Triple<String, String, Int> {
    var user = "root"
    var host = input.trim()
    var port = 22

    if (host.contains("@")) {
        val splitAt = host.split("@", limit = 2)
        user = splitAt[0]
        host = splitAt[1]
    }

    if (host.contains(":")) {
        val splitPort = host.split(":", limit = 2)
        host = splitPort[0]
        port = splitPort[1].toIntOrNull() ?: 22
    }

    return Triple(user, host, port)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHostDialog(
    initialHost: HostEntity?,
    savedKeys: List<com.inscopelabs.abx.binbox.data.entity.KeyEntity>,
    onDismiss: () -> Unit,
    onSave: (HostEntity) -> Unit
) {
    var label by remember { mutableStateOf(initialHost?.label ?: "") }
    var host by remember { mutableStateOf(initialHost?.host ?: "") }
    var portText by remember { mutableStateOf((initialHost?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(initialHost?.username ?: "root") }
    var protocol by remember { mutableStateOf(initialHost?.protocol ?: "SSH") }
    var authType by remember { mutableStateOf(initialHost?.authType ?: "PASSWORD") }
    var password by remember { mutableStateOf(initialHost?.password ?: "") }
    var selectedKeyId by remember { mutableStateOf<Long?>(initialHost?.keyId) }
    var groupTag by remember { mutableStateOf(initialHost?.groupTag ?: "Cloud") }
    var themeId by remember { mutableStateOf(initialHost?.themeId ?: "monokai_pro") }
    var shellProfileId by remember { mutableStateOf(initialHost?.shellProfileId ?: "default") }
    var initialDirectory by remember { mutableStateOf(initialHost?.initialDirectory ?: "") }
    var envVars by remember { mutableStateOf(initialHost?.envVarsJson ?: "") }
    var startupCommand by remember { mutableStateOf(initialHost?.startupCommand ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialHost == null) "Add Host Shell" else "Edit Host Shell",
                color = ImmersiveTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Protocol Selector
                item {
                    Text("Protocol", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SSH", "LOCAL_SHELL", "WEBSOCKET", "DEMO_HOST").forEach { proto ->
                            val isSelected = protocol == proto
                            val labelText = when (proto) {
                                "SSH" -> "SSH"
                                "LOCAL_SHELL" -> "Local"
                                "WEBSOCKET" -> "WS Relay"
                                "DEMO_HOST" -> "Demo"
                                else -> proto.take(5)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { protocol = proto }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = labelText,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Label
                item {
                    Text("Connection Label", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("e.g. HomeLab Server", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Host & Port (if not local)
                if (protocol != "LOCAL_SHELL") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(2.5f)) {
                                Text("Host / IP", color = ImmersiveTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = host,
                                    onValueChange = { host = it },
                                    placeholder = { Text("192.168.1.10", color = ImmersiveTextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = ImmersiveComponent,
                                        unfocusedContainerColor = ImmersiveComponent,
                                        focusedTextColor = ImmersiveTextPrimary,
                                        unfocusedTextColor = ImmersiveTextPrimary,
                                        cursorColor = ImmersivePrimary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Port", color = ImmersiveTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = portText,
                                    onValueChange = { portText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = ImmersiveComponent,
                                        unfocusedContainerColor = ImmersiveComponent,
                                        focusedTextColor = ImmersiveTextPrimary,
                                        unfocusedTextColor = ImmersiveTextPrimary,
                                        cursorColor = ImmersivePrimary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Username
                    item {
                        Text("Username", color = ImmersiveTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ImmersiveComponent,
                                unfocusedContainerColor = ImmersiveComponent,
                                focusedTextColor = ImmersiveTextPrimary,
                                unfocusedTextColor = ImmersiveTextPrimary,
                                cursorColor = ImmersivePrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }

                    // Auth Type
                    item {
                        Text("Authentication", color = ImmersiveTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("PASSWORD", "PRIVATE_KEY", "PASSWORDLESS").forEach { auth ->
                                val isSelected = authType == auth
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { authType = auth }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = when (auth) {
                                                "PASSWORD" -> "Password"
                                                "PRIVATE_KEY" -> "SSH Key"
                                                else -> "None"
                                            },
                                            color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Password or Key Dropdown
                    if (authType == "PASSWORD") {
                        item {
                            Text("Password", color = ImmersiveTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = ImmersiveComponent,
                                    unfocusedContainerColor = ImmersiveComponent,
                                    focusedTextColor = ImmersiveTextPrimary,
                                    unfocusedTextColor = ImmersiveTextPrimary,
                                    cursorColor = ImmersivePrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            null,
                                            tint = ImmersiveTextSecondary
                                        )
                                    }
                                },
                                singleLine = true
                            )
                        }
                    } else if (authType == "PRIVATE_KEY") {
                        item {
                            Text("Select SSH Key", color = ImmersiveTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (savedKeys.isEmpty()) {
                                Text("No SSH keys found in Key Manager. Create one first.", color = ImmersiveStatusAmber, fontSize = 11.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    savedKeys.forEach { key ->
                                        val isSelected = selectedKeyId == key.id
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) ImmersivePrimary.copy(alpha = 0.2f) else ImmersiveComponent,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedKeyId = key.id }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(Icons.Default.Key, null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                                                Column {
                                                    Text(key.title, color = ImmersiveTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text(key.fingerprint, color = ImmersiveTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Group Tag
                item {
                    Text("Category Tag", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cloud", "HomeLab", "Production", "Local", "IoT").forEach { tag ->
                            val isSelected = groupTag == tag
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { groupTag = tag }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Shell Profile
                item {
                    Text("Shell Profile", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("default" to "Default", "bash" to "Bash", "zsh" to "Zsh", "fish" to "Fish", "python" to "Python").forEach { (profileKey, profileName) ->
                            val isSelected = shellProfileId == profileKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { shellProfileId = profileKey }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = profileName,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Initial Directory
                item {
                    Text("Initial Working Directory (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = initialDirectory,
                        onValueChange = { initialDirectory = it },
                        placeholder = { Text("e.g. /var/www or /home/user", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Custom Environment Variables
                item {
                    Text("Environment Variables (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = envVars,
                        onValueChange = { envVars = it },
                        placeholder = { Text("e.g. TERM=xterm-256color, EDITOR=vim", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Startup Command
                item {
                    Text("Startup Command (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = startupCommand,
                        onValueChange = { startupCommand = it },
                        placeholder = { Text("e.g. htop or tmux attach", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (initialHost ?: HostEntity(
                        label = label.ifBlank { if (protocol == "LOCAL_SHELL") "Local Shell" else host },
                        host = if (protocol == "LOCAL_SHELL") "localhost" else host.ifBlank { "127.0.0.1" }
                    )).copy(
                        label = label.ifBlank { if (protocol == "LOCAL_SHELL") "Local Shell" else host },
                        host = if (protocol == "LOCAL_SHELL") "localhost" else host.ifBlank { "127.0.0.1" },
                        port = portText.toIntOrNull() ?: 22,
                        username = username.ifBlank { "root" },
                        protocol = protocol,
                        authType = authType,
                        password = if (authType == "PASSWORD") password else null,
                        keyId = if (authType == "PRIVATE_KEY") selectedKeyId else null,
                        groupTag = groupTag,
                        themeId = themeId,
                        shellProfileId = shellProfileId,
                        initialDirectory = initialDirectory.takeIf { it.isNotBlank() },
                        envVarsJson = envVars.takeIf { it.isNotBlank() },
                        startupCommand = startupCommand.takeIf { it.isNotBlank() }
                    )
                    onSave(entity)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                )
            ) {
                Text("Save Host", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersiveTextSecondary)
            }
        }
    )
}
