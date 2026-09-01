package com.inscopelabs.abx.binbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inscopelabs.abx.binbox.binboxshell.LocalShellFeature
import com.inscopelabs.abx.binbox.binboxshell.modules.ModuleState
import com.inscopelabs.abx.binbox.binboxshell.runtime.ShellTier
import com.inscopelabs.abx.binbox.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalShellModulesSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shellProvider = remember { LocalShellFeature.getProvider(context) }
    val tierStates by shellProvider.moduleStateManager.tierStates.collectAsStateWithLifecycle()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ImmersiveBorderSubtle) },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BinBox Local POSIX Packages",
                        color = ImmersiveTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Modular execution tiers for offline Android shell",
                        color = ImmersiveTextSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ImmersiveTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                items(ShellTier.entries) { tier ->
                    val manifest = remember(tier) { shellProvider.binaryRegistry.getManifest(tier) }
                    val state = tierStates[tier] ?: ModuleState.Available

                    TierModuleCard(
                        tier = tier,
                        title = when (tier) {
                            ShellTier.BASE -> "Base POSIX Layer (Built-in)"
                            ShellTier.STANDARD -> "Standard Productivity Pack"
                            ShellTier.EXTENDED -> "Extended Developer Toolchain"
                        },
                        description = manifest?.description ?: "",
                        binaries = manifest?.binaries ?: emptyList(),
                        state = state,
                        onInstallClick = {
                            shellProvider.requestTierInstallation(tier)
                        }
                    )
                }
            }
        }
    }
}
