package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.ServerTelemetry

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.domain.model.Workspace
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

@Composable
fun RenameSessionDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(20.dp))
                Text("Rename Session Tab", color = ImmersiveTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter a descriptive title for this terminal tab:", color = ImmersiveTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Production Web Bastion", color = ImmersiveTextMuted) },
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
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim())
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersiveTextSecondary)
            }
        }
    )
}

@Composable
fun WorkspaceModal(
    initialWorkspace: Workspace? = null,
    savedHosts: List<com.inscopelabs.abx.binbox.domain.model.ConnectionProfile>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, icon: String, colorHex: String, hostIds: List<Long>) -> Unit
) {
    var name by remember { mutableStateOf(initialWorkspace?.name ?: "") }
    var description by remember { mutableStateOf(initialWorkspace?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(initialWorkspace?.iconName ?: "Terminal") }
    var selectedColor by remember { mutableStateOf(initialWorkspace?.colorHex ?: "#38BDF8") }
    val selectedHostIds = remember { mutableStateListOf<Long>().apply { initialWorkspace?.let { addAll(it.hostProfileIds) } } }

    val iconOptions = listOf("Terminal", "Cloud", "Storage", "Dns", "Security", "Memory", "Folder")
    val colorOptions = listOf("#38BDF8", "#34D399", "#FBBF24", "#A78BFA", "#F43F5E", "#60A5FA")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FolderSpecial, null, tint = ImmersivePrimary, modifier = Modifier.size(22.dp))
                Text(
                    text = if (initialWorkspace == null) "New Workspace" else "Edit Workspace",
                    color = ImmersiveTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Workspace Name", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Kubernetes Cluster, Dev Lab", color = ImmersiveTextMuted) },
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

                item {
                    Text("Description (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Short note on purpose of this workspace", color = ImmersiveTextMuted) },
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

                item {
                    Text("Accent Color", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Icon Preset", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.forEach { iconKey ->
                            val isSelected = selectedIcon == iconKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { selectedIcon = iconKey }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = when (iconKey) {
                                        "Cloud" -> Icons.Default.Cloud
                                        "Storage" -> Icons.Default.Storage
                                        "Dns" -> Icons.Default.Dns
                                        "Security" -> Icons.Default.Security
                                        "Memory" -> Icons.Default.Memory
                                        "Folder" -> Icons.Default.Folder
                                        else -> Icons.Default.Terminal
                                    }
                                    Icon(
                                        icon,
                                        null,
                                        tint = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Assign Saved Hosts (${selectedHostIds.size} selected)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (savedHosts.isEmpty()) {
                        Text("No hosts saved yet. You can assign hosts later.", color = ImmersiveTextMuted, fontSize = 11.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedHosts.forEach { host ->
                                val isChecked = selectedHostIds.contains(host.id)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isChecked) ImmersiveComponent else ImmersiveSurfaceElevated,
                                    border = if (isChecked) androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.5f)) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) selectedHostIds.remove(host.id) else selectedHostIds.add(host.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(host.label, color = ImmersiveTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${host.username}@${host.host}:${host.port}", color = ImmersiveTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedHostIds.add(host.id) else selectedHostIds.remove(host.id)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = ImmersivePrimary,
                                                uncheckedColor = ImmersiveTextMuted,
                                                checkmarkColor = ImmersiveOnPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), description.trim(), selectedIcon, selectedColor, selectedHostIds.toList())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                )
            ) {
                Text("Save Workspace", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersiveTextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSwitcherSheet(
    viewModel: BinBoxViewModel,
    onDismiss: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activeSessionIndex.collectAsStateWithLifecycle()
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ImmersiveBorderSubtle) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Terminal Sessions",
                        color = ImmersiveTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sessions.size} sessions open • Workspace: ${activeWorkspace.name}",
                        color = ImmersiveTextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (sessions.isNotEmpty()) {
                    TextButton(onClick = { viewModel.closeAllSessions() }) {
                        Text("Close All", color = ImmersiveStatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Terminal, null, tint = ImmersiveTextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active terminal sessions", color = ImmersiveTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sessions) { index, session ->
                        val isSelected = index == activeIdx
                        val state by session.state.collectAsStateWithLifecycle()
                        val bytesIn by session.bytesReceived.collectAsStateWithLifecycle()
                        val bytesOut by session.bytesSent.collectAsStateWithLifecycle()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ImmersiveComponent else ImmersiveSurfaceElevated,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSession(index)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Status Indicator
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (state) {
                                                is SessionState.Connected -> ImmersiveStatusGreen
                                                is SessionState.Connecting -> ImmersiveStatusAmber
                                                is SessionState.Error -> ImmersiveStatusRed
                                                is SessionState.Disconnected -> ImmersiveTextMuted
                                            }
                                        )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = session.title,
                                            color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ImmersivePrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    color = ImmersivePrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${session.hostLabel} • In: ${bytesIn / 1024}KB, Out: ${bytesOut / 1024}KB",
                                        color = ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Quick Actions: Move Up, Move Down, Duplicate, Rename, Close
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { viewModel.moveSession(index, index - 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, "Move up", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (index < sessions.size - 1) {
                                        IconButton(
                                            onClick = { viewModel.moveSession(index, index + 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, "Move down", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.duplicateSession(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, "Duplicate tab", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            onDismiss()
                                            viewModel.openRenameDialog(index)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Rename tab", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.closeSession(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Close session", tint = ImmersiveStatusRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fast Session Creator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.openLocalSession()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersivePrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle)
                ) {
                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Local Shell", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.openDemoSession()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    )
                ) {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cloud Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TelemetryDialog(
    telemetry: com.inscopelabs.abx.binbox.ui.viewmodel.ServerTelemetry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyanAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Speed, null, tint = CyanGlow, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Server Telemetry", color = Slate100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(telemetry.hostLabel, color = Slate400, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OS & Kernel
                TelemetryMetricCard(
                    title = "Operating System & Kernel",
                    value = telemetry.osInfo,
                    icon = Icons.Default.Dns,
                    color = CyanGlow
                )

                // Uptime
                TelemetryMetricCard(
                    title = "System Uptime & Load Avg",
                    value = telemetry.uptime,
                    icon = Icons.Default.Schedule,
                    color = EmeraldGreen
                )

                // CPU Load
                TelemetryMetricCard(
                    title = "CPU Utilization",
                    value = telemetry.cpuUsage,
                    icon = Icons.Default.Memory,
                    color = AmberGlow
                )

                // Memory RAM
                TelemetryMetricCard(
                    title = "RAM Memory Allocation",
                    value = telemetry.memUsage,
                    icon = Icons.Default.Storage,
                    color = VioletAccent
                )

                // Disk Space
                TelemetryMetricCard(
                    title = "NVMe / SSD Root Partition",
                    value = telemetry.diskUsage,
                    icon = Icons.Default.PieChart,
                    color = BlueAccent
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Close", color = Slate950, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate850,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Text(title, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    value,
                    color = Slate100,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SnippetRunnerDialog(
    snippet: SnippetEntity,
    onDismiss: () -> Unit,
    onExecute: (resolvedCommand: String) -> Unit
) {
    // Extract parameters from template: {{param}} or {{param:default}}
    val pattern = Regex("\\{\\{([^}]+)\\}\\}")
    val matches = remember(snippet.commandTemplate) {
        pattern.findAll(snippet.commandTemplate).map { it.groupValues[1] }.distinct().toList()
    }

    val paramValues = remember(matches) {
        mutableStateMapOf<String, String>().apply {
            matches.forEach { raw ->
                if (raw.contains(":")) {
                    val split = raw.split(":", limit = 2)
                    this[raw] = split[1]
                } else {
                    this[raw] = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Column {
                Text("Run Script in Terminal", color = Slate100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(snippet.title, color = CyanGlow, fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (matches.isNotEmpty()) {
                    Text("Provide Parameter Values:", color = Slate400, fontSize = 12.sp)
                    matches.forEach { key ->
                        val label = if (key.contains(":")) key.split(":", limit = 2)[0] else key
                        Column {
                            Text(label, color = Slate300, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            TextField(
                                value = paramValues[key] ?: "",
                                onValueChange = { paramValues[key] = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Slate850,
                                    unfocusedContainerColor = Slate850,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                ),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }
                    }
                }

                // Command Preview
                val resolvedPreview = remember(paramValues.values.toList()) {
                    var resolved = snippet.commandTemplate
                    matches.forEach { key ->
                        val value = paramValues[key] ?: ""
                        resolved = resolved.replace("{{$key}}", value)
                    }
                    resolved
                }

                Text("Resolved Command:", color = Slate400, fontSize = 11.sp)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate950,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resolvedPreview,
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var resolved = snippet.commandTemplate
                    matches.forEach { key ->
                        val value = paramValues[key] ?: ""
                        resolved = resolved.replace("{{$key}}", value)
                    }
                    onExecute(resolved)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Slate950, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Execute", color = Slate950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
