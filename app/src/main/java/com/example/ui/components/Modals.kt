package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SnippetEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ServerTelemetry

@Composable
fun TelemetryDialog(
    telemetry: ServerTelemetry,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyanAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Speed, null, tint = CyanGlow, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Server Telemetry", color = Slate100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(telemetry.hostLabel, color = Slate400, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // OS & Kernel
                TelemetryMetricCard(
                    title = "Operating System & Kernel",
                    value = telemetry.osInfo,
                    icon = Icons.Default.Dns,
                    color = CyanGlow
                )

                // Uptime
                TelemetryMetricCard(
                    title = "System Uptime & Load Avg",
                    value = telemetry.uptime,
                    icon = Icons.Default.Schedule,
                    color = EmeraldGreen
                )

                // CPU Load
                TelemetryMetricCard(
                    title = "CPU Utilization",
                    value = telemetry.cpuUsage,
                    icon = Icons.Default.Memory,
                    color = AmberGlow
                )

                // Memory RAM
                TelemetryMetricCard(
                    title = "RAM Memory Allocation",
                    value = telemetry.memUsage,
                    icon = Icons.Default.Storage,
                    color = VioletAccent
                )

                // Disk Space
                TelemetryMetricCard(
                    title = "NVMe / SSD Root Partition",
                    value = telemetry.diskUsage,
                    icon = Icons.Default.PieChart,
                    color = BlueAccent
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Text("Close", color = Slate950, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate850,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Text(title, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    value,
                    color = Slate100,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SnippetRunnerDialog(
    snippet: SnippetEntity,
    onDismiss: () -> Unit,
    onExecute: (resolvedCommand: String) -> Unit
) {
    // Extract parameters from template: {{param}} or {{param:default}}
    val pattern = Regex("\\{\\{([^}]+)\\}\\}")
    val matches = remember(snippet.commandTemplate) {
        pattern.findAll(snippet.commandTemplate).map { it.groupValues[1] }.distinct().toList()
    }

    val paramValues = remember(matches) {
        mutableStateMapOf<String, String>().apply {
            matches.forEach { raw ->
                if (raw.contains(":")) {
                    val split = raw.split(":", limit = 2)
                    this[raw] = split[1]
                } else {
                    this[raw] = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Column {
                Text("Run Script in Terminal", color = Slate100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(snippet.title, color = CyanGlow, fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (matches.isNotEmpty()) {
                    Text("Provide Parameter Values:", color = Slate400, fontSize = 12.sp)
                    matches.forEach { key ->
                        val label = if (key.contains(":")) key.split(":", limit = 2)[0] else key
                        Column {
                            Text(label, color = Slate300, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            TextField(
                                value = paramValues[key] ?: "",
                                onValueChange = { paramValues[key] = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Slate850,
                                    unfocusedContainerColor = Slate850,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                ),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }
                    }
                }

                // Command Preview
                val resolvedPreview = remember(paramValues.values.toList()) {
                    var resolved = snippet.commandTemplate
                    matches.forEach { key ->
                        val value = paramValues[key] ?: ""
                        resolved = resolved.replace("{{$key}}", value)
                    }
                    resolved
                }

                Text("Resolved Command:", color = Slate400, fontSize = 11.sp)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate950,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resolvedPreview,
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var resolved = snippet.commandTemplate
                    matches.forEach { key ->
                        val value = paramValues[key] ?: ""
                        resolved = resolved.replace("{{$key}}", value)
                    }
                    onExecute(resolved)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Slate950, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Execute", color = Slate950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
