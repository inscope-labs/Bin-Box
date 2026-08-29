package com.inscopelabs.abx.binbox.oci.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Modular card UI rendering a single unredacted OCI API call trace entry with expandable inspection.
 */
@Composable
fun OciCallTraceCard(
    entry: OciCallTraceEntry,
    index: Int,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val isError = (entry.httpStatusCode ?: 0) !in 200..299 || entry.exceptionClass != null

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = ImmersiveSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isError) ImmersiveStatusRed.copy(alpha = 0.6f) else ImmersiveBorderVerySubtle
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("oci_trace_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MethodBadge(entry.method)
                    StatusBadge(entry.httpStatusCode, entry.exceptionClass != null)
                    DurationBadge(entry.durationMs)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.stageId} / ${entry.stepId}",
                        color = ImmersiveTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = ImmersiveTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = entry.url,
                color = ImmersiveTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = if (expanded) Int.MAX_VALUE else 1
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = ImmersiveBorderVerySubtle, thickness = 1.dp)
                    Spacer(Modifier.height(8.dp))

                    Text("Timestamp (UTC): ${entry.timestampUtc}", color = ImmersiveTextMuted, fontSize = 10.sp)
                    Text("Trace ID: ${entry.id}", color = ImmersiveTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                    if (entry.ociErrorCode != null || entry.ociErrorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        OciErrorSection(entry.ociErrorCode, entry.ociErrorMessage)
                    }

                    if (entry.exceptionClass != null || entry.exceptionMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        ExceptionSection(entry.exceptionClass, entry.exceptionMessage)
                    }

                    Spacer(Modifier.height(8.dp))
                    HeadersSection(title = "Request Headers", headers = entry.requestHeaders, onCopy = {
                        clipboard.setText(AnnotatedString(entry.requestHeaders.entries.joinToString("\n") { "${it.key}: ${it.value}" }))
                    })

                    entry.requestBody?.let { body ->
                        Spacer(Modifier.height(8.dp))
                        BodySection(title = "Request Body", body = body, onCopy = { clipboard.setText(AnnotatedString(body)) })
                    }

                    entry.responseHeaders?.let { headers ->
                        Spacer(Modifier.height(8.dp))
                        HeadersSection(title = "Response Headers", headers = headers, onCopy = {
                            clipboard.setText(AnnotatedString(headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }))
                        })
                    }

                    entry.responseBody?.let { body ->
                        Spacer(Modifier.height(8.dp))
                        BodySection(title = "Response Body", body = body, onCopy = { clipboard.setText(AnnotatedString(body)) })
                    }
                }
            }
        }
    }
}
