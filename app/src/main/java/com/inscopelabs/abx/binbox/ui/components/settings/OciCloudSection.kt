package com.inscopelabs.abx.binbox.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.oci.wizard.OciFreeTierPromoCard
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Cloud Infrastructure & Hosting (OCI) promo entry point. Module component:
 * launches the wizard via [onLaunchWizard], holds no state of its own.
 *
 * NOTE: not yet gated behind Feature.OCI_EXTENDED_SHELL_HOST — that gating
 * is still an open follow-up (see agent-reports), not addressed by this
 * restructuring task per AGENTS.md 4.2 (must not change external behavior).
 */
@Composable
fun OciCloudSection(onLaunchWizard: () -> Unit) {
    Column {
        Text(
            text = "Cloud Infrastructure & Hosting",
            color = ImmersiveTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OciFreeTierPromoCard(
            onLaunchWizard = { ociLauncher() }
        )
    }
}
