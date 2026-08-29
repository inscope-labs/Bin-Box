package com.inscopelabs.abx.binbox.oci.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Top-level Orchestrator Dialog for inspecting unredacted OCI API call traces in real-time.
 */
@Composable
fun OciCallTraceViewerDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val TAG = "OciCallTraceViewer"
    val entries by OciCallTraceStore.entries.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    var searchQuery by remember { mutableStateOf("") }
    var copiedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BinBoxLogger.i(TAG, "Opened OciCallTraceViewerDialog with ${entries.size} entries")
    }

    val filteredEntries = remember(entries, searchQuery) {
        if (searchQuery.isBlank()) entries else {
            val q = searchQuery.trim().lowercase()
            entries.filter {
                it.url.lowercase().contains(q) ||
                    it.stageId.lowercase().contains(q) ||
                    it.stepId.lowercase().contains(q) ||
                    it.method.lowercase().contains(q) ||
                    (it.ociErrorCode?.lowercase()?.contains(q) == true) ||
                    (it.ociErrorMessage?.lowercase()?.contains(q) == true) ||
                    it.httpStatusCode?.toString()?.contains(q) == true
            }
        }
    }

    Dialog(
        onDismissRequest = {
            BinBoxLogger.i(TAG, "Dismissed OciCallTraceViewerDialog")
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize().testTag("oci_call_trace_viewer_dialog"),
            containerColor = ImmersiveBg,
            topBar = {
                Surface(
                    color = ImmersiveSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("OCI API Call Traces", color = ImmersiveTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                val errCount = entries.count { (it.httpStatusCode ?: 0) !in 200..299 || it.exceptionClass != null }
                                Text(
                                    "${entries.size} calls recorded${if (errCount > 0) " ($errCount with errors)" else ""}",
                                    color = if (errCount > 0) ImmersiveStatusRed else ImmersiveTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(OciCallTraceStore.exportAsText()))
                                        copiedFeedback = true
                                        BinBoxLogger.i(TAG, "Copied full raw trace dump to clipboard (${entries.size} entries)")
                                    },
                                    modifier = Modifier.testTag("oci_trace_export_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (copiedFeedback) "Copied!" else "Copy All", color = CyanAccent, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = {
                                        BinBoxLogger.i(TAG, "Cleared in-memory traces via viewer UI")
                                        OciCallTraceStore.clear()
                                    },
                                    modifier = Modifier.testTag("oci_trace_clear_button")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = ImmersiveTextSecondary)
                                }
                                IconButton(onClick = onDismiss, modifier = Modifier.testTag("oci_trace_close_button")) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextPrimary)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Filter by stage, step, endpoint, or error…", fontSize = 12.sp, color = ImmersiveTextMuted) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ImmersiveTextMuted, modifier = Modifier.size(16.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = ImmersiveBorderSubtle,
                                focusedContainerColor = ImmersiveSurfaceElevated,
                                unfocusedContainerColor = ImmersiveSurfaceElevated,
                                focusedTextColor = ImmersiveTextPrimary,
                                unfocusedTextColor = ImmersiveTextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("oci_trace_search_field")
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (filteredEntries.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = ImmersiveTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No API call traces found", color = ImmersiveTextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (entries.isEmpty()) "Traces will appear here automatically when the wizard makes API calls." else "No traces match the search query.",
                            color = ImmersiveTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) {
                    itemsIndexed(filteredEntries, key = { _, item -> item.id }) { index, entry ->
                        OciCallTraceCard(entry = entry, index = index)
                    }
                }
            }
        }
    }
}
