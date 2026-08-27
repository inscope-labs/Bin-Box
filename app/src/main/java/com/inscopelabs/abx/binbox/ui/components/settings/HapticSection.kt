package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Haptic feedback toggle. Module component: renders only, reports changes to
 * [viewModel]; holds no state of its own.
 */
@Composable
fun HapticSection(
    strings: AppStrings,
    hapticFeedbackEnabled: Boolean,
    viewModel: BinBoxViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.hapticTitle, color = ImmersiveTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(strings.hapticSubtitle, color = ImmersiveTextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.toggleHapticFeedback(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ImmersiveOnPrimary,
                        checkedTrackColor = ImmersivePrimary,
                        uncheckedThumbColor = ImmersiveTextMuted,
                        uncheckedTrackColor = ImmersiveComponent
                    )
                )
            }
        }
    }
}
