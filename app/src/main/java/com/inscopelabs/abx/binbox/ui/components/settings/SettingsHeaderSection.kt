package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Settings screen title, subtitle, and the "reset to defaults" action.
 * Module component: renders only, all mutation delegated to [viewModel].
 */
@Composable
fun SettingsHeaderSection(
    strings: AppStrings,
    viewModel: BinBoxViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.settingsTitle,
                color = ImmersiveTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.settingsSubtitle,
                color = ImmersiveTextSecondary,
                fontSize = 12.sp
            )
        }

        FilledTonalButton(
            onClick = { viewModel.resetPreferences() },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = ImmersiveComponent,
                contentColor = ImmersiveTextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = strings.resetToDefaults, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(strings.resetToDefaults, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
