package com.inscopelabs.abx.binbox.ui.components.hosts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun HostCard(
    host: HostEntity,
    onConnect: () -> Unit,
    onPing: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Protocol Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (host.protocol) {
                                    "SSH" -> ImmersivePrimary.copy(alpha = 0.15f)
                                    "LOCAL_SHELL" -> ImmersiveStatusGreen.copy(alpha = 0.15f)
                                    "DEMO_HOST" -> ImmersiveStatusAmber.copy(alpha = 0.15f)
                                    else -> ImmersiveComponent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (host.protocol) {
                                "SSH" -> Icons.Default.Terminal
                                "LOCAL_SHELL" -> Icons.Default.PhoneAndroid
                                "DEMO_HOST" -> Icons.Default.Cloud
                                else -> Icons.Default.Dns
                            },
                            contentDescription = null,
                            tint = when (host.protocol) {
                                "SSH" -> ImmersivePrimary
                                "LOCAL_SHELL" -> ImmersiveStatusGreen
                                "DEMO_HOST" -> ImmersiveStatusAmber
                                else -> ImmersiveTextPrimary
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = host.label,
                            color = ImmersiveTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (host.protocol == "LOCAL_SHELL") "Android Shell" else "${host.username}@${host.host}:${host.port}",
                                color = ImmersiveTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (host.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (host.isFavorite) ImmersiveStatusAmber else ImmersiveTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Chips Row (Protocol, Group, Latency, Auth)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Protocol Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                ) {
                    Text(
                        text = host.protocol,
                        color = ImmersivePrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Group Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                ) {
                    Text(
                        text = host.groupTag,
                        color = ImmersiveTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Latency Badge
                host.lastLatencyMs?.let { latency ->
                    val latencyColor = when {
                        latency < 30 -> ImmersiveStatusGreen
                        latency < 100 -> ImmersiveStatusAmber
                        else -> ImmersiveStatusRed
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(latencyColor.copy(alpha = 0.15f))
                            .border(0.5.dp, latencyColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(latencyColor)
                        )
                        Text(
                            text = "${latency}ms",
                            color = latencyColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (Connect, Ping, Edit, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onPing,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Outlined.NetworkPing, null, tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ping", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.Edit, "Edit", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                            .testTag("delete_host_button_${host.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = ImmersiveStatusRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }

                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = ImmersiveOnPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
