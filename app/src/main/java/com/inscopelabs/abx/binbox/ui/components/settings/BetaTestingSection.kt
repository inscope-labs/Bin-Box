package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Beta Testing enrollment toggle. Module component: purely a controlled
 * switch — [betaEnrolled] is owned by the caller (SettingsScreen), this file
 * only renders it and reports changes via [onEnrolledChange].
 */
@Composable
fun BetaTestingSection(
    betaEnrolled: Boolean,
    onEnrolledChange: (Boolean) -> Unit
) {
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
                    Icon(Icons.Default.Science, null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                }
                Text("Beta Testing", color = ImmersiveTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Opt in to try features still being proven out before they graduate to the stable release. You can turn this off at any time.",
                color = ImmersiveTextPrimary.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enrolled in Beta", color = ImmersiveTextPrimary, fontSize = 13.sp)
                Switch(
                    checked = betaEnrolled,
                    onCheckedChange = onEnrolledChange
                )
            }
        }
    }
}
