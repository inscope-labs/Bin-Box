package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

@Composable
fun RenameSessionDialog(
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(20.dp))
                Text("Rename Session Tab", color = ImmersiveTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter a descriptive title for this terminal tab:", color = ImmersiveTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Production Web Bastion", color = ImmersiveTextMuted) },
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
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim())
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                )
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersiveTextSecondary)
            }
        }
    )
}
