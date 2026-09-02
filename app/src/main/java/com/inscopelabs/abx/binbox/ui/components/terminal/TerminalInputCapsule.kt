package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun TerminalInputCapsule(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onSendSpecialKey: (TerminalKey) -> Unit,
    history: List<HistoryEntity>,
    inputFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // History Quick Select Row
            if (history.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(history.take(6)) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ImmersiveComponent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                            modifier = Modifier.clickable {
                                onInputTextChange(item.command)
                            }
                        ) {
                            Text(
                                text = item.command,
                                color = ImmersiveTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Capsule Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Capsule Input Container (rounded-2xl / 16.dp)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ImmersiveComponent)
                        .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$",
                        color = ImmersivePrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(inputFocusRequester)
                            .testTag("terminal_input_field"),
                        placeholder = {
                            Text("Type command...", color = ImmersiveTextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = ImmersiveTextPrimary,
                            unfocusedTextColor = ImmersiveTextPrimary,
                            cursorColor = ImmersivePrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ImmersiveTextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendCommand(inputText)
                                    onInputTextChange("")
                                }
                            }
                        )
                    )

                    // Quick Arrow History Browsers
                    IconButton(
                        onClick = { onSendSpecialKey(TerminalKey.ARROW_UP) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Previous command",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSendSpecialKey(TerminalKey.ARROW_DOWN) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Next command",
                            tint = ImmersiveTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendCommand(inputText)
                                onInputTextChange("")
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) ImmersivePrimary else Color.Transparent)
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send command",
                            tint = if (inputText.isNotBlank()) ImmersiveOnPrimary else ImmersivePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
