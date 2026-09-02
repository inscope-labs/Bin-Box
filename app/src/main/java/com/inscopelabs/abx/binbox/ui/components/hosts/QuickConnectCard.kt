package com.inscopelabs.abx.binbox.ui.components.hosts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun QuickConnectCard(
    onConnect: (username: String, host: String, port: Int) -> Unit
) {
    var quickInput by remember { mutableStateOf("root@192.168.1.100:22") }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveStatusAmber.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FlashOn, null, tint = ImmersiveStatusAmber, modifier = Modifier.size(18.dp))
                Text("Quick SSH Connect", color = ImmersiveTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    placeholder = { Text("user@host:port", color = ImmersiveTextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = ImmersiveComponent,
                        unfocusedContainerColor = ImmersiveComponent,
                        focusedTextColor = ImmersiveTextPrimary,
                        unfocusedTextColor = ImmersiveTextPrimary,
                        cursorColor = ImmersivePrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val parsed = parseQuickConnect(quickInput)
                        onConnect(parsed.first, parsed.second, parsed.third)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersiveStatusAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Text("Go", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun parseQuickConnect(input: String): Triple<String, String, Int> {
    var user = "root"
    var host = input.trim()
    var port = 22

    if (host.contains("@")) {
        val splitAt = host.split("@", limit = 2)
        user = splitAt[0]
        host = splitAt[1]
    }

    if (host.contains(":")) {
        val splitPort = host.split(":", limit = 2)
        host = splitPort[0]
        port = splitPort[1].toIntOrNull() ?: 22
    }

    return Triple(user, host, port)
}
