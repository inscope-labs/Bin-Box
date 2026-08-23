package com.inscopelabs.abx.binbox.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.terminal.model.CursorStyle
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemePreset
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.ui.i18n.AppLanguage
import com.inscopelabs.abx.binbox.ui.i18n.LocalAppStrings
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val fontSizeSp by viewModel.fontSizeSp.collectAsStateWithLifecycle()
    val cursorStyle by viewModel.cursorStyle.collectAsStateWithLifecycle()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()

    val allThemes = listOf(
        TerminalThemes.MonokaiPro,
        TerminalThemes.Dracula,
        TerminalThemes.Nord,
        TerminalThemes.Cyberpunk,
        TerminalThemes.MatrixGreen,
        TerminalThemes.AmberCrt,
        TerminalThemes.SolarizedDark,
        TerminalThemes.OneDark,
        TerminalThemes.TokyoNight,
        TerminalThemes.GruvboxDark
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
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

            // ----------------------------------------------------
            // 1. Language & Localization Picker
            // ----------------------------------------------------
            item {
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
                                Icon(Icons.Default.Language, null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(
                                    text = strings.languageSectionTitle,
                                    color = ImmersiveTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.languageSectionSubtitle,
                                    color = ImmersiveTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid of Language Selection Cards
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.chunked(2).forEach { rowLanguages ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowLanguages.forEach { lang ->
                                        val isSelected = lang == appLanguage
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) ImmersivePrimary.copy(alpha = 0.15f) else ImmersiveComponent,
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) ImmersivePrimary else ImmersiveBorderVerySubtle
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.setLanguage(lang) }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(text = lang.flagEmoji, fontSize = 18.sp)
                                                    Column {
                                                        Text(
                                                            text = lang.nativeName,
                                                            color = if (isSelected) ImmersivePrimary else ImmersiveTextPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                        if (lang.displayName != lang.nativeName && lang != AppLanguage.SYSTEM) {
                                                            Text(
                                                                text = lang.displayName,
                                                                color = ImmersiveTextMuted,
                                                                fontSize = 10.sp
                                                            )
                                                        }
                                                    }
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = ImmersivePrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (rowLanguages.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. Theme Picker & Mini Live Preview
            // ----------------------------------------------------
            item {
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

            // ----------------------------------------------------
            // 3. Typography & Font Size Slider
            // ----------------------------------------------------
            item {
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

            // ----------------------------------------------------
            // 4. Cursor Style Picker
            // ----------------------------------------------------
            item {
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

            // ----------------------------------------------------
            // 5. Haptic & Interaction Feedback
            // ----------------------------------------------------
            item {
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

            // ----------------------------------------------------
            // 6. Local System & Shell Engine Telemetry
            // ----------------------------------------------------
            item {
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

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ImmersiveTextSecondary, fontSize = 12.sp)
        Text(
            value,
            color = ImmersiveTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
