package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun SnippetRunnerDialog(
    snippet: SnippetEntity,
    onDismiss: () -> Unit,
    onExecute: (resolvedCommand: String) -> Unit
) {
    // Extract parameters from template: {{param}} or {{param:default}}
    val pattern = Regex("\\{\\{([^}]+)\\}\\}")
    val matches = remember(snippet.commandTemplate) {
        pattern.findAll(snippet.commandTemplate).map { it.groupValues[1] }.distinct().toList()
    }

    val paramValues = remember(matches) {
        mutableStateMapOf<String, String>().apply {
            matches.forEach { raw ->
                if (raw.contains(":")) {
                    val split = raw.split(":", limit = 2)
                    this[raw] = split[1]
                } else {
                    this[raw] = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Column {
                Text("Run Script in Terminal", color = Slate100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(snippet.title, color = CyanGlow, fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (matches.isNotEmpty()) {
                    Text("Provide Parameter Values:", color = Slate400, fontSize = 12.sp)
                    matches.forEach { key ->
                        val label = if (key.contains(":")) key.split(":", limit = 2)[0] else key
                        Column {
                            Text(label, color = Slate300, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            TextField(
                                value = paramValues[key] ?: "",
                                onValueChange = { paramValues[key] = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Slate850,
                                    unfocusedContainerColor = Slate850,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                ),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            )
                        }
                    }
                }

                // Command Preview
                val resolvedPreview = remember(paramValues.values.toList()) {
                    var resolved = snippet.commandTemplate
                    matches.forEach { key ->
                        val value = paramValues[key] ?: ""
                        resolved = resolved.replace("{{$key}}", value)
                    }
                    resolved
                }

                Text("Resolved Command:", color = Slate400, fontSize = 11.sp)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate950,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = resolvedPreview,
                        color = EmeraldGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var resolved = snippet.commandTemplate
                    matches.forEach { key ->
                        val value = paramValues[key] ?: ""
                        resolved = resolved.replace("{{$key}}", value)
                    }
                    onExecute(resolved)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Slate950, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Execute", color = Slate950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
