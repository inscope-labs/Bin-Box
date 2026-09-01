package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleState
import com.inscopelabs.abx.binbox.binboxshell.runtime.BinaryDescriptor
import com.inscopelabs.abx.binbox.binboxshell.runtime.ShellTier
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun TierModuleCard(
    tier: ShellTier,
    title: String,
    description: String,
    binaries: List<BinaryDescriptor>,
    state: ModuleState,
    onInstallClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveComponent),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = ImmersiveTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = description,
                        color = ImmersiveTextSecondary,
                        fontSize = 11.sp
                    )
                }

                when (state) {
                    is ModuleState.Installed -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ImmersiveStatusGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ImmersiveStatusGreen, modifier = Modifier.size(14.dp))
                                Text("Ready", color = ImmersiveStatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is ModuleState.Installing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ImmersivePrimary
                            )
                            Text("${(state.progress * 100).toInt()}%", color = ImmersivePrimary, fontSize = 11.sp)
                        }
                    }
                    is ModuleState.Available, is ModuleState.Failed -> {
                        Button(
                            onClick = onInstallClick,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ImmersivePrimary,
                                contentColor = ImmersiveOnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (binaries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Includes: " + binaries.joinToString(", ") { it.name },
                    color = ImmersiveTextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
