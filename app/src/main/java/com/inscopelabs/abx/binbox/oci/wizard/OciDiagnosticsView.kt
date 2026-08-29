package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.oci.diagnostics.OciCallTraceViewerDialog
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Modular diagnostics and troubleshooting UI for OCI wizard API errors.
 */
@Composable
fun OciDiagnosticsCard(
    diagnostics: OciVerificationDiagnostics,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showTraceViewer by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ImmersiveSurface)
            .border(1.dp, if (diagnostics.isSignatureFailure) ImmersiveStatusRed.copy(alpha = 0.5f) else ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (diagnostics.isSignatureFailure) Icons.Default.KeyOff else Icons.Default.BugReport,
                    contentDescription = null,
                    tint = if (diagnostics.isSignatureFailure) ImmersiveStatusRed else CyanAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (diagnostics.isSignatureFailure) "Signature Verification Failed" else "Connection Diagnostics",
                    color = ImmersiveTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showTraceViewer = true },
                    modifier = Modifier.testTag("oci_view_raw_traces_button")
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("API Traces", color = CyanAccent, fontSize = 12.sp)
                }
                TextButton(
                    onClick = { clipboard.setText(AnnotatedString(diagnostics.toFormattedReport())) },
                    modifier = Modifier.testTag("oci_copy_diagnostics_report")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy Report", color = CyanAccent, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Endpoint: ${diagnostics.httpMethod} ${diagnostics.endpointUrl}", color = ImmersiveTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("Region: ${diagnostics.region} (${OciRegionHelper.getDisplayName(diagnostics.region)})", color = ImmersiveTextSecondary, fontSize = 11.sp)
        if (diagnostics.httpStatusCode != null) {
            Text(
                "HTTP Status: ${diagnostics.httpStatusCode} (${diagnostics.ociErrorCode ?: "Error"})",
                color = if (diagnostics.httpStatusCode in 200..299) ImmersiveStatusGreen else ImmersiveStatusRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (diagnostics.opcRequestId != null) {
            Text("OPC Request ID: ${diagnostics.opcRequestId}", color = ImmersiveTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (expanded) "▲ Hide technical details & key info" else "▼ Show technical details & troubleshooting",
            color = ImmersivePrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 4.dp)
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                HorizontalDivider(color = ImmersiveBorderVerySubtle, thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                Text("Tenancy: ${diagnostics.tenancyOcid}", color = ImmersiveTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("User: ${diagnostics.userOcid}", color = ImmersiveTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Fingerprint: ${diagnostics.fingerprint}", color = ImmersiveTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("KeyID: ${diagnostics.keyId}", color = ImmersiveTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                if (diagnostics.publicKeyPem != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Public Key (PEM)", color = ImmersiveTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { clipboard.setText(AnnotatedString(diagnostics.publicKeyPem)) },
                            modifier = Modifier.height(28.dp).testTag("oci_copy_public_key_from_diagnostics")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Key", color = ImmersivePrimary, fontSize = 11.sp)
                        }
                    }
                    Text(
                        diagnostics.publicKeyPem.take(120) + "…",
                        color = ImmersiveTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 3
                    )
                }

                if (diagnostics.ociErrorMessage != null || diagnostics.rawExceptionMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Error details: ${diagnostics.ociErrorMessage ?: diagnostics.rawExceptionMessage}",
                        color = ImmersiveStatusRed,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text("Troubleshooting suggestions:", color = ImmersiveTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                diagnostics.getTroubleshootingSuggestions().forEach { suggestion ->
                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = if (diagnostics.isSignatureFailure) ImmersiveStatusRed else ImmersivePrimary, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(suggestion, color = ImmersiveTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }

    if (showTraceViewer) {
        OciCallTraceViewerDialog(onDismiss = { showTraceViewer = false })
    }
}

