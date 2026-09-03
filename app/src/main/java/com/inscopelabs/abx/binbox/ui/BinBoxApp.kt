package com.inscopelabs.abx.binbox.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.oci.management.OciManagementScreen
import com.inscopelabs.abx.binbox.oci.management.OciProvisioningStatus
import com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingScreen
import com.inscopelabs.abx.binbox.terminal.service.TerminalForegroundService
import com.inscopelabs.abx.binbox.ui.components.*
import com.inscopelabs.abx.binbox.ui.components.navigation.BinBoxBottomBar
import com.inscopelabs.abx.binbox.ui.components.navigation.MoreNavigationBottomSheet
import com.inscopelabs.abx.binbox.ui.components.terminal.FileTransferBottomSheet
import com.inscopelabs.abx.binbox.ui.components.terminal.TerminalSessionTabsBar
import com.inscopelabs.abx.binbox.ui.i18n.LocalAppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinBoxApp(
    viewModel: BinBoxViewModel = viewModel(),
    hosts: List<HostEntity> = emptyList(),
    showOciWizard: Boolean = false,
    onSetShowOciWizard: (Boolean) -> Unit = {},
    showOciManagement: Boolean = false,
    onSetShowOciManagement: (Boolean) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val currentTab by viewModel.currentAppTab.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activeSessionIndex.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val selectedSnippet by viewModel.selectedSnippetForRun.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var isFileTransferSheetOpen by remember { mutableStateOf(false) }
    var showPackagesSheet by remember { mutableStateOf(false) }
    val isSessionSwitcherOpen by viewModel.isSessionSwitcherOpen.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ImmersiveBg,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = ImmersiveSurfaceElevated,
                    contentColor = ImmersiveTextPrimary,
                    actionColor = ImmersivePrimary
                )
            }
        },
        topBar = {
            // Immersive Header
            Surface(
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Far Left: Rectangular Back Arrow Button (activates persistent notification and minimizes to background)
                    IconButton(
                        onClick = {
                            BinBoxLogger.i("BinBoxApp", "Top Bar back button clicked: activating terminal persistent notification")
                            TerminalForegroundService.startService(
                                context = context,
                                activeShellTitle = activeSession?.title,
                                sessionCount = if (activeSession != null) 1 else 0
                            )
                            (context as? android.app.Activity)?.moveTaskToBack(true)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: App Name & Connection Pulse aligned horizontally and vertically in the center
                    val isConnected = activeSession != null
                    val rawShellName = activeSession?.title ?: "local-device"
                    val shellName = rawShellName.uppercase()
                    val statusText = if (isConnected) strings.statusConnected.uppercase() else strings.statusStandby.uppercase()

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "BinBox",
                            color = ImmersiveTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )

                        // Status Indicator Row with Live Pulse:
                        // Shell name on left of color indicator, status 'Connected' on right of color indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = shellName,
                                color = if (isConnected) ImmersiveTextSecondary else ImmersiveTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )

                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) ImmersiveStatusGreen else ImmersiveTextMuted)
                            )

                            Text(
                                text = statusText,
                                color = if (isConnected) ImmersiveTextSecondary else ImmersiveTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    // Far Right: Header Quick Settings in a rectangular button (converted from round)
                    IconButton(
                        onClick = {
                            if (currentTab == AppTab.SETTINGS) {
                                viewModel.setAppTab(AppTab.TERMINAL)
                            } else {
                                viewModel.setAppTab(AppTab.SETTINGS)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = if (currentTab == AppTab.SETTINGS) Icons.Default.Close else Icons.Outlined.Settings,
                            contentDescription = strings.tabSettings,
                            tint = if (currentTab == AppTab.SETTINGS) ImmersivePrimary else ImmersiveTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            BinBoxBottomBar(
                currentTab = currentTab,
                terminalLabel = strings.tabTerminal,
                hostsLabel = strings.tabHosts,
                onToggleTerminalHosts = {
                    if (currentTab == AppTab.HOSTS) {
                        viewModel.setAppTab(AppTab.TERMINAL)
                    } else {
                        viewModel.setAppTab(AppTab.HOSTS)
                    }
                },
                onContextMenuClick = {
                    // Future context-specific menu
                    viewModel.showSnackbar("Context menu: active for ${if (currentTab == AppTab.HOSTS) strings.tabHosts else strings.tabTerminal}")
                },
                onMoreMenuClick = {
                    isMoreMenuOpen = true
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Unified Terminal & Hosts Session Tabs Bar (Top level for Terminal and Hosts modes)
            if (currentTab == AppTab.TERMINAL || currentTab == AppTab.HOSTS) {
                TerminalSessionTabsBar(
                    viewModel = viewModel,
                    onOpenOciWizard = { onSetShowOciWizard(true) },
                    onOpenPackagesSheet = { showPackagesSheet = true }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(
                    targetState = currentTab,
                    label = "tab_crossfade"
                ) { tab ->
                    when (tab) {
                        AppTab.TERMINAL -> TerminalScreen(viewModel = viewModel, showTabsBar = false)
                        AppTab.HOSTS -> HostsScreen(viewModel = viewModel)
                        AppTab.SNIPPETS -> SnippetsScreen(viewModel = viewModel)
                        AppTab.KEYS -> KeysScreen(viewModel = viewModel)
                        AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Server Telemetry Modal
    telemetry?.let { data ->
        TelemetryDialog(
            telemetry = data,
            onDismiss = { viewModel.dismissTelemetry() }
        )
    }

    // Snippet Parameter Runner Modal
    selectedSnippet?.let { snippet ->
        SnippetRunnerDialog(
            snippet = snippet,
            onDismiss = { viewModel.dismissSnippetDialog() },
            onExecute = { resolvedCmd ->
                viewModel.executeSnippet(snippet, resolvedCmd)
            }
        )
    }

    // Oracle Cloud Provisioning Wizard Modal
    if (showOciWizard) {
        Dialog(
            onDismissRequest = { onSetShowOciWizard(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OciOnboardingScreen(
                onDismiss = { onSetShowOciWizard(false) },
                // Provisioning just verifiably completed (a host was registered) — land the
                // user straight in management instead of dropping them back wherever they were.
                onShellReady = {
                    onSetShowOciWizard(false)
                    onSetShowOciManagement(true)
                }
            )
        }
    }

    // Oracle Cloud Management Modal — once provisioning has verifiably completed (a real
    // registered host exists, per OciProvisioningStatus), every OCI entry point lands here
    // instead of re-running onboarding. "Provision another VM" still drops back into the wizard.
    if (showOciManagement) {
        val ociHosts = hosts.filter { OciProvisioningStatus.isOciHost(it) }
        Dialog(
            onDismissRequest = { onSetShowOciManagement(false) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OciManagementScreen(
                hosts = ociHosts,
                onConnect = { host ->
                    onSetShowOciManagement(false)
                    viewModel.connectToHost(host)
                },
                onPing = { host -> viewModel.pingHost(host) },
                onToggleFavorite = { host -> viewModel.toggleHostFavorite(host) },
                onProvisionAnother = {
                    onSetShowOciManagement(false)
                    onSetShowOciWizard(true)
                },
                onDismiss = { onSetShowOciManagement(false) }
            )
        }
    }

    // More Navigation Bottom Sheet (Scripts, Keys, Files, Preferences)
    if (isMoreMenuOpen) {
        MoreNavigationBottomSheet(
            onDismiss = { isMoreMenuOpen = false },
            onNavigateToScripts = { viewModel.setAppTab(AppTab.SNIPPETS) },
            onNavigateToKeys = { viewModel.setAppTab(AppTab.KEYS) },
            onOpenFiles = { isFileTransferSheetOpen = true },
            onOpenContextSettings = { viewModel.setAppTab(AppTab.SETTINGS) },
            scriptsLabel = strings.tabScripts,
            keysLabel = strings.tabKeys,
            filesLabel = "Files"
        )
    }

    // File & Directory Transfer Bottom Sheet (opened from More menu)
    if (isFileTransferSheetOpen) {
        FileTransferBottomSheet(
            activeSession = activeSession,
            onDismiss = { isFileTransferSheetOpen = false }
        )
    }

    // Session Switcher Bottom Sheet (Active terminals quick switcher)
    if (isSessionSwitcherOpen) {
        SessionSwitcherSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setSessionSwitcherOpen(false) }
        )
    }

    // Local Shell Packages Bottom Sheet
    if (showPackagesSheet) {
        LocalShellModulesSheet(
            onDismiss = { showPackagesSheet = false }
        )
    }
}
