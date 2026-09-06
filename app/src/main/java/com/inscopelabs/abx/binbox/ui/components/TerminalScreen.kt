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
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.terminal.model.*
import com.inscopelabs.abx.binbox.ui.components.terminal.*
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: BinBoxViewModel,
    showTabsBar: Boolean = false,
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
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val connectionProfiles by viewModel.connectionProfiles.collectAsStateWithLifecycle()
    val renameDialogSessionIndex by viewModel.renameDialogSessionIndex.collectAsStateWithLifecycle()
    val isSessionSwitcherOpen by viewModel.isSessionSwitcherOpen.collectAsStateWithLifecycle()
    val isWorkspaceDialogOpen by viewModel.isWorkspaceDialogOpen.collectAsStateWithLifecycle()

    val inputFocusRequester = remember { FocusRequester() }
    var showPackagesSheet by remember { mutableStateOf(false) }
    var isFileTransferOpen by remember { mutableStateOf(false) }
    val ociLauncher = LocalOciWizardLauncher.current

    // Shadow of the current line, kept ONLY so BinBox's own history browser
    // (a separate app feature, not the live terminal) has something to record.
    // It plays no part in what's displayed or what's sent to the shell — every
    // byte is already forwarded immediately as it's typed. History recall
    // in the terminal itself (up/down arrows) is the shell's own readline,
    // same as any other terminal — see sendRaw/sendBackspace/sendEnter below.
    var typedLineShadow by remember { mutableStateOf("") }

    fun recordShadowIfComplete(shadow: String): String {
        if (shadow.isNotBlank()) {
            val session = activeSession
            if (session != null) {
                scope.launch {
                    viewModel.historyUseCases.recordHistory(shadow, session.hostLabel)
                }
            }
        }
        return ""
    }

    val sendRaw: (String) -> Unit = { text ->
        viewModel.sendRawInput(text)
        // A paste can contain embedded newlines (each line executes in turn,
        // same as any terminal); split so each completed line still gets
        // recorded into BinBox's own history browser.
        var shadow = typedLineShadow
        var remaining = text
        while (true) {
            val breakIndex = remaining.indexOfFirst { it == '\n' || it == '\r' }
            if (breakIndex == -1) {
                shadow += remaining
                break
            }
            shadow = recordShadowIfComplete(shadow + remaining.substring(0, breakIndex))
            remaining = remaining.substring(breakIndex + 1)
        }
        typedLineShadow = shadow
    }

    val sendBackspace: () -> Unit = {
        viewModel.sendRawInput("\u007F")
        if (typedLineShadow.isNotEmpty()) {
            typedLineShadow = typedLineShadow.dropLast(1)
        }
    }

    val sendEnter: () -> Unit = {
        viewModel.sendRawInput("\r")
        typedLineShadow = recordShadowIfComplete(typedLineShadow)
    }

    val sendArrowUp: () -> Unit = { viewModel.sendSpecialKey(TerminalKey.ARROW_UP) }
    val sendArrowDown: () -> Unit = { viewModel.sendSpecialKey(TerminalKey.ARROW_DOWN) }

    if (showPackagesSheet) {
        LocalShellModulesSheet(onDismiss = { showPackagesSheet = false })
    }

    // Session lines observation
    val sessionLines by (activeSession?.lines?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) })
    val sessionState by (activeSession?.state?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(SessionState.Disconnected) })

    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new output. sessionLines already reflects every
    // keystroke (the PTY echoes it back through the real parser), so this no
    // longer needs a separate client input value as a trigger.
    LaunchedEffect(sessionLines) {
        if (sessionLines.isNotEmpty()) {
            listState.animateScrollToItem(sessionLines.size)
        }
    }

    // Cursor blink: only BLINKING_BLOCK actually blinks — BLOCK/UNDERLINE/BAR
    // stay solid. Typing resets to visible and holds solid briefly (industry
    // standard — a blinking cursor while actively typing reads as broken).
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(cursorStyle, typedLineShadow) {
        if (cursorStyle != CursorStyle.BLINKING_BLOCK) {
            cursorVisible = true
            return@LaunchedEffect
        }
        cursorVisible = true
        delay(600) // pause blinking briefly after activity
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
        // 1. Session Tabs Strip & Quick Actions (when showTabsBar is requested)
        if (showTabsBar) {
            TerminalSessionTabsBar(
                viewModel = viewModel,
                onOpenOciWizard = { ociLauncher() },
                onOpenPackagesSheet = { showPackagesSheet = true }
            )
        }

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
                val log = activeSession?.cleanPlainText ?: ""
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Log", log))
                viewModel.showSnackbar("Copied terminal output to clipboard")
            },
            onShareLog = {
                val log = activeSession?.cleanPlainText ?: ""
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

        // 4. Square-Cornered Terminal Window with Integrated Prompt
        TerminalBufferView(
            activeSession = activeSession,
            sessionLines = sessionLines,
            listState = listState,
            currentTheme = currentTheme,
            fontSizeSp = fontSizeSp,
            cursorStyle = cursorStyle,
            cursorVisible = cursorVisible,
            searchQuery = searchQuery,
            inputFocusRequester = inputFocusRequester,
            onLaunchDemo = { viewModel.openDemoSession() },
            onLaunchLocal = { viewModel.openLocalSession() },
            onLaunchOci = { ociLauncher() },
            onRawInsert = sendRaw,
            onBackspace = sendBackspace,
            onEnter = sendEnter,
            onArrowUp = sendArrowUp,
            onArrowDown = sendArrowDown,
            modifier = Modifier.weight(1f)
        )

        // 5. Keyboard Accessory Row
        Surface(
            color = ImmersiveBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            TerminalQuickKeysBar(
                ctrlLatched = ctrlLatched,
                altLatched = altLatched,
                onToggleCtrl = { viewModel.toggleCtrl() },
                onToggleAlt = { viewModel.toggleAlt() },
                onSendSpecialKey = { specialKey ->
                    if (specialKey == TerminalKey.CTRL_C) {
                        typedLineShadow = ""
                    }
                    viewModel.sendSpecialKey(specialKey)
                },
                onSendRawInput = sendRaw
            )
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
