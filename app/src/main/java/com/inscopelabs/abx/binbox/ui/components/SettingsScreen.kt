package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.core.diagnostics.CrashReporterManager
import com.inscopelabs.abx.binbox.core.diagnostics.DiagnosticPreferences
import com.inscopelabs.abx.binbox.core.featureflags.BetaEnrollment
import com.inscopelabs.abx.binbox.core.featureflags.Feature
import com.inscopelabs.abx.binbox.core.featureflags.FeatureFlags
import com.inscopelabs.abx.binbox.oci.wizard.LocalOciWizardLauncher
import com.inscopelabs.abx.binbox.terminal.model.TerminalThemes
import com.inscopelabs.abx.binbox.ui.components.settings.BetaTestingSection
import com.inscopelabs.abx.binbox.ui.components.settings.CursorStyleSection
import com.inscopelabs.abx.binbox.ui.components.settings.DiagnosticsSection
import com.inscopelabs.abx.binbox.ui.components.settings.HapticSection
import com.inscopelabs.abx.binbox.ui.components.settings.LanguageSection
import com.inscopelabs.abx.binbox.ui.components.settings.OciCloudSection
import com.inscopelabs.abx.binbox.ui.components.settings.SettingsHeaderSection
import com.inscopelabs.abx.binbox.ui.components.settings.SystemTelemetrySection
import com.inscopelabs.abx.binbox.ui.components.settings.ThemeSection
import com.inscopelabs.abx.binbox.ui.components.settings.TypographySection
import com.inscopelabs.abx.binbox.ui.i18n.LocalAppStrings
import com.inscopelabs.abx.binbox.ui.theme.Slate950
import com.inscopelabs.abx.binbox.ui.viewmodel.BinBoxViewModel

/**
 * Orchestrator: assembles the Settings screen from its Module sections
 * (ui/components/settings/*) and owns the state each section needs, per
 * AGENTS.md Section 4 (Orchestrator/Module role split). No business logic
 * lives here — every section below is display + event delegation only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val ociLauncher = LocalOciWizardLauncher.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val fontSizeSp by viewModel.fontSizeSp.collectAsStateWithLifecycle()
    val cursorStyle by viewModel.cursorStyle.collectAsStateWithLifecycle()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    var betaEnrolled by remember { mutableStateOf(BetaEnrollment.isEnrolled(context)) }
    var remoteReportingEnabled by remember { mutableStateOf(DiagnosticPreferences.isRemoteReportingEnabled(context)) }

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
            item { SettingsHeaderSection(strings = strings, viewModel = viewModel) }

            item { LanguageSection(strings = strings, appLanguage = appLanguage, viewModel = viewModel) }

            item {
                ThemeSection(
                    strings = strings,
                    currentTheme = currentTheme,
                    allThemes = allThemes,
                    viewModel = viewModel
                )
            }

            item { TypographySection(strings = strings, fontSizeSp = fontSizeSp, viewModel = viewModel) }

            item { CursorStyleSection(strings = strings, cursorStyle = cursorStyle, viewModel = viewModel) }

            item { HapticSection(strings = strings, hapticFeedbackEnabled = hapticFeedbackEnabled, viewModel = viewModel) }

            // NOTE: not yet gated behind Feature.OCI_EXTENDED_SHELL_HOST — open
            // follow-up, not addressed by this restructuring task (must not
            // change external behavior per AGENTS.md 4.2).
            item { OciCloudSection(onLaunchWizard = { ociLauncher() }) }

            item {
                BetaTestingSection(
                    betaEnrolled = betaEnrolled,
                    onEnrolledChange = { enabled ->
                        betaEnrolled = enabled
                        BetaEnrollment.setEnrolled(context, enabled)
                    }
                )
            }

            if (FeatureFlags.isEnabled(Feature.DIAGNOSTICS_INSPECTOR, betaEnrolled)) {
                item {
                    DiagnosticsSection(
                        remoteReportingEnabled = remoteReportingEnabled,
                        onReportingEnabledChange = { enabled ->
                            remoteReportingEnabled = enabled
                            CrashReporterManager.updateReportingPreference(context, enabled)
                        }
                    )
                }
            }

            item { SystemTelemetrySection(strings = strings) }

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
        Text(label, color = com.inscopelabs.abx.binbox.ui.theme.ImmersiveTextSecondary, fontSize = 12.sp)
        Text(
            value,
            color = com.inscopelabs.abx.binbox.ui.theme.ImmersiveTextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
