package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.KeyEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BinBoxViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeysScreen(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keys by viewModel.keys.collectAsStateWithLifecycle()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var keyToInspect by remember { mutableStateOf<KeyEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showGenerateDialog = true },
                containerColor = CyanAccent,
                contentColor = Slate950,
                modifier = Modifier.testTag("generate_key_fab")
            ) {
                Icon(Icons.Default.Key, contentDescription = "Generate SSH Key")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SSH Key Manager",
                        color = Slate100,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage cryptographic identity keys for passwordless authentication",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800)
                ) {
                    Icon(Icons.Default.Download, "Import Key", tint = CyanGlow, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick How-To Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Security, null, tint = CyanGlow, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Authorizing Keys on Remote Linux", color = Slate100, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Copy the public key and append to ~/.ssh/authorized_keys on the server.",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.KeyOff,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No SSH keypairs generated yet", color = Slate400, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showGenerateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Slate950, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate RSA Key", color = Slate950, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(keys, key = { it.id }) { key ->
                        KeyCard(
                            key = key,
                            onCopyPublic = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("SSH Public Key", key.publicKey))
                                viewModel.showSnackbar("Copied public key to clipboard")
                            },
                            onInspect = { keyToInspect = key },
                            onDelete = { viewModel.deleteKey(key) }
                        )
                    }
                }
            }
        }
    }

    // Generate Dialog
    if (showGenerateDialog) {
        var keyTitle by remember { mutableStateOf("id_rsa_main") }
        var keySize by remember { mutableIntStateOf(2048) }

        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            containerColor = Slate900,
            title = {
                Text("Generate New SSH Keypair", color = Slate100, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Key Name / Title", color = Slate400, fontSize = 12.sp)
                    TextField(
                        value = keyTitle,
                        onValueChange = { keyTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        singleLine = true
                    )

                    Text("Key Size", color = Slate400, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2048, 4096).forEach { size ->
                            val isSelected = keySize == size
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) CyanAccent else Slate800,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { keySize = size }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        text = "RSA $size-bit",
                                        color = if (isSelected) Slate950 else Slate300,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.generateRsaKey(keyTitle, keySize)
                        showGenerateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Generate", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        var importTitle by remember { mutableStateOf("") }
        var importPem by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Slate900,
            title = {
                Text("Import Private Key (PEM)", color = Slate100, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Key Title", color = Slate400, fontSize = 12.sp)
                    TextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        placeholder = { Text("e.g. aws-production.pem", color = Slate600) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        singleLine = true
                    )

                    Text("Paste Private Key", color = Slate400, fontSize = 12.sp)
                    TextField(
                        value = importPem,
                        onValueChange = { importPem = it },
                        placeholder = { Text("-----BEGIN RSA PRIVATE KEY-----\n...", color = Slate600) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Slate850,
                            unfocusedContainerColor = Slate850,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100
                        ),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        minLines = 4,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importPem.isNotBlank()) {
                            viewModel.saveCustomKey(
                                KeyEntity(
                                    title = importTitle.ifBlank { "imported_key_" + System.currentTimeMillis().toString().takeLast(4) },
                                    keyType = "Imported PEM",
                                    publicKey = "ssh-rsa (Imported key)",
                                    privateKey = importPem,
                                    fingerprint = "SHA256:imported..."
                                )
                            )
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Save Key", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }

    // Inspect Key Modal
    keyToInspect?.let { key ->
        AlertDialog(
            onDismissRequest = { keyToInspect = null },
            containerColor = Slate900,
            title = {
                Text(key.title, color = Slate100, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Public Key:", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate950,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = key.publicKey,
                                color = CyanGlow,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    item {
                        Text("Fingerprint:", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(key.fingerprint, color = AmberGlow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    item {
                        Text("Private Key:", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate950,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = key.privateKey,
                                color = Slate300,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Public Key", key.publicKey))
                        viewModel.showSnackbar("Copied public key")
                        keyToInspect = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Copy Public Key", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToInspect = null }) {
                    Text("Close", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun KeyCard(
    key: KeyEntity,
    onCopyPublic: () -> Unit,
    onInspect: () -> Unit,
    onDelete: () -> Unit
) {
    val df = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val dateStr = df.format(Date(key.createdAt))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ImmersivePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Key, null, tint = ImmersivePrimary, modifier = Modifier.size(22.dp))
                    }

                    Column {
                        Text(key.title, color = ImmersiveTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(key.keyType, color = ImmersiveTextSecondary, fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveComponent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                ) {
                    Text(
                        text = dateStr,
                        color = ImmersiveTextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fingerprint Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ImmersiveComponent,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = key.fingerprint,
                    color = ImmersiveStatusAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onInspect,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.Visibility, "View", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = ImmersiveStatusRed.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    }
                }

                Button(
                    onClick = onCopyPublic,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, tint = ImmersiveOnPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Public Key", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
