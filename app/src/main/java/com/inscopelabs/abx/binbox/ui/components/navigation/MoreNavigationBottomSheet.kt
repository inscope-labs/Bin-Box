package com.inscopelabs.abx.binbox.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*

/**
 * Bottom Sheet menu opened by the bottom bar's 'More' menu button.
 * Contains four primary action buttons:
 * 1. Scripts (Scripts Vault)
 * 2. Keys (SSH Keypair Manager)
 * 3. Files (File & Directory Transfer Engine)
 * 4. Context Settings / Quick Options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreNavigationBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onNavigateToKeys: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenContextSettings: () -> Unit,
    scriptsLabel: String = "Scripts",
    keysLabel: String = "Keys",
    filesLabel: String = "Files",
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurfaceElevated,
        contentColor = ImmersiveTextPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = ImmersiveBorderSubtle)
        },
        modifier = modifier.testTag("more_navigation_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tools & Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImmersiveTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quick access to automation, identities, and transfers",
                        style = MaterialTheme.typography.bodySmall,
                        color = ImmersiveTextSecondary
                    )
                }
            }

            // Grid of 4 Menu Buttons (2x2 layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Scripts Button
                MoreMenuItemCard(
                    icon = Icons.Default.Code,
                    iconColor = CyanAccent,
                    title = scriptsLabel,
                    subtitle = "Automations & snippets",
                    testTag = "more_menu_scripts_btn",
                    onClick = {
                        onDismiss()
                        onNavigateToScripts()
                    },
                    modifier = Modifier.weight(1f)
                )

                // 2. Keys Button
                MoreMenuItemCard(
                    icon = Icons.Default.VpnKey,
                    iconColor = AmberAccent,
                    title = keysLabel,
                    subtitle = "SSH credentials & vault",
                    testTag = "more_menu_keys_btn",
                    onClick = {
                        onDismiss()
                        onNavigateToKeys()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3. Files Button
                MoreMenuItemCard(
                    icon = Icons.Default.Folder,
                    iconColor = ImmersivePrimary,
                    title = filesLabel,
                    subtitle = "Transfers & downloads",
                    testTag = "more_menu_files_btn",
                    onClick = {
                        onDismiss()
                        onOpenFiles()
                    },
                    modifier = Modifier.weight(1f)
                )

                // 4. Preferences / Quick Tuning Button
                MoreMenuItemCard(
                    icon = Icons.Default.Tune,
                    iconColor = EmeraldGreen,
                    title = "Preferences",
                    subtitle = "Display & terminal settings",
                    testTag = "more_menu_context_btn",
                    onClick = {
                        onDismiss()
                        onOpenContextSettings()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItemCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ImmersiveSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = ImmersiveTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ImmersiveTextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
