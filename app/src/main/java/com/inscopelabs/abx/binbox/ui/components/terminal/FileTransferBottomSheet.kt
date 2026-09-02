package com.inscopelabs.abx.binbox.ui.components.terminal

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.transfer.FileTransferEngine
import com.inscopelabs.abx.binbox.terminal.transfer.TransferProgress
import com.inscopelabs.abx.binbox.terminal.transfer.TransferStatus
import com.inscopelabs.abx.binbox.ui.theme.*

private sealed class LastTransferRequest {
    data class UriRequest(val uri: Uri, val isDirectory: Boolean) : LastTransferRequest()
    data class TextRequest(val fileName: String, val content: String) : LastTransferRequest()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransferBottomSheet(
    activeSession: ShellSession?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transferEngine = remember { FileTransferEngine(context) }
    val progressState by transferEngine.progress.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isDirectorySelected by remember { mutableStateOf(false) }
    var selectedDisplayName by remember { mutableStateOf<String?>(null) }

    var textFileName by remember { mutableStateOf("") }
    var textFileContent by remember { mutableStateOf("") }
    var lastTransferRequest by remember { mutableStateOf<LastTransferRequest?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            isDirectorySelected = false
            selectedDisplayName = uri.lastPathSegment ?: "Selected File"
            transferEngine.reset()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            selectedUri = treeUri
            isDirectorySelected = true
            selectedDisplayName = treeUri.lastPathSegment ?: "Selected Directory"
            transferEngine.reset()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (progressState.status == TransferStatus.STREAMING || progressState.status == TransferStatus.PACKING || progressState.status == TransferStatus.VERIFYING) {
                transferEngine.cancel()
            }
            onDismiss()
        },
        containerColor = ImmersiveSurfaceElevated,
        contentColor = ImmersiveTextPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = ImmersiveBorderSubtle
            )
        },
        modifier = modifier.testTag("file_transfer_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Title and Active Shell Target Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Transfer to Shell",
                        style = MaterialTheme.typography.titleLarge,
                        color = ImmersiveTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Routes silently into active working directory",
                        style = MaterialTheme.typography.bodySmall,
                        color = ImmersiveTextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        if (progressState.status == TransferStatus.STREAMING || progressState.status == TransferStatus.VERIFYING) {
                            transferEngine.cancel()
                        }
                        onDismiss()
                    },
                    modifier = Modifier.testTag("close_transfer_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close transfer sheet",
                        tint = ImmersiveTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Target Session Info
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ImmersiveComponent,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ImmersivePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeSession?.hostLabel ?: "No Active Session",
                            color = ImmersiveTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Target: Remote active shell (`pwd`)",
                            color = ImmersiveTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // State Transitions: Idle/Selecting vs In-Progress vs Completed vs Error
            when (progressState.status) {
                TransferStatus.COMPLETED -> {
                    TransferCompletedView(
                        progress = progressState,
                        onTransferAnother = {
                            selectedUri = null
                            selectedDisplayName = null
                            textFileName = ""
                            textFileContent = ""
                            transferEngine.reset()
                        },
                        onDone = onDismiss
                    )
                }

                TransferStatus.STREAMING, TransferStatus.PACKING, TransferStatus.SCANNING, TransferStatus.VERIFYING -> {
                    TransferInProgressView(
                        progress = progressState,
                        onCancel = { transferEngine.cancel() }
                    )
                }

                TransferStatus.ERROR, TransferStatus.CANCELLED -> {
                    TransferErrorView(
                        errorMessage = progressState.errorMessage ?: "Transfer was interrupted",
                        onRetry = {
                            if (activeSession != null) {
                                when (val req = lastTransferRequest) {
                                    is LastTransferRequest.UriRequest -> {
                                        transferEngine.startTransfer(
                                            session = activeSession,
                                            uri = req.uri,
                                            isDirectory = req.isDirectory
                                        )
                                    }
                                    is LastTransferRequest.TextRequest -> {
                                        transferEngine.startTextFileTransfer(
                                            session = activeSession,
                                            fileName = req.fileName,
                                            content = req.content
                                        )
                                    }
                                    null -> {
                                        if (selectedUri != null) {
                                            transferEngine.startTransfer(
                                                session = activeSession,
                                                uri = selectedUri!!,
                                                isDirectory = isDirectorySelected
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onSelectNew = {
                            selectedUri = null
                            selectedDisplayName = null
                            textFileName = ""
                            textFileContent = ""
                            transferEngine.reset()
                        }
                    )
                }

                TransferStatus.IDLE -> {
                    TransferSelectionView(
                        selectedTabIndex = selectedTabIndex,
                        onTabSelect = { selectedTabIndex = it },
                        selectedUri = selectedUri,
                        isDirectory = isDirectorySelected,
                        selectedDisplayName = selectedDisplayName,
                        textFileName = textFileName,
                        onTextFileNameChange = { textFileName = it },
                        textFileContent = textFileContent,
                        onTextFileContentChange = { textFileContent = it },
                        onPickFile = {
                            isDirectorySelected = false
                            filePickerLauncher.launch("*/*")
                        },
                        onPickFolder = {
                            isDirectorySelected = true
                            folderPickerLauncher.launch(null)
                        },
                        onClearSelection = {
                            selectedUri = null
                            selectedDisplayName = null
                        },
                        onStartUriTransfer = {
                            if (activeSession != null && selectedUri != null) {
                                lastTransferRequest = LastTransferRequest.UriRequest(selectedUri!!, isDirectorySelected)
                                transferEngine.startTransfer(
                                    session = activeSession,
                                    uri = selectedUri!!,
                                    isDirectory = isDirectorySelected
                                )
                            }
                        },
                        onStartTextTransfer = { name, content ->
                            if (activeSession != null && name.isNotBlank()) {
                                lastTransferRequest = LastTransferRequest.TextRequest(name, content)
                                transferEngine.startTextFileTransfer(
                                    session = activeSession,
                                    fileName = name,
                                    content = content
                                )
                            }
                        },
                        hasActiveSession = activeSession != null
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferSelectionView(
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    selectedUri: Uri?,
    isDirectory: Boolean,
    selectedDisplayName: String?,
    textFileName: String,
    onTextFileNameChange: (String) -> Unit,
    textFileContent: String,
    onTextFileContentChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit,
    onClearSelection: () -> Unit,
    onStartUriTransfer: () -> Unit,
    onStartTextTransfer: (String, String) -> Unit,
    hasActiveSession: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tab Row: File / Folder / Create Text
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = ImmersiveComponent,
            contentColor = ImmersivePrimary,
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ImmersiveBorderVerySubtle, RoundedCornerShape(12.dp))
                .testTag("transfer_mode_tab_row")
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { onTabSelect(0) },
                text = { Text("File", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = ImmersivePrimary,
                unselectedContentColor = ImmersiveTextSecondary,
                modifier = Modifier.testTag("tab_single_file")
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { onTabSelect(1) },
                text = { Text("Folder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = CyanAccent,
                unselectedContentColor = ImmersiveTextSecondary,
                modifier = Modifier.testTag("tab_folder_tree")
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { onTabSelect(2) },
                text = { Text("Create / Paste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Outlined.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = AmberAccent,
                unselectedContentColor = ImmersiveTextSecondary,
                modifier = Modifier.testTag("tab_create_paste")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> {
                // Single File Mode
                if (selectedUri == null || isDirectory) {
                    Surface(
                        onClick = onPickFile,
                        shape = RoundedCornerShape(14.dp),
                        color = ImmersiveComponent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("pick_file_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.InsertDriveFile,
                                contentDescription = "Select File",
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose File to Transfer",
                                color = ImmersiveTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Packs and extracts directly into remote shell directory",
                                color = ImmersiveTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    SelectedItemCard(
                        displayName = selectedDisplayName ?: "Selected File",
                        isDirectory = false,
                        onClear = onClearSelection
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStartUriTransfer,
                        enabled = hasActiveSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersivePrimary,
                            contentColor = ImmersiveOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_transfer_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload File to Active Shell", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            1 -> {
                // Folder Mode
                if (selectedUri == null || !isDirectory) {
                    Surface(
                        onClick = onPickFolder,
                        shape = RoundedCornerShape(14.dp),
                        color = ImmersiveComponent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("pick_folder_button")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = "Select Folder",
                                tint = CyanAccent,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose Directory Tree to Transfer",
                                color = ImmersiveTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recursively archives and preserves folder hierarchy",
                                color = ImmersiveTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    SelectedItemCard(
                        displayName = selectedDisplayName ?: "Selected Directory",
                        isDirectory = true,
                        onClear = onClearSelection
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStartUriTransfer,
                        enabled = hasActiveSession,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_transfer_button")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload Folder Tree to Active Shell", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            2 -> {
                // Create / Paste File Mode
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = textFileName,
                        onValueChange = onTextFileNameChange,
                        label = { Text("Target Filename", fontSize = 12.sp) },
                        placeholder = { Text(".env, config.json, script.sh", color = ImmersiveTextSecondary.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Description, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (textFileName.isNotBlank()) {
                                IconButton(onClick = { onTextFileNameChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = ImmersiveBorderSubtle,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("text_transfer_filename_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "File Content",
                            style = MaterialTheme.typography.bodySmall,
                            color = ImmersiveTextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${textFileContent.length} chars",
                                color = ImmersiveTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            FilledTonalButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrEmpty()) {
                                        onTextFileContentChange(clip)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ImmersiveComponentHover,
                                    contentColor = AmberAccent
                                ),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("paste_clipboard_file_button")
                            ) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = textFileContent,
                        onValueChange = onTextFileContentChange,
                        placeholder = { Text("Paste or enter file content to save directly in remote shell...", color = ImmersiveTextSecondary.copy(alpha = 0.5f), fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = ImmersiveBorderSubtle,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = ImmersiveTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("text_transfer_content_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onStartTextTransfer(textFileName, textFileContent) },
                        enabled = hasActiveSession && textFileName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberAccent,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_text_transfer_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create & Write to Shell", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedItemCard(
    displayName: String,
    isDirectory: Boolean,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ImmersiveComponent,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDirectory) CyanAccent.copy(alpha = 0.4f) else ImmersivePrimary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isDirectory) CyanAccent.copy(alpha = 0.2f) else ImmersivePrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (isDirectory) CyanAccent else ImmersivePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = ImmersiveTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isDirectory) "Directory Tree (Recursive)" else "Single File",
                    color = ImmersiveTextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp).testTag("clear_selection_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Selection",
                    tint = ImmersiveTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TransferInProgressView(
    progress: TransferProgress,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Linear Progress Indicator
        LinearProgressIndicator(
            progress = { progress.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .testTag("transfer_progress_bar"),
            color = if (progress.status == TransferStatus.VERIFYING) AmberAccent else ImmersivePrimary,
            trackColor = ImmersiveBorderVerySubtle
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = progress.currentItemName,
                color = ImmersiveTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${progress.percentageInt}%",
                color = if (progress.status == TransferStatus.VERIFYING) AmberAccent else ImmersivePrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (progress.status) {
                    TransferStatus.SCANNING -> "Scanning item tree..."
                    TransferStatus.PACKING -> "Compressing tar.gz payload..."
                    TransferStatus.STREAMING -> "Streaming Base64 chunks silently to shell..."
                    TransferStatus.VERIFYING -> "Verifying extraction & remote sync (3s grace)..."
                    else -> "Processing..."
                },
                color = if (progress.status == TransferStatus.VERIFYING) AmberAccent else ImmersiveTextSecondary,
                fontSize = 11.sp,
                fontWeight = if (progress.status == TransferStatus.VERIFYING) FontWeight.Medium else FontWeight.Normal
            )
            Text(
                text = "${progress.formattedTransferred} / ${progress.formattedTotal}",
                color = ImmersiveTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ImmersiveStatusRed
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveStatusRed.copy(alpha = 0.5f)),
            modifier = Modifier.testTag("cancel_transfer_button")
        ) {
            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel Transfer")
        }
    }
}

@Composable
private fun TransferCompletedView(
    progress: TransferProgress,
    onTransferAnother: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big Success Checkmark
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ImmersiveStatusGreen.copy(alpha = 0.15f))
                .border(2.dp, ImmersiveStatusGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Transfer Successful",
                tint = ImmersiveStatusGreen,
                modifier = Modifier.size(40.dp).testTag("transfer_success_checkmark")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Transfer Verified & Complete!",
            color = ImmersiveTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Successfully extracted ${progress.totalFiles} file(s) into shell's current working directory.",
            color = ImmersiveTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Transfer Another vs Done
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onTransferAnother,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("transfer_another_button")
            ) {
                Text("Transfer Another", color = ImmersiveTextPrimary)
            }

            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("transfer_done_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TransferErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onSelectNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ImmersiveStatusRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = ImmersiveStatusRed,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Transfer Failed",
            color = ImmersiveTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = errorMessage,
            color = ImmersiveTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSelectNew,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Select Different", color = ImmersiveTextPrimary)
            }

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                ),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Text("Retry Transfer", fontWeight = FontWeight.Bold)
            }
        }
    }
}
