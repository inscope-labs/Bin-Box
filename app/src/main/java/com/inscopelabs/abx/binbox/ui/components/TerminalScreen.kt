package com.inscopelabs.abx.binbox.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.oci.wizard.LocalOciWizardLauncher
import com.inscopelabs.abx.binbox.oci.wizard.OciFreeTierPromoCard
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.model.*
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activeSessionIndex.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val fontSizeSp by viewModel.fontSizeSp.collectAsStateWithLifecycle()
    val cursorStyle by viewModel.cursorStyle.collectAsStateWithLifecycle()
    val ctrlLatched by viewModel.ctrlLatched.collectAsStateWithLifecycle()
    val altLatched by viewModel.altLatched.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val connectionProfiles by viewModel.connectionProfiles.collectAsStateWithLifecycle()
    val renameDialogSessionIndex by viewModel.renameDialogSessionIndex.collectAsStateWithLifecycle()
    val isSessionSwitcherOpen by viewModel.isSessionSwitcherOpen.collectAsStateWithLifecycle()
    val isWorkspaceDialogOpen by viewModel.isWorkspaceDialogOpen.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }
    var isCaseSensitive by remember { mutableStateOf(false) }
    var isRegexMode by remember { mutableStateOf(false) }
    var showWorkspaceDropdown by remember { mutableStateOf(false) }
    val ociLauncher = LocalOciWizardLauncher.current

    // Session lines observation
    val sessionLines by (activeSession?.lines?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) })
    val sessionState by (activeSession?.state?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(SessionState.Disconnected) })

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new output
    LaunchedEffect(sessionLines.size) {
        if (sessionLines.isNotEmpty()) {
            listState.animateScrollToItem(sessionLines.size - 1)
        }
    }

    // Cursor Blink Animation
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(530)
            cursorVisible = !cursorVisible
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBg)
    ) {
        // ----------------------------------------------------
        // 1. Session Tabs Strip & Quick Actions
        // ----------------------------------------------------
        Surface(
            color = ImmersiveSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
            modifier = Modifier.fillMaxWidth()
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
                                    viewModel.switchWorkspace(ws)
                                }
                            )
                        }
                        HorizontalDivider(color = ImmersiveBorderVerySubtle)
                        DropdownMenuItem(
                            text = { Text("+ New Workspace", color = ImmersivePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showWorkspaceDropdown = false
                                viewModel.openWorkspaceDialog()
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
                                    .clickable { viewModel.selectSession(index) }
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
                                        onClick = { viewModel.closeSession(index) },
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
                                        viewModel.openRenameDialog(index)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicate Tab", color = ImmersiveTextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = ImmersiveTextSecondary) },
                                    onClick = {
                                        showTabOptions = false
                                        viewModel.duplicateSession(index)
                                    }
                                )
                                if (index > 0) {
                                    DropdownMenuItem(
                                        text = { Text("Move Left", color = ImmersiveTextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.ArrowBack, null, tint = ImmersiveTextSecondary) },
                                        onClick = {
                                            showTabOptions = false
                                            viewModel.moveSession(index, index - 1)
                                        }
                                    )
                                }
                                if (index < sessions.size - 1) {
                                    DropdownMenuItem(
                                        text = { Text("Move Right", color = ImmersiveTextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.ArrowForward, null, tint = ImmersiveTextSecondary) },
                                        onClick = {
                                            showTabOptions = false
                                            viewModel.moveSession(index, index + 1)
                                        }
                                    )
                                }
                                HorizontalDivider(color = ImmersiveBorderVerySubtle)
                                DropdownMenuItem(
                                    text = { Text("Close Tab", color = ImmersiveStatusRed) },
                                    leadingIcon = { Icon(Icons.Default.Close, null, tint = ImmersiveStatusRed) },
                                    onClick = {
                                        showTabOptions = false
                                        viewModel.closeSession(index)
                                    }
                                )
                            }
                        }
                    }
                }

                // Grid / Session Switcher Button
                IconButton(
                    onClick = { viewModel.setSessionSwitcherOpen(true) },
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
                var showAddMenu by remember { mutableStateOf(false) }
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
                                viewModel.openDemoSession()
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
                                ociLauncher()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Local Device Shell", color = ImmersiveTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Terminal, null, tint = ImmersiveStatusGreen) },
                            onClick = {
                                showAddMenu = false
                                viewModel.openLocalSession()
                            }
                        )
                        HorizontalDivider(color = ImmersiveBorderVerySubtle)
                        DropdownMenuItem(
                            text = { Text("Saved Hosts...", color = ImmersiveTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Dns, null, tint = ImmersiveStatusAmber) },
                            onClick = {
                                showAddMenu = false
                                viewModel.setAppTab(com.inscopelabs.abx.binbox.ui.viewmodel.AppTab.HOSTS)
                            }
                        )
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 2. Terminal Utility Action Bar (Search, Font, Clear, Telemetry)
        // ----------------------------------------------------
        Surface(
            color = ImmersiveBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Host State Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusText = when (sessionState) {
                        is SessionState.Connected -> "ONLINE"
                        is SessionState.Connecting -> "CONNECTING"
                        is SessionState.Error -> "ERROR"
                        is SessionState.Disconnected -> "OFFLINE"
                    }
                    val statusColor = when (sessionState) {
                        is SessionState.Connected -> ImmersiveStatusGreen
                        is SessionState.Connecting -> ImmersiveStatusAmber
                        is SessionState.Error -> ImmersiveStatusRed
                        is SessionState.Disconnected -> ImmersiveTextMuted
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(0.5.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }

                    activeSession?.let {
                        Text(
                            text = it.hostLabel,
                            color = ImmersiveTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Search toggle
                    IconButton(
                        onClick = { viewModel.toggleSearching(!isSearching) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSearching) ImmersiveComponent else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search buffer",
                            tint = if (isSearching) ImmersivePrimary else ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Telemetry Quick Probe
                    IconButton(
                        onClick = {
                            activeSession?.let { viewModel.probeHostTelemetry(it.hostLabel) }
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = "Server telemetry",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Font Zoom Out
                    IconButton(
                        onClick = { viewModel.setFontSize(fontSizeSp - 1) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Text("A-", color = ImmersiveTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Font Zoom In
                    IconButton(
                        onClick = { viewModel.setFontSize(fontSizeSp + 1) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Text("A+", color = ImmersiveTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy Log
                    IconButton(
                        onClick = {
                            val log = activeSession?.rawLogText ?: ""
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Log", log))
                            viewModel.showSnackbar("Copied terminal output to clipboard")
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy output",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Share Log
                    IconButton(
                        onClick = {
                            val log = activeSession?.rawLogText ?: ""
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, log)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Terminal Log"))
                        },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share output",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Clear Terminal
                    IconButton(
                        onClick = { viewModel.clearCurrentTerminal() },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear screen",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Search Bar (if active)
        AnimatedVisibility(visible = isSearching) {
            val matchingLineIndices = remember(sessionLines, searchQuery, isCaseSensitive) {
                if (searchQuery.isBlank()) emptyList<Int>()
                else sessionLines.mapIndexedNotNull { index, line ->
                    if (line.rawText.contains(searchQuery, ignoreCase = !isCaseSensitive)) index else null
                }
            }
            var currentMatchIdx by remember { mutableIntStateOf(0) }

            Surface(
                color = ImmersiveSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Search, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            viewModel.setSearchQuery(it)
                            currentMatchIdx = 0
                        },
                        placeholder = { Text("Search terminal scrollback...", color = ImmersiveTextMuted, fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, "Clear", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    )

                    // Case Sensitivity Toggle (Aa)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCaseSensitive) ImmersivePrimary else ImmersiveComponent,
                        modifier = Modifier
                            .clickable { isCaseSensitive = !isCaseSensitive }
                            .size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "Aa",
                                color = if (isCaseSensitive) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Match Count Badge
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = if (matchingLineIndices.isEmpty()) "0" else "${currentMatchIdx + 1}/${matchingLineIndices.size}",
                            color = if (matchingLineIndices.isEmpty()) ImmersiveStatusRed else ImmersiveStatusGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        // Prev Match Button
                        IconButton(
                            onClick = {
                                if (matchingLineIndices.isNotEmpty()) {
                                    currentMatchIdx = if (currentMatchIdx > 0) currentMatchIdx - 1 else matchingLineIndices.size - 1
                                    scope.launch {
                                        listState.animateScrollToItem(matchingLineIndices[currentMatchIdx])
                                    }
                                }
                            },
                            enabled = matchingLineIndices.isNotEmpty(),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "Previous match", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                        }

                        // Next Match Button
                        IconButton(
                            onClick = {
                                if (matchingLineIndices.isNotEmpty()) {
                                    currentMatchIdx = if (currentMatchIdx < matchingLineIndices.size - 1) currentMatchIdx + 1 else 0
                                    scope.launch {
                                        listState.animateScrollToItem(matchingLineIndices[currentMatchIdx])
                                    }
                                }
                            },
                            enabled = matchingLineIndices.isNotEmpty(),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Next match", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Close Search Button
                    IconButton(
                        onClick = { viewModel.toggleSearching(false) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close search", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 3. Immersive Floating Terminal Window (rounded-3xl card)
        // ----------------------------------------------------
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ImmersiveTerminalCardBg)
                .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(24.dp))
                .padding(14.dp)
        ) {
            if (activeSession == null || sessionLines.isEmpty()) {
                // Empty Session State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ImmersiveComponent)
                            .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bin Box Terminal",
                        color = ImmersiveTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Connect to remote host shells via SSH, Telnet, or launch the local Android shell.",
                        color = ImmersiveTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.openDemoSession() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImmersivePrimary,
                                contentColor = ImmersiveOnPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Launch Demo Host", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.openLocalSession() },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextPrimary)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Local Shell")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    OciFreeTierPromoCard(
                        onLaunchWizard = { ociLauncher() },
                        modifier = Modifier.fillMaxWidth(0.95f)
                    )
                }
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("terminal_lines_list"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(sessionLines) { line ->
                            val annotatedString = renderLineAnnotatedString(line, currentTheme, searchQuery)
                            Text(
                                text = annotatedString,
                                fontSize = fontSizeSp.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (fontSizeSp + 5).sp
                            )
                        }

                        // Active input / cursor line
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                if (cursorVisible) {
                                    when (cursorStyle) {
                                        CursorStyle.BLOCK, CursorStyle.BLINKING_BLOCK -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = (fontSizeSp * 0.6).dp, height = fontSizeSp.dp)
                                                    .background(ImmersivePrimary)
                                            )
                                        }
                                        CursorStyle.UNDERLINE -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = (fontSizeSp * 0.6).dp, height = 2.5.dp)
                                                    .background(ImmersivePrimary)
                                            )
                                        }
                                        CursorStyle.BAR -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 2.5.dp, height = fontSizeSp.dp)
                                                    .background(ImmersivePrimary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ----------------------------------------------------
        // 4. Keyboard Accessory Row (Ctrl, Alt, Esc, Tab, Arrows, Combos)
        // ----------------------------------------------------
        Surface(
            color = ImmersiveBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Special Combos Row (Ctrl+C, Ctrl+D, Ctrl+Z, Ctrl+L, Ctrl+A, Ctrl+E)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Latched Modifier Keys
                    AccessoryKeyButton(
                        label = "ESC",
                        onClick = { viewModel.sendSpecialKey(TerminalKey.ESC) }
                    )
                    AccessoryKeyButton(
                        label = "TAB",
                        onClick = { viewModel.sendSpecialKey(TerminalKey.TAB) }
                    )
                    AccessoryKeyButton(
                        label = "CTRL",
                        isLatched = ctrlLatched,
                        onClick = { viewModel.toggleCtrl() },
                        accentColor = ImmersivePrimary
                    )
                    AccessoryKeyButton(
                        label = "ALT",
                        isLatched = altLatched,
                        onClick = { viewModel.toggleAlt() },
                        accentColor = ImmersiveStatusAmber
                    )

                    // Special LOGS Action Pill with Ice Blue Luminous Accent
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ImmersivePrimary,
                        modifier = Modifier
                            .clickable { viewModel.sendCommand("journalctl -n 30 --no-pager || dmesg | tail -n 30") }
                            .height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = ImmersiveOnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "LOGS",
                                color = ImmersiveOnPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Quick Interrupt / Terminal Combos
                    ComboKeyPill(label = "^C", sub = "SIGINT", onClick = { viewModel.sendSpecialKey(TerminalKey.CTRL_C) }, tint = ImmersiveStatusRed)
                    ComboKeyPill(label = "^D", sub = "EOF", onClick = { viewModel.sendSpecialKey(TerminalKey.CTRL_D) })
                    ComboKeyPill(label = "^Z", sub = "STOP", onClick = { viewModel.sendSpecialKey(TerminalKey.CTRL_Z) }, tint = ImmersiveStatusAmber)
                    ComboKeyPill(label = "^L", sub = "CLEAR", onClick = { viewModel.sendSpecialKey(TerminalKey.CTRL_L) })
                    ComboKeyPill(label = "^A", sub = "HOME", onClick = { viewModel.sendSpecialKey(TerminalKey.CTRL_A) })
                    ComboKeyPill(label = "^E", sub = "END", onClick = { viewModel.sendSpecialKey(TerminalKey.CTRL_E) })

                    // Directional Arrows
                    AccessoryKeyButton(label = "▲", onClick = { viewModel.sendSpecialKey(TerminalKey.ARROW_UP) })
                    AccessoryKeyButton(label = "▼", onClick = { viewModel.sendSpecialKey(TerminalKey.ARROW_DOWN) })
                    AccessoryKeyButton(label = "◀", onClick = { viewModel.sendSpecialKey(TerminalKey.ARROW_LEFT) })
                    AccessoryKeyButton(label = "▶", onClick = { viewModel.sendSpecialKey(TerminalKey.ARROW_RIGHT) })

                    // Symbols & Navigation Keys
                    AccessorySymbolKey(symbol = "|", onClick = { viewModel.sendRawInput("|") })
                    AccessorySymbolKey(symbol = "/", onClick = { viewModel.sendRawInput("/") })
                    AccessorySymbolKey(symbol = "\\", onClick = { viewModel.sendRawInput("\\") })
                    AccessorySymbolKey(symbol = "~", onClick = { viewModel.sendRawInput("~") })
                    AccessorySymbolKey(symbol = "-", onClick = { viewModel.sendRawInput("-") })
                    AccessorySymbolKey(symbol = "_", onClick = { viewModel.sendRawInput("_") })
                    AccessorySymbolKey(symbol = ":", onClick = { viewModel.sendRawInput(":") })
                    AccessorySymbolKey(symbol = "$", onClick = { viewModel.sendRawInput("$") })
                    AccessorySymbolKey(symbol = ">", onClick = { viewModel.sendRawInput(">") })
                }

                // History Quick Select Row
                if (history.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(history.take(6)) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ImmersiveComponent,
                                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier.clickable {
                                    inputText = item.command
                                }
                            ) {
                                Text(
                                    text = item.command,
                                    color = ImmersiveTextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // ----------------------------------------------------
                // 5. Immersive Command Input Capsule (footer layout)
                // ----------------------------------------------------
                Surface(
                    color = ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Capsule Input Container (rounded-2xl / 16.dp)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(ImmersiveComponent)
                                .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$",
                                color = ImmersivePrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(inputFocusRequester)
                                    .testTag("terminal_input_field"),
                                placeholder = {
                                    Text("Type command...", color = ImmersiveTextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = ImmersiveTextPrimary,
                                    unfocusedTextColor = ImmersiveTextPrimary,
                                    cursorColor = ImmersivePrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ImmersiveTextPrimary
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank()) {
                                            viewModel.sendCommand(inputText)
                                            inputText = ""
                                        }
                                    }
                                )
                            )

                            // Quick Arrow History Browsers
                            IconButton(
                                onClick = { viewModel.sendSpecialKey(TerminalKey.ARROW_UP) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous command",
                                    tint = ImmersiveTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.sendSpecialKey(TerminalKey.ARROW_DOWN) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next command",
                                    tint = ImmersiveTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Send Button
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendCommand(inputText)
                                        inputText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (inputText.isNotBlank()) ImmersivePrimary else Color.Transparent)
                                    .testTag("send_command_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send command",
                                    tint = if (inputText.isNotBlank()) ImmersiveOnPrimary else ImmersivePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rename Session Tab Dialog
        renameDialogSessionIndex?.let { idx ->
            val sessionToRename = sessions.getOrNull(idx)
            if (sessionToRename != null) {
                RenameSessionDialog(
                    initialTitle = sessionToRename.title,
                    onDismiss = { viewModel.closeRenameDialog() },
                    onConfirm = { newTitle ->
                        viewModel.renameSession(idx, newTitle)
                        viewModel.closeRenameDialog()
                    }
                )
            }
        }

        // Session Switcher Bottom Sheet
        if (isSessionSwitcherOpen) {
            SessionSwitcherSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.setSessionSwitcherOpen(false) }
            )
        }

        // Workspace Management Dialog
        if (isWorkspaceDialogOpen) {
            WorkspaceModal(
                savedHosts = connectionProfiles,
                onDismiss = { viewModel.closeWorkspaceDialog() },
                onSave = { name, desc, icon, color, hostIds ->
                    viewModel.createWorkspace(name, desc, icon, color, hostIds)
                    viewModel.closeWorkspaceDialog()
                }
            )
        }
    }
}

@Composable
fun AccessoryKeyButton(
    label: String,
    onClick: () -> Unit,
    isLatched: Boolean = false,
    accentColor: Color = ImmersivePrimary
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLatched) accentColor.copy(alpha = 0.25f) else ImmersiveComponent,
        border = if (isLatched) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                color = if (isLatched) accentColor else ImmersiveTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ComboKeyPill(
    label: String,
    sub: String,
    onClick: () -> Unit,
    tint: Color = ImmersivePrimary
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ImmersiveComponent,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sub,
                color = ImmersiveTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AccessorySymbolKey(
    symbol: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ImmersiveComponent,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                color = ImmersiveTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

private fun renderLineAnnotatedString(
    line: TerminalLine,
    theme: TerminalThemePreset,
    searchQuery: String
): AnnotatedString {
    return buildAnnotatedString {
        line.segments.forEach { segment ->
            val spanStyle = segment.style.toSpanStyle(theme)
            pushStyle(spanStyle)
            append(segment.text)
            pop()
        }

        // Highlight search results if present
        if (searchQuery.isNotBlank()) {
            val fullText = line.rawText
            var searchIdx = fullText.indexOf(searchQuery, ignoreCase = true)
            while (searchIdx >= 0) {
                addStyle(
                    style = SpanStyle(
                        background = ImmersiveStatusAmber.copy(alpha = 0.5f),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    start = searchIdx,
                    end = searchIdx + searchQuery.length
                )
                searchIdx = fullText.indexOf(searchQuery, searchIdx + searchQuery.length, ignoreCase = true)
            }
        }
    }
}
