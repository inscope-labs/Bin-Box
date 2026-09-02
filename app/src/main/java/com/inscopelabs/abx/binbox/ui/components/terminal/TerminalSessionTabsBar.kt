package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.domain.model.Workspace
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun TerminalSessionTabsBar(
    sessions: List<ShellSession>,
    activeIdx: Int,
    activeWorkspace: Workspace,
    workspaces: List<Workspace>,
    onSelectSession: (Int) -> Unit,
    onCloseSession: (Int) -> Unit,
    onOpenRenameDialog: (Int) -> Unit,
    onDuplicateSession: (Int) -> Unit,
    onMoveSession: (Int, Int) -> Unit,
    onSwitchWorkspace: (Workspace) -> Unit,
    onOpenWorkspaceDialog: () -> Unit,
    onOpenSessionSwitcher: () -> Unit,
    onOpenDemoSession: () -> Unit,
    onOpenLocalSession: () -> Unit,
    onOpenOciWizard: () -> Unit,
    onOpenPackagesSheet: () -> Unit,
    onNavigateToHosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWorkspaceDropdown by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    Surface(
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Workspace Selector Pill
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                    modifier = Modifier
                        .clickable { showWorkspaceDropdown = true }
                        .testTag("workspace_pill_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val wsColor = try {
                            Color(android.graphics.Color.parseColor(activeWorkspace.colorHex))
                        } catch (_: Exception) {
                            ImmersivePrimary
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(wsColor)
                        )
                        Text(
                            text = activeWorkspace.name,
                            color = ImmersiveTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Switch workspace",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showWorkspaceDropdown,
                    onDismissRequest = { showWorkspaceDropdown = false },
                    modifier = Modifier
                        .background(ImmersiveSurface)
                        .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Workspaces",
                        color = ImmersiveTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    workspaces.forEach { ws ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val color = try {
                                        Color(android.graphics.Color.parseColor(ws.colorHex))
                                    } catch (_: Exception) {
                                        ImmersivePrimary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(ws.name, color = if (ws.id == activeWorkspace.id) ImmersivePrimary else ImmersiveTextPrimary, fontSize = 13.sp)
                                }
                            },
                            trailingIcon = {
                                if (ws.id == activeWorkspace.id) {
                                    Icon(Icons.Default.Check, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                showWorkspaceDropdown = false
                                onSwitchWorkspace(ws)
                            }
                        )
                    }
                    HorizontalDivider(color = ImmersiveBorderVerySubtle)
                    DropdownMenuItem(
                        text = { Text("+ New Workspace", color = ImmersivePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Add, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp)) },
                        onClick = {
                            showWorkspaceDropdown = false
                            onOpenWorkspaceDialog()
                        }
                    )
                }
            }

            // Session Tabs List
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(sessions) { index, session ->
                    val isSelected = index == activeIdx
                    val tabState by session.state.collectAsStateWithLifecycle()
                    var showTabOptions by remember { mutableStateOf(false) }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) ImmersiveComponent else ImmersiveSurfaceElevated,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.6f)) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                            modifier = Modifier
                                .clickable { onSelectSession(index) }
                                .testTag("session_tab_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Status Dot
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (tabState) {
                                                is SessionState.Connected -> ImmersiveStatusGreen
                                                is SessionState.Connecting -> ImmersiveStatusAmber
                                                is SessionState.Error -> ImmersiveStatusRed
                                                is SessionState.Disconnected -> ImmersiveTextMuted
                                            }
                                        )
                                )

                                Text(
                                    text = session.title,
                                    color = if (isSelected) ImmersivePrimary else ImmersiveTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Tab Menu Dropdown Trigger
                                IconButton(
                                    onClick = { showTabOptions = true },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Tab options",
                                        tint = if (isSelected) ImmersiveTextPrimary else ImmersiveTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                // Close Tab Button
                                IconButton(
                                    onClick = { onCloseSession(index) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close session",
                                        tint = if (isSelected) ImmersiveTextPrimary else ImmersiveTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showTabOptions,
                            onDismissRequest = { showTabOptions = false },
                            modifier = Modifier
                                .background(ImmersiveSurface)
                                .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Tab", color = ImmersiveTextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = ImmersivePrimary) },
                                onClick = {
                                    showTabOptions = false
                                    onOpenRenameDialog(index)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate Tab", color = ImmersiveTextPrimary) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = ImmersiveTextSecondary) },
                                onClick = {
                                    showTabOptions = false
                                    onDuplicateSession(index)
                                }
                            )
                            if (index > 0) {
                                DropdownMenuItem(
                                    text = { Text("Move Left", color = ImmersiveTextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.ArrowBack, null, tint = ImmersiveTextSecondary) },
                                    onClick = {
                                        showTabOptions = false
                                        onMoveSession(index, index - 1)
                                    }
                                )
                            }
                            if (index < sessions.size - 1) {
                                DropdownMenuItem(
                                    text = { Text("Move Right", color = ImmersiveTextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.ArrowForward, null, tint = ImmersiveTextSecondary) },
                                    onClick = {
                                        showTabOptions = false
                                        onMoveSession(index, index + 1)
                                    }
                                )
                            }
                            HorizontalDivider(color = ImmersiveBorderVerySubtle)
                            DropdownMenuItem(
                                text = { Text("Close Tab", color = ImmersiveStatusRed) },
                                leadingIcon = { Icon(Icons.Default.Close, null, tint = ImmersiveStatusRed) },
                                onClick = {
                                    showTabOptions = false
                                    onCloseSession(index)
                                }
                            )
                        }
                    }
                }
            }

            // Grid / Session Switcher Button
            IconButton(
                onClick = onOpenSessionSwitcher,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ImmersiveComponent)
                    .border(1.dp, ImmersiveBorderVerySubtle, RoundedCornerShape(10.dp))
                    .testTag("session_grid_button")
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Session switcher grid",
                    tint = ImmersiveTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // New Session Add Menu
            Box {
                IconButton(
                    onClick = { showAddMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ImmersivePrimary)
                        .testTag("add_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add session",
                        tint = ImmersiveOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    modifier = Modifier
                        .background(ImmersiveSurface)
                        .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Cloud Sandbox (Demo SSH)", color = ImmersiveTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Cloud, null, tint = ImmersivePrimary) },
                        onClick = {
                            showAddMenu = false
                            onOpenDemoSession()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Oracle Cloud VM", color = ImmersiveTextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Free ARM", color = CyanAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.CloudQueue, null, tint = CyanGlow) },
                        onClick = {
                            showAddMenu = false
                            onOpenOciWizard()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Local Device Shell", color = ImmersiveTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Terminal, null, tint = ImmersiveStatusGreen) },
                        onClick = {
                            showAddMenu = false
                            onOpenLocalSession()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Manage Local Packages", color = ImmersiveTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Extension, null, tint = ImmersivePrimary) },
                        onClick = {
                            showAddMenu = false
                            onOpenPackagesSheet()
                        }
                    )
                    HorizontalDivider(color = ImmersiveBorderVerySubtle)
                    DropdownMenuItem(
                        text = { Text("Saved Hosts...", color = ImmersiveTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Dns, null, tint = ImmersiveStatusAmber) },
                        onClick = {
                            showAddMenu = false
                            onNavigateToHosts()
                        }
                    )
                }
            }
        }
    }
}
