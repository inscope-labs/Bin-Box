package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.domain.model.Workspace
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel
import kotlinx.coroutines.launch

/**
 * Session Tabs Bar converting between Terminal tabs and Host tabs based on the 2-phase button selection.
 *
 * Requirements:
 * 1. Mode Conversion:
 *    - Terminal mode: 1 tab per open terminal session (e.g. 2 open -> 2 tabs).
 *    - Hosts mode: 1 tab per registered host (e.g. 5 hosts -> 5 tabs).
 * 2. Far-Left: Active terminals / registered host quick switcher with count badge and switcher trigger.
 * 3. Middle: Horizontally scrolling tabs with left/right "more" indicators (chevrons).
 * 4. Far-Right: Dedicated open / new terminal button.
 */
@Composable
fun TerminalSessionTabsBar(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier,
    onOpenOciWizard: () -> Unit = {},
    onOpenPackagesSheet: () -> Unit = {}
) {
    val currentTab by viewModel.currentAppTab.collectAsStateWithLifecycle()
    val isTerminalMode = currentTab != AppTab.HOSTS
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activeSessionIndex.collectAsStateWithLifecycle()
    val hosts by viewModel.hosts.collectAsStateWithLifecycle()
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()

    TerminalSessionTabsBar(
        sessions = sessions,
        activeIdx = activeIdx,
        activeWorkspace = activeWorkspace,
        workspaces = workspaces,
        onSelectSession = { idx ->
            BinBoxLogger.i("TerminalSessionTabsBar", "Selected terminal session at index $idx")
            viewModel.selectSession(idx)
        },
        onCloseSession = { idx ->
            BinBoxLogger.i("TerminalSessionTabsBar", "Closing terminal session at index $idx")
            viewModel.closeSession(idx)
        },
        onOpenRenameDialog = { idx -> viewModel.openRenameDialog(idx) },
        onDuplicateSession = { idx -> viewModel.duplicateSession(idx) },
        onMoveSession = { from, to -> viewModel.moveSession(from, to) },
        onSwitchWorkspace = { ws -> viewModel.switchWorkspace(ws) },
        onOpenWorkspaceDialog = { viewModel.openWorkspaceDialog() },
        onOpenSessionSwitcher = {
            BinBoxLogger.d("TerminalSessionTabsBar", "Opening active terminals session switcher")
            viewModel.setSessionSwitcherOpen(true)
        },
        onOpenDemoSession = {
            BinBoxLogger.i("TerminalSessionTabsBar", "Launching demo SSH session")
            viewModel.openDemoSession()
        },
        onOpenLocalSession = {
            BinBoxLogger.i("TerminalSessionTabsBar", "Launching local shell session")
            viewModel.openLocalSession()
        },
        onOpenOciWizard = onOpenOciWizard,
        onOpenPackagesSheet = onOpenPackagesSheet,
        onNavigateToHosts = {
            BinBoxLogger.d("TerminalSessionTabsBar", "Navigating to Hosts tab")
            viewModel.setAppTab(AppTab.HOSTS)
        },
        modifier = modifier,
        isTerminalMode = isTerminalMode,
        hosts = hosts,
        onSelectHost = { host ->
            BinBoxLogger.i("TerminalSessionTabsBar", "Host tab clicked: ${host.label} (${host.host})")
            val existingIdx = sessions.indexOfFirst {
                it.hostLabel.equals(host.label, ignoreCase = true) || it.hostLabel.contains(host.host)
            }
            if (existingIdx >= 0) {
                viewModel.selectSession(existingIdx)
                viewModel.setAppTab(AppTab.TERMINAL)
            } else {
                viewModel.connectToHost(host)
            }
        },
        onConnectHost = { host ->
            BinBoxLogger.i("TerminalSessionTabsBar", "Explicit connect requested for host: ${host.label}")
            viewModel.connectToHost(host)
        },
        onPingHost = { host ->
            BinBoxLogger.d("TerminalSessionTabsBar", "Ping host requested: ${host.label}")
            viewModel.pingHost(host)
        },
        onEditHost = { host ->
            viewModel.setAppTab(AppTab.HOSTS)
        },
        onDeleteHost = { host ->
            BinBoxLogger.i("TerminalSessionTabsBar", "Delete host requested from tab: ${host.label}")
            viewModel.deleteHost(host)
        },
        onOpenHostSwitcher = {
            viewModel.setAppTab(AppTab.HOSTS)
        },
        onOpenAddHostDialog = {
            viewModel.setAppTab(AppTab.HOSTS)
        }
    )
}

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
    modifier: Modifier = Modifier,
    isTerminalMode: Boolean = true,
    hosts: List<HostEntity> = emptyList(),
    selectedHostId: Long? = null,
    onSelectHost: (HostEntity) -> Unit = {},
    onConnectHost: (HostEntity) -> Unit = {},
    onPingHost: (HostEntity) -> Unit = {},
    onEditHost: (HostEntity) -> Unit = {},
    onDeleteHost: (HostEntity) -> Unit = {},
    onOpenHostSwitcher: () -> Unit = onNavigateToHosts,
    onOpenAddHostDialog: () -> Unit = onNavigateToHosts
) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    var showQuickSwitcherMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }

    // Scroll state detection for "more" indicators
    val canScrollLeft by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val canScrollRight by remember {
        derivedStateOf {
            lazyListState.canScrollForward
        }
    }

    // Auto-scroll to selected tab when active index changes in terminal mode
    LaunchedEffect(activeIdx, isTerminalMode) {
        if (isTerminalMode && activeIdx in sessions.indices) {
            lazyListState.animateScrollToItem(activeIdx)
        }
    }

    Surface(
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. VERY LEFT: Active Terminals / Registered Host Quick Switcher
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTerminalMode) ImmersivePrimary.copy(alpha = 0.4f) else CyanAccent.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clickable { showQuickSwitcherMenu = true }
                        .testTag(if (isTerminalMode) "active_terminals_quick_switcher" else "registered_hosts_quick_switcher")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isTerminalMode) Icons.Default.Terminal else Icons.Default.Dns,
                            contentDescription = if (isTerminalMode) "Active terminals quick switcher" else "Registered hosts quick switcher",
                            tint = if (isTerminalMode) ImmersivePrimary else CyanAccent,
                            modifier = Modifier.size(15.dp)
                        )

                        // Count badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isTerminalMode) ImmersivePrimary.copy(alpha = 0.22f)
                                    else CyanAccent.copy(alpha = 0.22f)
                                )
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isTerminalMode) "${sessions.size}" else "${hosts.size}",
                                color = if (isTerminalMode) ImmersivePrimary else CyanAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch menu",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showQuickSwitcherMenu,
                    onDismissRequest = { showQuickSwitcherMenu = false },
                    modifier = Modifier
                        .background(ImmersiveSurface)
                        .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "QUICK SWITCHER",
                        color = ImmersiveTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Terminal, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Active Terminals (${sessions.size})",
                                    color = if (isTerminalMode) ImmersivePrimary else ImmersiveTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isTerminalMode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        trailingIcon = {
                            if (isTerminalMode) {
                                Icon(Icons.Default.Check, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            showQuickSwitcherMenu = false
                            BinBoxLogger.d("TerminalSessionTabsBar", "Quick switcher: opened active terminals switcher")
                            onOpenSessionSwitcher()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Dns, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Registered Hosts (${hosts.size})",
                                    color = if (!isTerminalMode) CyanAccent else ImmersiveTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (!isTerminalMode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        trailingIcon = {
                            if (!isTerminalMode) {
                                Icon(Icons.Default.Check, null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            showQuickSwitcherMenu = false
                            BinBoxLogger.d("TerminalSessionTabsBar", "Quick switcher: selected registered hosts view")
                            onNavigateToHosts()
                        }
                    )

                    HorizontalDivider(color = ImmersiveBorderVerySubtle)

                    Text(
                        text = "WORKSPACE: ${activeWorkspace.name.uppercase()}",
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
                                    val wsColor = try {
                                        Color(android.graphics.Color.parseColor(ws.colorHex))
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
                                        text = ws.name,
                                        color = if (ws.id == activeWorkspace.id) ImmersivePrimary else ImmersiveTextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            },
                            trailingIcon = {
                                if (ws.id == activeWorkspace.id) {
                                    Icon(Icons.Default.Check, null, tint = ImmersivePrimary, modifier = Modifier.size(14.dp))
                                }
                            },
                            onClick = {
                                showQuickSwitcherMenu = false
                                onSwitchWorkspace(ws)
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("+ Manage Workspaces", color = ImmersivePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.Add, null, tint = ImmersivePrimary, modifier = Modifier.size(15.dp)) },
                        onClick = {
                            showQuickSwitcherMenu = false
                            onOpenWorkspaceDialog()
                        }
                    )
                }
            }

            // 2. MIDDLE: Horizontally Scrollable Tabs with Left / Right Indicators
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left "More" Indicator
                AnimatedVisibility(
                    visible = canScrollLeft,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.animateScrollBy(-220f)
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ImmersiveSurfaceElevated)
                            .border(1.dp, ImmersiveBorderSubtle, CircleShape)
                            .testTag("tabs_scroll_left_indicator")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Scroll tabs left",
                            tint = if (isTerminalMode) ImmersivePrimary else CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                LazyRow(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTerminalMode) {
                        // TERMINAL TABS (e.g. 2 open terminals -> 2 tabs)
                        if (sessions.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ImmersiveSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "No open terminals",
                                        color = ImmersiveTextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(sessions, key = { _, s -> s.id }) { index, session ->
                                val isSelected = index == activeIdx
                                val tabState by session.state.collectAsStateWithLifecycle()
                                var showTabOptions by remember { mutableStateOf(false) }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) ImmersiveComponent else ImmersiveSurfaceElevated,
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.7f))
                                        } else {
                                            androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                                        },
                                        modifier = Modifier
                                            .clickable { onSelectSession(index) }
                                            .testTag("session_tab_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                                                text = session.title.ifBlank { "Terminal ${index + 1}" },
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
                    } else {
                        // HOST TABS (e.g. 5 registered hosts -> 5 tabs)
                        if (hosts.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ImmersiveSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "No registered hosts",
                                        color = ImmersiveTextMuted,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(hosts, key = { _, h -> h.id }) { _, host ->
                                val isConnectedHost = sessions.any {
                                    it.hostLabel.equals(host.label, ignoreCase = true) || it.hostLabel.contains(host.host)
                                }
                                val isSelected = host.id == selectedHostId || isConnectedHost
                                var showHostOptions by remember { mutableStateOf(false) }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) ImmersiveComponent else ImmersiveSurfaceElevated,
                                        border = if (isSelected) {
                                            androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f))
                                        } else {
                                            androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                                        },
                                        modifier = Modifier
                                            .clickable { onSelectHost(host) }
                                            .testTag("host_tab_${host.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            if (isConnectedHost) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(ImmersiveStatusGreen)
                                                )
                                            } else if (host.isFavorite) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Favorite host",
                                                    tint = ImmersiveStatusAmber,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Dns,
                                                    contentDescription = null,
                                                    tint = CyanAccent,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }

                                            Text(
                                                text = host.label.ifBlank { host.host },
                                                color = if (isSelected) CyanAccent else ImmersiveTextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            // Options Menu Trigger
                                            IconButton(
                                                onClick = { showHostOptions = true },
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Host options",
                                                    tint = ImmersiveTextMuted,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showHostOptions,
                                        onDismissRequest = { showHostOptions = false },
                                        modifier = Modifier
                                            .background(ImmersiveSurface)
                                            .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Connect (Terminal)", color = CyanAccent, fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.Terminal, null, tint = CyanAccent) },
                                            onClick = {
                                                showHostOptions = false
                                                onConnectHost(host)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Ping Host", color = ImmersiveTextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.Sensors, null, tint = ImmersivePrimary) },
                                            onClick = {
                                                showHostOptions = false
                                                onPingHost(host)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Manage in Hosts", color = ImmersiveTextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = ImmersiveTextSecondary) },
                                            onClick = {
                                                showHostOptions = false
                                                onEditHost(host)
                                            }
                                        )
                                        HorizontalDivider(color = ImmersiveBorderVerySubtle)
                                        DropdownMenuItem(
                                            text = { Text("Delete Host", color = ImmersiveStatusRed) },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ImmersiveStatusRed) },
                                            onClick = {
                                                showHostOptions = false
                                                onDeleteHost(host)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right "More" Indicator
                AnimatedVisibility(
                    visible = canScrollRight,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.animateScrollBy(220f)
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ImmersiveSurfaceElevated)
                            .border(1.dp, ImmersiveBorderSubtle, CircleShape)
                            .testTag("tabs_scroll_right_indicator")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Scroll tabs right",
                            tint = if (isTerminalMode) ImmersivePrimary else CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 3. VERY RIGHT: Open / New Terminal Button
            Box {
                IconButton(
                    onClick = { showAddMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTerminalMode) ImmersivePrimary else CyanAccent)
                        .testTag("add_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isTerminalMode) "Open / New Terminal" else "Add Host or Terminal",
                        tint = if (isTerminalMode) ImmersiveOnPrimary else Slate950,
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
                    Text(
                        text = if (isTerminalMode) "LAUNCH TERMINAL" else "HOSTS & TERMINALS",
                        color = ImmersiveTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    DropdownMenuItem(
                        text = { Text("Cloud Sandbox (Demo SSH)", color = ImmersiveTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Cloud, null, tint = ImmersivePrimary) },
                        onClick = {
                            showAddMenu = false
                            BinBoxLogger.i("TerminalSessionTabsBar", "User launched Cloud Sandbox terminal")
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
                            BinBoxLogger.i("TerminalSessionTabsBar", "User launched Oracle Cloud VM wizard")
                            onOpenOciWizard()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Local Device Shell", color = ImmersiveTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Terminal, null, tint = ImmersiveStatusGreen) },
                        onClick = {
                            showAddMenu = false
                            BinBoxLogger.i("TerminalSessionTabsBar", "User launched Local Device Shell")
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
                        text = {
                            Text(
                                text = if (isTerminalMode) "Saved Hosts..." else "+ Add Saved Host",
                                color = ImmersiveTextPrimary
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Dns, null, tint = ImmersiveStatusAmber) },
                        onClick = {
                            showAddMenu = false
                            if (isTerminalMode) {
                                onNavigateToHosts()
                            } else {
                                onOpenAddHostDialog()
                            }
                        }
                    )
                }
            }
        }
    }
}
