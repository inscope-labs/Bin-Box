package com.inscopelabs.abx.binbox.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.ui.theme.CyanAccent
import com.inscopelabs.abx.binbox.ui.theme.Slate700
import com.inscopelabs.abx.binbox.ui.theme.Slate800

@Composable
fun BetaEnrollmentCard(
    isBetaActive: Boolean,
    onToggleBeta: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("beta_enrollment_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Beta Testing",
                    tint = CyanAccent
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BinBox Early Access & Beta",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isBetaActive) "Beta testing mode active" else "Production v0.1.0 mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBetaActive) CyanAccent else Slate700
                    )
                }
                Switch(
                    checked = isBetaActive,
                    onCheckedChange = onToggleBeta,
                    modifier = Modifier.testTag("beta_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Beta testers get private early access to OCI cloud provisioning, extended toolchain tiers, and remote server bridges. Feedback is submitted privately to the developer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        BinBoxLogger.i("BetaEnrollmentCard", "Opening Play Store beta enrollment link")
                        val betaUrl = "https://play.google.com/apps/testing/${context.packageName}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(betaUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            BinBoxLogger.w("BetaEnrollmentCard", "Failed to open Play Store intent", e)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("join_beta_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Play Store Beta", color = Slate800)
                }

                if (isBetaActive) {
                    OutlinedButton(
                        onClick = {
                            BinBoxLogger.i("BetaEnrollmentCard", "Opening Beta feedback intent")
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:afterdawn.service@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "[BinBox Beta Feedback] v0.1.0")
                            }
                            try {
                                context.startActivity(emailIntent)
                            } catch (e: Exception) {
                                BinBoxLogger.w("BetaEnrollmentCard", "Failed to open email client", e)
                            }
                        },
                        modifier = Modifier.testTag("send_feedback_button")
                    ) {
                        Text("Send Feedback")
                    }
                }
            }
        }
    }
}
