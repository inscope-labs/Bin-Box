package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Font size slider. Module component: renders only, reports changes to
 * [viewModel]; holds no state of its own.
 */
@Composable
fun TypographySection(
    strings: AppStrings,
    fontSizeSp: Int,
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
                Text(strings.fontSizeTitle, color = ImmersiveTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${fontSizeSp}sp", color = ImmersivePrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Slider(
                value = fontSizeSp.toFloat(),
                onValueChange = { viewModel.setFontSize(it.toInt()) },
                valueRange = 9f..22f,
                steps = 12,
                colors = SliderDefaults.colors(
                    thumbColor = ImmersivePrimary,
                    activeTrackColor = ImmersivePrimary,
                    inactiveTrackColor = ImmersiveComponent
                )
            )
        }
    }
}
