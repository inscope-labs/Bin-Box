package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSwitcherSheet(
    viewModel: BinBoxViewModel,
    onDismiss: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeIdx by viewModel.activeSessionIndex.collectAsStateWithLifecycle()
    val activeWorkspace by viewModel.activeWorkspace.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ImmersiveBorderSubtle) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Terminal Sessions",
                        color = ImmersiveTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${sessions.size} sessions open • Workspace: ${activeWorkspace.name}",
                        color = ImmersiveTextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (sessions.isNotEmpty()) {
                    TextButton(onClick = { viewModel.closeAllSessions() }) {
                        Text("Close All", color = ImmersiveStatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Terminal, null, tint = ImmersiveTextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active terminal sessions", color = ImmersiveTextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sessions) { index, session ->
                        val isSelected = index == activeIdx
                        val state by session.state.collectAsStateWithLifecycle()
                        val bytesIn by session.bytesReceived.collectAsStateWithLifecycle()
                        val bytesOut by session.bytesSent.collectAsStateWithLifecycle()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) ImmersiveComponent else ImmersiveSurfaceElevated,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSession(index)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Status Indicator
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (state) {
                                                is SessionState.Connected -> ImmersiveStatusGreen
                                                is SessionState.Connecting -> ImmersiveStatusAmber
                                                is SessionState.Error -> ImmersiveStatusRed
                                                is SessionState.Disconnected -> ImmersiveTextMuted
                                            }
                                        )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = session.title,
                                            color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = ImmersivePrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    color = ImmersivePrimary,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${session.hostLabel} • In: ${bytesIn / 1024}KB, Out: ${bytesOut / 1024}KB",
                                        color = ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Quick Actions: Move Up, Move Down, Duplicate, Rename, Close
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { viewModel.moveSession(index, index - 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, "Move up", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    if (index < sessions.size - 1) {
                                        IconButton(
                                            onClick = { viewModel.moveSession(index, index + 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, "Move down", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.duplicateSession(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, "Duplicate tab", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            onDismiss()
                                            viewModel.openRenameDialog(index)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Rename tab", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.closeSession(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, "Close session", tint = ImmersiveStatusRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Fast Session Creator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.openLocalSession()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersivePrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle)
                ) {
                    Icon(Icons.Default.Terminal, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Local Shell", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        viewModel.openDemoSession()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    )
                ) {
                    Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cloud Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
