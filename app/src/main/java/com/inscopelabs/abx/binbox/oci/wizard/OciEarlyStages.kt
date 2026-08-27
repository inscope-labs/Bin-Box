package com.inscopelabs.abx.binbox.oci.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun WelcomeStage(onGetStarted: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(24.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ImmersivePrimary)
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = ImmersiveOnPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Provision a free Oracle Cloud VM",
            color = ImmersiveTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "This walks through creating a VM on Oracle's Always Free tier and adding it straight to Bin Box as a host — no separate SSH setup needed.",
            color = ImmersiveTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        InfoCard(
            icon = Icons.Default.Info,
            title = "Assisted setup available",
            body = "You can enter your OCIDs manually or paste your OCI configuration snippet directly from the Oracle Cloud console."
        )
        Spacer(Modifier.height(12.dp))
        InfoCard(
            icon = Icons.Default.Lock,
            title = "Nothing leaves this device unnecessarily",
            body = "The API signing key is generated on-device and stored in the Android Keystore. Only its public half is ever sent to Oracle."
        )
        Spacer(Modifier.weight(1f, fill = false))
        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Get started", onClick = onGetStarted, modifier = Modifier.testTag("oci_get_started"))
    }
}

@Composable
fun AccountInfoStage(
    tenancyOcidPrefill: String?,
    userOcidPrefill: String?,
    regionPrefill: String?,
    error: String?,
    onImportConfig: (String) -> Unit,
    onSubmit: (tenancyOcid: String, userOcid: String, region: String) -> Unit
) {
    var tenancyOcid by remember(tenancyOcidPrefill) { mutableStateOf(tenancyOcidPrefill.orEmpty()) }
    var userOcid by remember(userOcidPrefill) { mutableStateOf(userOcidPrefill.orEmpty()) }
    var region by remember(regionPrefill) { mutableStateOf(regionPrefill.orEmpty()) }
    var showImportDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    StageHeader("Account information", "Find these in the OCI console under Profile menu → Tenancy and user details, or paste your ~/.oci/config snippet.")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Paste OCI Config Snippet", color = CyanAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Button(
            onClick = {
                val clipText = clipboard.getText()?.text
                if (!clipText.isNullOrBlank() && (clipText.contains("ocid1.") || clipText.contains("user=") || clipText.contains("fingerprint="))) {
                    onImportConfig(clipText)
                } else {
                    showImportDialog = true
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveComponent),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Paste / Import", color = ImmersiveTextPrimary, fontSize = 12.sp)
        }
    }

    Spacer(Modifier.height(14.dp))

    LabeledField(
        label = "Tenancy OCID",
        value = tenancyOcid,
        onValueChange = { input ->
            if (input.contains("\n") || input.contains("user=")) {
                onImportConfig(input)
            } else {
                tenancyOcid = input
            }
        },
        placeholder = "ocid1.tenancy.oc1..aaaa..."
    )
    Spacer(Modifier.height(12.dp))

    LabeledField(
        label = "User OCID",
        value = userOcid,
        onValueChange = { input ->
            if (input.contains("\n") || input.contains("tenancy=")) {
                onImportConfig(input)
            } else {
                userOcid = input
            }
        },
        placeholder = "ocid1.user.oc1..aaaa..."
    )
    Spacer(Modifier.height(12.dp))

    LabeledField(
        label = "Home region",
        value = region,
        onValueChange = { region = it },
        placeholder = "e.g. sa-saopaulo-1 or us-ashburn-1"
    )

    Spacer(Modifier.height(8.dp))
    Text("Popular regions:", color = ImmersiveTextMuted, fontSize = 11.sp)
    Spacer(Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(listOf("sa-saopaulo-1", "us-ashburn-1", "us-phoenix-1", "eu-frankfurt-1", "uk-london-1", "ap-tokyo-1")) { reg ->
            Surface(
                color = if (region == reg) ImmersivePrimary.copy(alpha = 0.2f) else ImmersiveSurface,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (region == reg) ImmersivePrimary else ImmersiveBorderVerySubtle),
                modifier = Modifier.clickable { region = reg }
            ) {
                Text(
                    reg,
                    color = if (region == reg) ImmersivePrimary else ImmersiveTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    error?.let { ErrorBanner(it) }

    Spacer(Modifier.height(24.dp))
    val valid = tenancyOcid.isNotBlank() && userOcid.isNotBlank() && region.isNotBlank()
    PrimaryButton(
        text = "Continue",
        enabled = valid,
        onClick = { onSubmit(tenancyOcid.trim(), userOcid.trim(), region.trim()) },
        modifier = Modifier.testTag("oci_submit_account_info")
    )

    if (showImportDialog) {
        var importInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Paste OCI Configuration Snippet", color = ImmersiveTextPrimary, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        "Paste the snippet provided by OCI Console (e.g. ~/.oci/config preview or details containing your OCIDs):",
                        color = ImmersiveTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInput,
                        onValueChange = { importInput = it },
                        placeholder = { Text("[DEFAULT]\nuser=ocid1.user...\ntenancy=ocid1.tenancy...\nregion=sa-saopaulo-1", color = ImmersiveTextMuted, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importInput.isNotBlank()) onImportConfig(importInput)
                    showImportDialog = false
                }) {
                    Text("Apply", color = CyanAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = ImmersiveTextSecondary) }
            },
            containerColor = ImmersiveSurface
        )
    }
}

@Composable
fun ApiKeyGenerationStage(
    publicKeyPem: String?,
    error: String?,
    onGenerate: () -> Unit
) {
    StageHeader("Generate an API signing key", "This key pair is created on-device and the private half never leaves the Android Keystore.")

    if (publicKeyPem == null) {
        InfoCard(
            icon = Icons.Default.VpnKey,
            title = "Ready to generate",
            body = "Tapping below creates a 2048-bit RSA key pair backed by Android's hardware keystore where available."
        )
        error?.let { ErrorBanner(it) }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Generate key", onClick = onGenerate, modifier = Modifier.testTag("oci_generate_api_key"))
    } else {
        Text("Public key generated", color = ImmersiveStatusGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        CopyableCodeBlock(publicKeyPem)
        Spacer(Modifier.height(16.dp))
        Text(
            "This moves you to the next step automatically. Copy the key above before continuing — you'll paste it into the OCI console there.",
            color = ImmersiveTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ApiKeyRegistrationStage(
    publicKeyPem: String?,
    pendingFingerprint: String?,
    error: String?,
    onSubmit: (String) -> Unit
) {
    var fingerprint by remember(pendingFingerprint) { mutableStateOf(pendingFingerprint.orEmpty()) }

    StageHeader("Register the key with Oracle", "In the OCI console: Profile → My profile → API keys → Add API key → Paste public key, then paste the public key below.")

    publicKeyPem?.let {
        CopyableCodeBlock(it)
        Spacer(Modifier.height(16.dp))
    }

    Text(
        "Once added, Oracle shows a fingerprint like aa:bb:cc:...:zz. Paste it here.",
        color = ImmersiveTextSecondary,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(8.dp))
    LabeledField(
        "Fingerprint",
        fingerprint,
        { fingerprint = it },
        placeholder = "aa:bb:cc:dd:ee:ff:...:zz",
        monospace = true
    )

    error?.let { ErrorBanner(it) }

    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = "Continue",
        enabled = fingerprint.isNotBlank(),
        onClick = { onSubmit(fingerprint.trim()) },
        modifier = Modifier.testTag("oci_submit_fingerprint")
    )
}
