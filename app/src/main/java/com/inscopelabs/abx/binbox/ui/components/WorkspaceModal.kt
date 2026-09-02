package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.domain.model.ConnectionProfile
import com.inscopelabs.abx.binbox.domain.model.Workspace
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun WorkspaceModal(
    initialWorkspace: Workspace? = null,
    savedHosts: List<ConnectionProfile>,
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
