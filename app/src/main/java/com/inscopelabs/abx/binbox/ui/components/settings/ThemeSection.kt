package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.ui.i18n.AppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Theme picker with a mini live terminal preview. Module component: renders
 * the current [currentTheme] preview and the [allThemes] chip row, reports
 * selection to [viewModel]; holds no state of its own.
 */
@Composable
fun ThemeSection(
    strings: AppStrings,
    currentTheme: TerminalThemePreset,
    allThemes: List<TerminalThemePreset>,
    viewModel: BinBoxViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.themeSectionTitle,
                color = ImmersiveTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.themeSectionSubtitle,
                color = ImmersiveTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Terminal Live Preview
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = currentTheme.backgroundColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ImmersiveStatusRed))
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ImmersiveStatusAmber))
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(ImmersiveStatusGreen))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${currentTheme.name} preview",
                            color = ImmersiveTextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "root@binbox:~# neofetch",
                        color = currentTheme.green,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OS: Linux 6.8.0-abx-binbox aarch64",
                        color = currentTheme.cyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Shell: binbox-sh 2.4.0 (xterm-256color)",
                        color = currentTheme.yellow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Memory: 1,420MiB / 8,192MiB",
                        color = currentTheme.foregroundColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        listOf(
                            currentTheme.black,
                            currentTheme.red,
                            currentTheme.green,
                            currentTheme.yellow,
                            currentTheme.blue,
                            currentTheme.magenta,
                            currentTheme.cyan,
                            currentTheme.white
                        ).forEach { c ->
                            Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(c))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Theme Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allThemes) { theme ->
                    val isSelected = theme.id == currentTheme.id
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = theme.backgroundColor,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) ImmersivePrimary else ImmersiveBorderVerySubtle
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setTheme(theme) }
                            .width(110.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = theme.name,
                                color = theme.foregroundColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.cyan))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.green))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.yellow))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.red))
                            }
                        }
                    }
                }
            }
        }
    }
}
