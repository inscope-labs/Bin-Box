package com.inscopelabs.abx.binbox.ui.components.hosts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHostDialog(
    initialHost: HostEntity?,
    savedKeys: List<KeyEntity>,
    onDismiss: () -> Unit,
    onSave: (HostEntity) -> Unit,
    onLaunchOciWizard: (() -> Unit)? = null
) {
    var label by remember { mutableStateOf(initialHost?.label ?: "") }
    var host by remember { mutableStateOf(initialHost?.host ?: "") }
    var portText by remember { mutableStateOf((initialHost?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(initialHost?.username ?: "root") }
    var protocol by remember { mutableStateOf(initialHost?.protocol ?: "SSH") }
    var authType by remember { mutableStateOf(initialHost?.authType ?: "PASSWORD") }
    var password by remember { mutableStateOf(initialHost?.password ?: "") }
    var selectedKeyId by remember { mutableStateOf<Long?>(initialHost?.keyId) }
    var groupTag by remember { mutableStateOf(initialHost?.groupTag ?: "Cloud") }
    var themeId by remember { mutableStateOf(initialHost?.themeId ?: "monokai_pro") }
    var shellProfileId by remember { mutableStateOf(initialHost?.shellProfileId ?: "default") }
    var initialDirectory by remember { mutableStateOf(initialHost?.initialDirectory ?: "") }
    var envVars by remember { mutableStateOf(initialHost?.envVarsJson ?: "") }
    var startupCommand by remember { mutableStateOf(initialHost?.startupCommand ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialHost == null) "Add Host Shell" else "Edit Host Shell",
                color = ImmersiveTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Oracle Cloud Quick Action (when creating new host)
                if (initialHost == null && onLaunchOciWizard != null) {
                    item {
                        com.inscopelabs.abx.binbox.oci.wizard.OciQuickActionTile(
                            onLaunchWizard = onLaunchOciWizard
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = ImmersiveBorderVerySubtle)
                            Text("OR MANUAL SETUP", color = ImmersiveTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            HorizontalDivider(modifier = Modifier.weight(1f), color = ImmersiveBorderVerySubtle)
                        }
                    }
                }

                // Protocol Selector
                item {
                    Text("Protocol", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SSH", "LOCAL_SHELL", "WEBSOCKET", "DEMO_HOST").forEach { proto ->
                            val isSelected = protocol == proto
                            val labelText = when (proto) {
                                "SSH" -> "SSH"
                                "LOCAL_SHELL" -> "Local"
                                "WEBSOCKET" -> "WS Relay"
                                "DEMO_HOST" -> "Demo"
                                else -> proto.take(5)
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { protocol = proto }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = labelText,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Label
                item {
                    Text("Connection Label", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("e.g. HomeLab Server", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Host & Port (if not local)
                if (protocol != "LOCAL_SHELL") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(2.5f)) {
                                Text("Host / IP", color = ImmersiveTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = host,
                                    onValueChange = { host = it },
                                    placeholder = { Text("192.168.1.10", color = ImmersiveTextMuted) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = ImmersiveComponent,
                                        unfocusedContainerColor = ImmersiveComponent,
                                        focusedTextColor = ImmersiveTextPrimary,
                                        unfocusedTextColor = ImmersiveTextPrimary,
                                        cursorColor = ImmersivePrimary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Port", color = ImmersiveTextSecondary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextField(
                                    value = portText,
                                    onValueChange = { portText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = ImmersiveComponent,
                                        unfocusedContainerColor = ImmersiveComponent,
                                        focusedTextColor = ImmersiveTextPrimary,
                                        unfocusedTextColor = ImmersiveTextPrimary,
                                        cursorColor = ImmersivePrimary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Username
                    item {
                        Text("Username", color = ImmersiveTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        TextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = ImmersiveComponent,
                                unfocusedContainerColor = ImmersiveComponent,
                                focusedTextColor = ImmersiveTextPrimary,
                                unfocusedTextColor = ImmersiveTextPrimary,
                                cursorColor = ImmersivePrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }

                    // Auth Type
                    item {
                        Text("Authentication", color = ImmersiveTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("PASSWORD", "PRIVATE_KEY", "PASSWORDLESS").forEach { auth ->
                                val isSelected = authType == auth
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { authType = auth }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = when (auth) {
                                                "PASSWORD" -> "Password"
                                                "PRIVATE_KEY" -> "SSH Key"
                                                else -> "None"
                                            },
                                            color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Password or Key Dropdown
                    if (authType == "PASSWORD") {
                        item {
                            Text("Password", color = ImmersiveTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = ImmersiveComponent,
                                    unfocusedContainerColor = ImmersiveComponent,
                                    focusedTextColor = ImmersiveTextPrimary,
                                    unfocusedTextColor = ImmersiveTextPrimary,
                                    cursorColor = ImmersivePrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            null,
                                            tint = ImmersiveTextSecondary
                                        )
                                    }
                                },
                                singleLine = true
                            )
                        }
                    } else if (authType == "PRIVATE_KEY") {
                        item {
                            Text("Select SSH Key", color = ImmersiveTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            if (savedKeys.isEmpty()) {
                                Text("No SSH keys found in Key Manager. Create one first.", color = ImmersiveStatusAmber, fontSize = 11.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    savedKeys.forEach { key ->
                                        val isSelected = selectedKeyId == key.id
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) ImmersivePrimary.copy(alpha = 0.2f) else ImmersiveComponent,
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, ImmersivePrimary) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedKeyId = key.id }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Icon(Icons.Default.Key, null, tint = ImmersivePrimary, modifier = Modifier.size(18.dp))
                                                Column {
                                                    Text(key.title, color = ImmersiveTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text(key.fingerprint, color = ImmersiveTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Group Tag
                item {
                    Text("Category Tag", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cloud", "HomeLab", "Production", "Local", "IoT").forEach { tag ->
                            val isSelected = groupTag == tag
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { groupTag = tag }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Shell Profile
                item {
                    Text("Shell Profile", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("default" to "Default", "bash" to "Bash", "zsh" to "Zsh", "fish" to "Fish", "python" to "Python").forEach { (profileKey, profileName) ->
                            val isSelected = shellProfileId == profileKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { shellProfileId = profileKey }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = profileName,
                                        color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Initial Directory
                item {
                    Text("Initial Working Directory (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = initialDirectory,
                        onValueChange = { initialDirectory = it },
                        placeholder = { Text("e.g. /var/www or /home/user", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Custom Environment Variables
                item {
                    Text("Environment Variables (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = envVars,
                        onValueChange = { envVars = it },
                        placeholder = { Text("e.g. TERM=xterm-256color, EDITOR=vim", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }

                // Startup Command
                item {
                    Text("Startup Command (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextField(
                        value = startupCommand,
                        onValueChange = { startupCommand = it },
                        placeholder = { Text("e.g. htop or tmux attach", color = ImmersiveTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (initialHost ?: HostEntity(
                        label = label.ifBlank { if (protocol == "LOCAL_SHELL") "Local Shell" else host },
                        host = if (protocol == "LOCAL_SHELL") "localhost" else host.ifBlank { "127.0.0.1" }
                    )).copy(
                        label = label.ifBlank { if (protocol == "LOCAL_SHELL") "Local Shell" else host },
                        host = if (protocol == "LOCAL_SHELL") "localhost" else host.ifBlank { "127.0.0.1" },
                        port = portText.toIntOrNull() ?: 22,
                        username = username.ifBlank { "root" },
                        protocol = protocol,
                        authType = authType,
                        password = if (authType == "PASSWORD") password else null,
                        keyId = if (authType == "PRIVATE_KEY") selectedKeyId else null,
                        groupTag = groupTag,
                        themeId = themeId,
                        shellProfileId = shellProfileId,
                        initialDirectory = initialDirectory.takeIf { it.isNotBlank() },
                        envVarsJson = envVars.takeIf { it.isNotBlank() },
                        startupCommand = startupCommand.takeIf { it.isNotBlank() }
                    )
                    onSave(entity)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                )
            ) {
                Text("Save Host", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersiveTextSecondary)
            }
        }
    )
}
