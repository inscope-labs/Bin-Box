package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.HostEntity
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun DeleteHostConfirmationDialog(
    host: HostEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val challengeWords = remember(host.id, host.label) {
        getChallengeWordsForHost(host)
    }

    var userInput by remember { mutableStateOf("") }
    val isMatched = userInput.trim().equals(challengeWords, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ImmersiveStatusRed.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = ImmersiveStatusRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Delete Host Shell",
                        color = ImmersiveTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = host.label,
                        color = ImmersiveTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Irrevocable Warning Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ImmersiveStatusRed.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveStatusRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Irrevocable Action",
                            color = ImmersiveStatusRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This action is permanent and cannot be undone. All connection credentials, saved keys, and settings for this host shell will be permanently removed.",
                            color = ImmersiveTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Instructions & Challenge Box
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Please enter the two words displayed below to confirm:",
                        color = ImmersiveTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ImmersiveComponent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = challengeWords,
                                color = ImmersiveStatusAmber,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("delete_host_challenge_text")
                            )
                        }
                    }
                }

                // User Input Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = {
                            Text(
                                text = "Type the two words here",
                                color = ImmersiveTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_host_verification_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ImmersiveComponent,
                            unfocusedContainerColor = ImmersiveComponent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = if (isMatched) ImmersiveStatusGreen else ImmersivePrimary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isMatched) onConfirm()
                            }
                        )
                    )

                    // Feedback status text
                    if (userInput.isNotBlank()) {
                        if (isMatched) {
                            Text(
                                text = "✓ Verification matched. Delete button is now enabled.",
                                color = ImmersiveStatusGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "Words do not match. Please enter '$challengeWords'",
                                color = ImmersiveStatusAmber,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isMatched,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersiveStatusRed,
                    contentColor = Color.White,
                    disabledContainerColor = ImmersiveComponent,
                    disabledContentColor = ImmersiveTextMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_delete_host_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Delete Host",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_host_button")
            ) {
                Text(
                    text = "Cancel",
                    color = ImmersiveTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    )
}

internal fun getChallengeWordsForHost(host: HostEntity): String {
    val wordPairs = listOf(
        "DELETE HOST",
        "CONFIRM REMOVE",
        "DROP SHELL",
        "PURGE HOST",
        "PERMANENT DELETE",
        "DISCARD PROFILE"
    )
    val index = (host.id.toInt().let { if (it < 0) -it else it } + host.label.hashCode().let { if (it < 0) -it else it }) % wordPairs.size
    return wordPairs[index]
}
