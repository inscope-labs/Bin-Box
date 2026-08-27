package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

private val ORDERED_STAGES = OciOnboardingStage.values().toList()

@Composable
fun WizardProgressBar(stage: OciOnboardingStage) {
    val index = ORDERED_STAGES.indexOf(stage).coerceAtLeast(0)
    val progress = (index + 1).toFloat() / ORDERED_STAGES.size
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "oci_wizard_progress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(ImmersiveComponent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(ImmersivePrimary)
        )
    }
}

@Composable
fun StageHeader(title: String, subtitle: String) {
    Text(title, color = ImmersiveTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, color = ImmersiveTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
    Spacer(Modifier.height(20.dp))
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    monospace: Boolean = false,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = ImmersiveTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            trailingAction?.invoke()
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = ImmersiveTextMuted, fontSize = 13.sp) },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 13.sp,
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = ImmersiveTextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ImmersiveSurface,
                unfocusedContainerColor = ImmersiveSurface,
                focusedBorderColor = ImmersivePrimary,
                unfocusedBorderColor = ImmersiveBorderSubtle,
                cursorColor = ImmersivePrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ImmersivePrimary,
            contentColor = ImmersiveOnPrimary,
            disabledContainerColor = ImmersiveComponent,
            disabledContentColor = ImmersiveTextMuted
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ImmersiveComponent.copy(alpha = 0.5f),
            contentColor = ImmersiveTextPrimary
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ImmersiveSurface)
            .border(1.dp, ImmersiveBorderVerySubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = ImmersiveTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            Text(body, color = ImmersiveTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveStatusRed.copy(alpha = 0.12f))
            .border(1.dp, ImmersiveStatusRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = ImmersiveStatusRed, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = ImmersiveTextPrimary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
fun LoadingRow(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ImmersivePrimary)
        Spacer(Modifier.width(12.dp))
        Text(message, color = ImmersiveTextSecondary, fontSize = 13.sp)
    }
}

@Composable
fun CopyableCodeBlock(text: String, label: String = "Public key") {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveTerminalCardBg)
            .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = ImmersiveTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            IconButton(
                onClick = { clipboard.setText(AnnotatedString(text)) },
                modifier = Modifier.size(28.dp).testTag("oci_copy_public_key")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ImmersivePrimary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text,
            color = ImmersiveTextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )
    }
}

@Composable
fun DiagnosticsCard(diagnostics: OciVerificationDiagnostics) {
    var expanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ImmersiveSurface)
            .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connection Diagnostics", color = ImmersiveTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(diagnostics.toFormattedReport()))
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy Report", color = CyanAccent, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Endpoint: ${diagnostics.endpointUrl}", color = ImmersiveTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("Region: ${diagnostics.region} (${OciRegionHelper.getDisplayName(diagnostics.region)})", color = ImmersiveTextSecondary, fontSize = 11.sp)
        if (diagnostics.httpStatusCode != null) {
            Text("HTTP Status: ${diagnostics.httpStatusCode}", color = if (diagnostics.httpStatusCode in 200..299) ImmersiveStatusGreen else ImmersiveStatusRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            if (expanded) "▲ Hide technical details" else "▼ Show technical details & troubleshooting",
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

                if (diagnostics.rawExceptionMessage != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("Error details: ${diagnostics.rawExceptionMessage}", color = ImmersiveStatusRed, fontSize = 11.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text("Troubleshooting suggestions:", color = ImmersiveTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                diagnostics.getTroubleshootingSuggestions().forEach { suggestion ->
                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = ImmersivePrimary, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(suggestion, color = ImmersiveTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SelectionSection(
    title: String,
    items: List<T>,
    selectedId: String?,
    idOf: (T) -> String,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    Text(title, color = ImmersiveTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    Spacer(Modifier.height(6.dp))
    if (items.isEmpty()) {
        Text("None available yet.", color = ImmersiveTextMuted, fontSize = 12.sp)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ImmersiveSurface)
            .border(1.dp, ImmersiveBorderVerySubtle, RoundedCornerShape(10.dp))
    ) {
        items.forEachIndexed { index, item ->
            val selected = idOf(item) == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item) }
                    .background(if (selected) ImmersiveComponent else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    labelOf(item),
                    color = if (selected) ImmersivePrimary else ImmersiveTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                }
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = ImmersiveBorderVerySubtle, thickness = 1.dp)
            }
        }
    }
}
