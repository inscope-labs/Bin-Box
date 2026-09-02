package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.ServerTelemetry

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
    icon: ImageVector,
    color: Color
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
