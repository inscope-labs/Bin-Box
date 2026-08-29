package com.inscopelabs.abx.binbox.oci.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun MethodBadge(method: String) {
    val (bg, fg) = when (method.uppercase()) {
        "GET" -> CyanAccent.copy(alpha = 0.15f) to CyanAccent
        "POST" -> ImmersiveStatusGreen.copy(alpha = 0.15f) to ImmersiveStatusGreen
        "PUT" -> ImmersivePrimary.copy(alpha = 0.15f) to ImmersivePrimary
        "DELETE" -> ImmersiveStatusRed.copy(alpha = 0.15f) to ImmersiveStatusRed
        else -> Slate700 to ImmersiveTextPrimary
    }
    TraceBadge(text = method.uppercase(), bg = bg, fg = fg)
}

@Composable
fun StatusBadge(statusCode: Int?, hasException: Boolean) {
    val (bg, fg, label) = when {
        hasException -> Triple(ImmersiveStatusRed.copy(alpha = 0.2f), ImmersiveStatusRed, "ERR")
        statusCode == null -> Triple(Slate700, ImmersiveTextMuted, "---")
        statusCode in 200..299 -> Triple(ImmersiveStatusGreen.copy(alpha = 0.2f), ImmersiveStatusGreen, "$statusCode OK")
        else -> Triple(ImmersiveStatusRed.copy(alpha = 0.2f), ImmersiveStatusRed, "$statusCode")
    }
    TraceBadge(text = label, bg = bg, fg = fg)
}

@Composable
fun DurationBadge(durationMs: Long) {
    TraceBadge(text = "${durationMs}ms", bg = ImmersiveComponent, fg = ImmersiveTextSecondary)
}

@Composable
fun TraceBadge(text: String, bg: Color, fg: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun OciErrorSection(code: String?, message: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ImmersiveStatusRed.copy(alpha = 0.1f))
            .border(1.dp, ImmersiveStatusRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text("OCI API Error: ${code ?: "Unknown"}", color = ImmersiveStatusRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        if (message != null) {
            Text(message, color = ImmersiveTextPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ExceptionSection(cls: String?, message: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ImmersiveStatusRed.copy(alpha = 0.1f))
            .border(1.dp, ImmersiveStatusRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text("Exception: ${cls ?: "Throwable"}", color = ImmersiveStatusRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        if (message != null) {
            Text(message, color = ImmersiveTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun HeadersSection(title: String, headers: Map<String, String>, onCopy: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = ImmersiveTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCopy, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy headers", tint = CyanAccent, modifier = Modifier.size(12.dp))
            }
        }
        headers.forEach { (k, v) ->
            Text("$k: $v", color = ImmersiveTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BodySection(title: String, body: String, onCopy: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = ImmersiveTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onCopy, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy body", tint = CyanAccent, modifier = Modifier.size(12.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(ImmersiveBg)
                .padding(8.dp)
        ) {
            Text(
                text = body,
                color = ImmersiveTextPrimary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 15
            )
        }
    }
}
