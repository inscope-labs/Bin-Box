package com.inscopelabs.abx.binbox.ui.components.terminal

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

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isDirectorySelected by remember { mutableStateOf(false) }
    var selectedDisplayName by remember { mutableStateOf<String?>(null) }

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
            if (progressState.status == TransferStatus.STREAMING || progressState.status == TransferStatus.PACKING) {
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
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
                        text = "Unpacks directly into current working directory",
                        style = MaterialTheme.typography.bodySmall,
                        color = ImmersiveTextSecondary
                    )
                }

                IconButton(
                    onClick = {
                        if (progressState.status == TransferStatus.STREAMING) {
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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            // State Transitions: Idle/Selecting vs In-Progress vs Completed vs Error
            when (progressState.status) {
                TransferStatus.COMPLETED -> {
                    TransferCompletedView(
                        progress = progressState,
                        onTransferAnother = {
                            selectedUri = null
                            selectedDisplayName = null
                            transferEngine.reset()
                        },
                        onDone = onDismiss
                    )
                }

                TransferStatus.STREAMING, TransferStatus.PACKING, TransferStatus.SCANNING -> {
                    TransferInProgressView(
                        progress = progressState,
                        onCancel = { transferEngine.cancel() }
                    )
                }

                TransferStatus.ERROR, TransferStatus.CANCELLED -> {
                    TransferErrorView(
                        errorMessage = progressState.errorMessage ?: "Transfer was interrupted",
                        onRetry = {
                            if (activeSession != null && selectedUri != null) {
                                transferEngine.startTransfer(
                                    session = activeSession,
                                    uri = selectedUri!!,
                                    isDirectory = isDirectorySelected
                                )
                            }
                        },
                        onSelectNew = {
                            selectedUri = null
                            selectedDisplayName = null
                            transferEngine.reset()
                        }
                    )
                }

                TransferStatus.IDLE -> {
                    TransferSelectionView(
                        selectedUri = selectedUri,
                        isDirectory = isDirectorySelected,
                        selectedDisplayName = selectedDisplayName,
                        onPickFile = { filePickerLauncher.launch("*/*") },
                        onPickFolder = { folderPickerLauncher.launch(null) },
                        onClearSelection = {
                            selectedUri = null
                            selectedDisplayName = null
                        },
                        onStartTransfer = {
                            if (activeSession != null && selectedUri != null) {
                                transferEngine.startTransfer(
                                    session = activeSession,
                                    uri = selectedUri!!,
                                    isDirectory = isDirectorySelected
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
    selectedUri: Uri?,
    isDirectory: Boolean,
    selectedDisplayName: String?,
    onPickFile: () -> Unit,
    onPickFolder: () -> Unit,
    onClearSelection: () -> Unit,
    onStartTransfer: () -> Unit,
    hasActiveSession: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (selectedUri == null) {
            // Choice Cards: File vs Folder Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pick Single File Card
                Surface(
                    onClick = onPickFile,
                    shape = RoundedCornerShape(14.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .testTag("pick_file_button")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.InsertDriveFile,
                            contentDescription = "Select File",
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select File",
                            color = ImmersiveTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload single file",
                            color = ImmersiveTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Pick Directory Tree Card
                Surface(
                    onClick = onPickFolder,
                    shape = RoundedCornerShape(14.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .testTag("pick_folder_button")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Select Folder",
                            tint = CyanAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select Folder",
                            color = ImmersiveTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload full directory",
                            color = ImmersiveTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        } else {
            // Selected Item Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ImmersiveComponent,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.4f)),
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
                            text = selectedDisplayName ?: "Selected item",
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
                        onClick = onClearSelection,
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

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = onStartTransfer,
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
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upload to Active Shell",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
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
            color = ImmersivePrimary,
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
                color = ImmersivePrimary,
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
                    TransferStatus.STREAMING -> "Streaming Base64 chunks to shell..."
                    else -> "Processing..."
                },
                color = ImmersiveTextSecondary,
                fontSize = 11.sp
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
            text = "Transfer Complete!",
            color = ImmersiveTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Successfully unpacked into your shell's current working directory.",
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
