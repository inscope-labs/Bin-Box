package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Cursor style picker. Module component: renders only, reports selection to
 * [viewModel]; holds no state of its own.
 */
@Composable
fun CursorStyleSection(
    strings: AppStrings,
    cursorStyle: CursorStyle,
    viewModel: BinBoxViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(strings.cursorStyleTitle, color = ImmersiveTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    CursorStyle.BLOCK to "Block █",
                    CursorStyle.BLINKING_BLOCK to "Blink █",
                    CursorStyle.UNDERLINE to "Line _",
                    CursorStyle.BAR to "Bar |"
                ).forEach { (style, label) ->
                    val isSelected = cursorStyle == style
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setCursorStyle(style) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
