package com.inscopelabs.abx.binbox.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.oci.wizard.LocalOciWizardLauncher
import com.inscopelabs.abx.binbox.terminal.model.*
import com.inscopelabs.abx.binbox.ui.components.terminal.*
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel
import kotlinx.coroutines.delay

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
    var showPackagesSheet by remember { mutableStateOf(false) }
    var isFileTransferOpen by remember { mutableStateOf(false) }
    val ociLauncher = LocalOciWizardLauncher.current

    if (showPackagesSheet) {
        LocalShellModulesSheet(onDismiss = { showPackagesSheet = false })
    }

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
        // 1. Session Tabs Strip & Quick Actions
        TerminalSessionTabsBar(
            sessions = sessions,
            activeIdx = activeIdx,
            activeWorkspace = activeWorkspace,
            workspaces = workspaces,
            onSelectSession = { viewModel.selectSession(it) },
            onCloseSession = { viewModel.closeSession(it) },
            onOpenRenameDialog = { viewModel.openRenameDialog(it) },
            onDuplicateSession = { viewModel.duplicateSession(it) },
            onMoveSession = { from, to -> viewModel.moveSession(from, to) },
            onSwitchWorkspace = { viewModel.switchWorkspace(it) },
            onOpenWorkspaceDialog = { viewModel.openWorkspaceDialog() },
            onOpenSessionSwitcher = { viewModel.setSessionSwitcherOpen(true) },
            onOpenDemoSession = { viewModel.openDemoSession() },
            onOpenLocalSession = { viewModel.openLocalSession() },
            onOpenOciWizard = { ociLauncher() },
            onOpenPackagesSheet = { showPackagesSheet = true },
            onNavigateToHosts = { viewModel.setAppTab(AppTab.HOSTS) }
        )

        // 2. Terminal Utility Action Bar (Search, Font, Clear, Telemetry, Transfer)
        TerminalUtilityBar(
            activeSession = activeSession,
            sessionState = sessionState,
            isSearching = isSearching,
            onToggleSearching = { viewModel.toggleSearching(it) },
            onOpenFileTransfer = { isFileTransferOpen = true },
            onProbeTelemetry = {
                activeSession?.let { viewModel.probeHostTelemetry(it.hostLabel) }
            },
            onFontZoomIn = { viewModel.setFontSize(fontSizeSp + 1) },
            onFontZoomOut = { viewModel.setFontSize(fontSizeSp - 1) },
            onCopyLog = {
                val log = activeSession?.rawLogText ?: ""
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Log", log))
                viewModel.showSnackbar("Copied terminal output to clipboard")
            },
            onShareLog = {
                val log = activeSession?.rawLogText ?: ""
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, log)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share Terminal Log"))
            },
            onClearTerminal = { viewModel.clearCurrentTerminal() }
        )

        // 3. Search Bar (if active)
        AnimatedVisibility(visible = isSearching) {
            TerminalSearchBar(
                searchQuery = searchQuery,
                sessionLines = sessionLines,
                listState = listState,
                scope = scope,
                onQueryChange = { viewModel.setSearchQuery(it) },
                onCloseSearch = { viewModel.toggleSearching(false) }
            )
        }

        // 4. Immersive Floating Terminal Window
        TerminalBufferView(
            activeSession = activeSession,
            sessionLines = sessionLines,
            listState = listState,
            currentTheme = currentTheme,
            fontSizeSp = fontSizeSp,
            cursorStyle = cursorStyle,
            cursorVisible = cursorVisible,
            searchQuery = searchQuery,
            onLaunchDemo = { viewModel.openDemoSession() },
            onLaunchLocal = { viewModel.openLocalSession() },
            onLaunchOci = { ociLauncher() },
            modifier = Modifier.weight(1f)
        )

        // 5. Keyboard Accessory Row
        Surface(
            color = ImmersiveBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                TerminalQuickKeysBar(
                    ctrlLatched = ctrlLatched,
                    altLatched = altLatched,
                    onToggleCtrl = { viewModel.toggleCtrl() },
                    onToggleAlt = { viewModel.toggleAlt() },
                    onSendSpecialKey = { viewModel.sendSpecialKey(it) },
                    onSendRawInput = { viewModel.sendRawInput(it) },
                    onSendCommand = { viewModel.sendCommand(it) }
                )

                // 6. Immersive Command Input Capsule
                TerminalInputCapsule(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    onSendCommand = { viewModel.sendCommand(it) },
                    onSendSpecialKey = { viewModel.sendSpecialKey(it) },
                    history = history,
                    inputFocusRequester = inputFocusRequester
                )
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

        // File & Directory Transfer Bottom Sheet
        if (isFileTransferOpen) {
            FileTransferBottomSheet(
                activeSession = activeSession,
                onDismiss = { isFileTransferOpen = false }
            )
        }
    }
}
