package com.inscopelabs.abx.binbox.ui.components.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.components.DetailRow
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Local system / shell engine info panel (OS, CPU, build target, device,
 * package id, SSH engine). Module component: pure read-only display, no
 * state, no callbacks — this is normal app-wide diagnostic display code,
 * not the gated DIAGNOSTICS_INSPECTOR feature (see DiagnosticsSection.kt).
 */
@Composable
fun SystemTelemetrySection(strings: AppStrings) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ImmersivePrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Info, null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                }
                Text(strings.hostShellInfoTitle, color = ImmersiveTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            DetailRow(strings.osKernelLabel, "${System.getProperty("os.name") ?: "Android"} ${System.getProperty("os.version") ?: "Linux"}")
            DetailRow(strings.cpuArchLabel, System.getProperty("os.arch") ?: "aarch64")
            DetailRow(strings.buildTargetLabel, "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            DetailRow("Device Hardware", "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}")
            DetailRow(strings.packageIdLabel, "com.inscopelabs.abx.binbox")
            DetailRow("SSH Engine", "JSch / Modern Native Socket Engine")
        }
    }
}
