package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.terminal.engine.ShellSession
import com.inscopelabs.abx.binbox.terminal.model.SessionState
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun TerminalUtilityBar(
    activeSession: ShellSession?,
    sessionState: SessionState,
    isSearching: Boolean,
    onToggleSearching: (Boolean) -> Unit,
    onOpenFileTransfer: () -> Unit,
    onProbeTelemetry: () -> Unit,
    onFontZoomIn: () -> Unit,
    onFontZoomOut: () -> Unit,
    onCopyLog: () -> Unit,
    onShareLog: () -> Unit,
    onClearTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ImmersiveSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status and Host Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Connection Indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (sessionState) {
                                is SessionState.Connected -> ImmersiveStatusGreen
                                is SessionState.Connecting -> ImmersiveStatusAmber
                                is SessionState.Error -> ImmersiveStatusRed
                                is SessionState.Disconnected -> ImmersiveTextMuted
                            }
                        )
                )

                Text(
                    text = activeSession?.hostLabel ?: "No Active Session",
                    color = ImmersiveTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                activeSession?.let {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ImmersiveComponent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                    ) {
                        Text(
                            text = it.id.take(6).uppercase(),
                            color = ImmersivePrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quick Toolbar Actions (Telemetry, Search, Zoom, Copy, Share, Clear)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                activeSession?.let {
                    IconButton(
                        onClick = onOpenFileTransfer,
                        modifier = Modifier.size(28.dp).testTag("upload_file_button")
                    ) {
                        Icon(
                            Icons.Default.DriveFolderUpload,
                            contentDescription = "Upload file or folder",
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onProbeTelemetry,
                        modifier = Modifier.size(28.dp).testTag("probe_telemetry_button")
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "System telemetry",
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onToggleSearching(!isSearching) },
                    modifier = Modifier.size(28.dp).testTag("search_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search terminal buffer",
                        tint = if (isSearching) ImmersivePrimary else ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onFontZoomIn,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Increase font",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onFontZoomOut,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Decrease font",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onCopyLog,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy log",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShareLog,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share terminal log",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onClearTerminal,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear terminal",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
