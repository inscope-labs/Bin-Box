package com.inscopelabs.abx.binbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.inscopelabs.abx.binbox.oci.wizard.LocalOciWizardLauncher
import com.inscopelabs.abx.binbox.oci.wizard.OciOnboardingScreen
import com.inscopelabs.abx.binbox.ui.components.*
import com.inscopelabs.abx.binbox.ui.i18n.LocalAppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BinBoxViewModel = viewModel()
            val strings by viewModel.strings.collectAsStateWithLifecycle()
            var showOciWizard by remember { mutableStateOf(false) }

            CompositionLocalProvider(
                LocalAppStrings provides strings,
                LocalOciWizardLauncher provides { showOciWizard = true }
            ) {
                BinBoxTheme {
                    BinBoxApp(
                        viewModel = viewModel,
                        showOciWizard = showOciWizard,
                        onSetShowOciWizard = { showOciWizard = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinBoxApp(
    viewModel: BinBoxViewModel = viewModel(),
    showOciWizard: Boolean = false,
    onSetShowOciWizard: (Boolean) -> Unit = {}
) {
    val strings = LocalAppStrings.current
    val currentTab by viewModel.currentAppTab.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activeSessionIndex.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val selectedSnippet by viewModel.selectedSnippetForRun.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Luminous Squircle Terminal Icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ImmersivePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Bin Box",
                                tint = ImmersiveOnPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Bin Box",
                                color = ImmersiveTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )

                            // Status Indicator Row with Live Pulse
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                val isConnected = activeSession != null
                                val activeHostTitle = activeSession?.title ?: "local-device"

                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) ImmersiveStatusGreen else ImmersiveTextMuted)
                                )

                                Text(
                                    text = if (isConnected) "${strings.statusConnected}: $activeHostTitle" else strings.statusStandby,
                                    color = if (isConnected) ImmersiveTextSecondary else ImmersiveTextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Header Quick Settings / Telemetry Trigger
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
                            .clip(CircleShape)
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
            Surface(
                color = ImmersiveSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
            ) {
                NavigationBar(
                    containerColor = ImmersiveSurface,
                    contentColor = ImmersiveTextSecondary,
                    tonalElevation = 0.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    // 1. Terminal Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.TERMINAL,
                        onClick = { viewModel.setAppTab(AppTab.TERMINAL) },
                        icon = {
                            BadgedBox(badge = {
                                if (sessions.isNotEmpty()) {
                                    Badge(
                                        containerColor = ImmersivePrimary,
                                        contentColor = ImmersiveOnPrimary
                                    ) {
                                        Text("${sessions.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (currentTab == AppTab.TERMINAL) Icons.Default.Terminal else Icons.Outlined.Terminal,
                                    contentDescription = strings.tabTerminal
                                )
                            }
                        },
                        label = { Text(strings.tabTerminal, fontSize = 11.sp, fontWeight = if (currentTab == AppTab.TERMINAL) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ImmersivePrimary,
                            selectedTextColor = ImmersivePrimary,
                            indicatorColor = ImmersiveComponent,
                            unselectedIconColor = ImmersiveTextSecondary,
                            unselectedTextColor = ImmersiveTextSecondary
                        )
                    )

                    // 2. Hosts Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOSTS,
                        onClick = { viewModel.setAppTab(AppTab.HOSTS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.HOSTS) Icons.Default.Dns else Icons.Outlined.Dns,
                                contentDescription = strings.tabHosts
                            )
                        },
                        label = { Text(strings.tabHosts, fontSize = 11.sp, fontWeight = if (currentTab == AppTab.HOSTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ImmersivePrimary,
                            selectedTextColor = ImmersivePrimary,
                            indicatorColor = ImmersiveComponent,
                            unselectedIconColor = ImmersiveTextSecondary,
                            unselectedTextColor = ImmersiveTextSecondary
                        )
                    )

                    // 3. Scripts / Snippets Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.SNIPPETS,
                        onClick = { viewModel.setAppTab(AppTab.SNIPPETS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.SNIPPETS) Icons.Default.Code else Icons.Outlined.Code,
                                contentDescription = strings.tabScripts
                            )
                        },
                        label = { Text(strings.tabScripts, fontSize = 11.sp, fontWeight = if (currentTab == AppTab.SNIPPETS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ImmersivePrimary,
                            selectedTextColor = ImmersivePrimary,
                            indicatorColor = ImmersiveComponent,
                            unselectedIconColor = ImmersiveTextSecondary,
                            unselectedTextColor = ImmersiveTextSecondary
                        )
                    )

                    // 4. Keys Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.KEYS,
                        onClick = { viewModel.setAppTab(AppTab.KEYS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.KEYS) Icons.Default.VpnKey else Icons.Outlined.VpnKey,
                                contentDescription = strings.tabKeys
                            )
                        },
                        label = { Text(strings.tabKeys, fontSize = 11.sp, fontWeight = if (currentTab == AppTab.KEYS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ImmersivePrimary,
                            selectedTextColor = ImmersivePrimary,
                            indicatorColor = ImmersiveComponent,
                            unselectedIconColor = ImmersiveTextSecondary,
                            unselectedTextColor = ImmersiveTextSecondary
                        )
                    )

                    // 5. Settings Tab
                    NavigationBarItem(
                        selected = currentTab == AppTab.SETTINGS,
                        onClick = { viewModel.setAppTab(AppTab.SETTINGS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == AppTab.SETTINGS) Icons.Default.Settings else Icons.Outlined.Settings,
                                contentDescription = strings.tabSettings
                            )
                        },
                        label = { Text(strings.tabSettings, fontSize = 11.sp, fontWeight = if (currentTab == AppTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ImmersivePrimary,
                            selectedTextColor = ImmersivePrimary,
                            indicatorColor = ImmersiveComponent,
                            unselectedIconColor = ImmersiveTextSecondary,
                            unselectedTextColor = ImmersiveTextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    AppTab.TERMINAL -> TerminalScreen(viewModel = viewModel)
                    AppTab.HOSTS -> HostsScreen(viewModel = viewModel)
                    AppTab.SNIPPETS -> SnippetsScreen(viewModel = viewModel)
                    AppTab.KEYS -> KeysScreen(viewModel = viewModel)
                    AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { onSetShowOciWizard(false) },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OciOnboardingScreen(
                onDismiss = { onSetShowOciWizard(false) },
                onShellReady = { onSetShowOciWizard(false) }
            )
        }
    }
}
