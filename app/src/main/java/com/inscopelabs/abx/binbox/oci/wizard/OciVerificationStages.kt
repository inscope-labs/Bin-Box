package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ConnectionVerificationStage(
    isVerifying: Boolean,
    isDiscovering: Boolean,
    error: String?,
    diagnostics: OciVerificationDiagnostics?,
    onVerify: () -> Unit,
    onContinue: () -> Unit,
    onEditAccountInfo: () -> Unit,
    onStartOver: () -> Unit
) {
    var attempted by remember { mutableStateOf(false) }
    val verified = attempted && !isVerifying && error == null

    StageHeader("Verify the connection", "A single harmless authenticated request — confirms signing, credentials, and region before any infrastructure is created.")

    when {
        isVerifying -> LoadingRow("Connecting to Oracle Cloud Identity API…")
        isDiscovering -> LoadingRow("Loading your OCI environment…")
        error != null -> {
            ErrorBanner(error)

            if (diagnostics != null) {
                Spacer(Modifier.height(14.dp))
                OciDiagnosticsCard(diagnostics)
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "Retry connection",
                icon = Icons.Default.Refresh,
                onClick = { attempted = true; onVerify() },
                modifier = Modifier.testTag("oci_retry_verify")
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = "Edit Account Info & Region",
                icon = Icons.Default.Edit,
                onClick = onEditAccountInfo,
                modifier = Modifier.testTag("oci_edit_account_info")
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = "Start Over",
                icon = Icons.Default.RestartAlt,
                onClick = onStartOver,
                modifier = Modifier.testTag("oci_start_over")
            )
        }
        verified -> {
            InfoCard(
                icon = Icons.Default.CheckCircle,
                title = "Connection verified",
                body = "Signing and account credentials both authenticated successfully with Oracle Cloud."
            )
            if (diagnostics != null) {
                Spacer(Modifier.height(12.dp))
                OciDiagnosticsCard(diagnostics)
            }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.testTag("oci_continue_after_verify"))
        }
        else -> {
            if (diagnostics != null) {
                OciDiagnosticsCard(diagnostics)
                Spacer(Modifier.height(16.dp))
            }
            PrimaryButton(
                text = "Verify connection",
                onClick = { attempted = true; onVerify() },
                modifier = Modifier.testTag("oci_verify_connection")
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = "Edit Account Info",
                icon = Icons.Default.Edit,
                onClick = onEditAccountInfo
            )
        }
    }
}

@Composable
fun ContextDiscoveryStage(
    isDiscovering: Boolean,
    error: String?,
    diagnostics: OciVerificationDiagnostics? = null,
    onDiscover: () -> Unit
) {
    LaunchedEffect(Unit) { onDiscover() }

    StageHeader("Discovering your environment", "Loading compartments and availability domains from your tenancy.")

    when {
        isDiscovering -> LoadingRow("Fetching compartments and availability domains…")
        error != null -> {
            ErrorBanner(error)
            if (diagnostics != null) {
                Spacer(Modifier.height(14.dp))
                OciDiagnosticsCard(diagnostics)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Retry", onClick = onDiscover, modifier = Modifier.testTag("oci_retry_discovery"))
        }
        else -> LoadingRow("Almost there…")
    }
}
