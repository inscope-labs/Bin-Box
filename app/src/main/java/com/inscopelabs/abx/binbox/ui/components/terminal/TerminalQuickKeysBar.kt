package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.terminal.engine.TerminalKey
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun TerminalQuickKeysBar(
    ctrlLatched: Boolean,
    altLatched: Boolean,
    onToggleCtrl: () -> Unit,
    onToggleAlt: () -> Unit,
    onSendSpecialKey: (TerminalKey) -> Unit,
    onSendRawInput: (String) -> Unit,
    onSendCommand: (String) -> Unit,
    onSendEnter: (() -> Unit)? = null,
    onHistoryUp: (() -> Unit)? = null,
    onHistoryDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Latched Modifier Keys
        AccessoryKeyButton(
            label = "ESC",
            onClick = { onSendSpecialKey(TerminalKey.ESC) }
        )
        AccessoryKeyButton(
            label = "TAB",
            onClick = { onSendSpecialKey(TerminalKey.TAB) }
        )
        AccessoryKeyButton(
            label = "ENTER",
            onClick = {
                if (onSendEnter != null) {
                    onSendEnter()
                } else {
                    onSendRawInput("\n")
                }
            },
            accentColor = ImmersivePrimary
        )
        AccessoryKeyButton(
            label = "CTRL",
            isLatched = ctrlLatched,
            onClick = onToggleCtrl,
            accentColor = ImmersivePrimary
        )
        AccessoryKeyButton(
            label = "ALT",
            isLatched = altLatched,
            onClick = onToggleAlt,
            accentColor = ImmersiveStatusAmber
        )

        // Special LOGS Action Pill with Ice Blue Luminous Accent
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ImmersivePrimary,
            modifier = Modifier
                .clickable { onSendCommand("journalctl -n 30 --no-pager || dmesg | tail -n 30") }
                .height(34.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = ImmersiveOnPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "LOGS",
                    color = ImmersiveOnPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Quick Interrupt / Terminal Combos
        ComboKeyPill(label = "^C", sub = "SIGINT", onClick = { onSendSpecialKey(TerminalKey.CTRL_C) }, tint = ImmersiveStatusRed)
        ComboKeyPill(label = "^D", sub = "EOF", onClick = { onSendSpecialKey(TerminalKey.CTRL_D) })
        ComboKeyPill(label = "^Z", sub = "STOP", onClick = { onSendSpecialKey(TerminalKey.CTRL_Z) }, tint = ImmersiveStatusAmber)
        ComboKeyPill(label = "^L", sub = "CLEAR", onClick = { onSendSpecialKey(TerminalKey.CTRL_L) })
        ComboKeyPill(label = "^A", sub = "HOME", onClick = { onSendSpecialKey(TerminalKey.CTRL_A) })
        ComboKeyPill(label = "^E", sub = "END", onClick = { onSendSpecialKey(TerminalKey.CTRL_E) })

        // Directional Arrows
        AccessoryKeyButton(
            label = "▲",
            onClick = {
                if (onHistoryUp != null) {
                    onHistoryUp()
                } else {
                    onSendSpecialKey(TerminalKey.ARROW_UP)
                }
            }
        )
        AccessoryKeyButton(
            label = "▼",
            onClick = {
                if (onHistoryDown != null) {
                    onHistoryDown()
                } else {
                    onSendSpecialKey(TerminalKey.ARROW_DOWN)
                }
            }
        )
        AccessoryKeyButton(label = "◀", onClick = { onSendSpecialKey(TerminalKey.ARROW_LEFT) })
        AccessoryKeyButton(label = "▶", onClick = { onSendSpecialKey(TerminalKey.ARROW_RIGHT) })

        // Symbols & Navigation Keys
        AccessorySymbolKey(symbol = "|", onClick = { onSendRawInput("|") })
        AccessorySymbolKey(symbol = "/", onClick = { onSendRawInput("/") })
        AccessorySymbolKey(symbol = "\\", onClick = { onSendRawInput("\\") })
        AccessorySymbolKey(symbol = "~", onClick = { onSendRawInput("~") })
        AccessorySymbolKey(symbol = "-", onClick = { onSendRawInput("-") })
        AccessorySymbolKey(symbol = "_", onClick = { onSendRawInput("_") })
        AccessorySymbolKey(symbol = ":", onClick = { onSendRawInput(":") })
        AccessorySymbolKey(symbol = "$", onClick = { onSendRawInput("$") })
        AccessorySymbolKey(symbol = ">", onClick = { onSendRawInput(">") })
    }
}

@Composable
fun AccessoryKeyButton(
    label: String,
    onClick: () -> Unit,
    isLatched: Boolean = false,
    accentColor: Color = ImmersivePrimary
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLatched) accentColor.copy(alpha = 0.25f) else ImmersiveComponent,
        border = if (isLatched) androidx.compose.foundation.BorderStroke(1.dp, accentColor) else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                color = if (isLatched) accentColor else ImmersiveTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ComboKeyPill(
    label: String,
    sub: String,
    onClick: () -> Unit,
    tint: Color = ImmersivePrimary
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ImmersiveComponent,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(34.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sub,
                color = ImmersiveTextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AccessorySymbolKey(
    symbol: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ImmersiveComponent,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                color = ImmersiveTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
