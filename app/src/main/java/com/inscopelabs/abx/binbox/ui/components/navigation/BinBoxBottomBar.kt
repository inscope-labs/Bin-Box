package com.inscopelabs.abx.binbox.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.ui.theme.*
import com.inscopelabs.abx.binbox.ui.viewmodel.AppTab

/**
 * Bottom control bar replacing the legacy NavigationBar.
 * Features:
 * 1. Circular context menu button (small)
 * 2. 2-phase Terminal / Hosts switch button (larger, displays active name and non-active icon)
 * 3. Circular more menu button (small, opens bottom sheet)
 */
@Composable
fun BinBoxBottomBar(
    currentTab: AppTab,
    terminalLabel: String,
    hostsLabel: String,
    onToggleTerminalHosts: () -> Unit,
    onContextMenuClick: () -> Unit,
    onMoreMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. Circular Context Menu Button (Smaller, 38dp)
            IconButton(
                onClick = onContextMenuClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ImmersiveSurfaceElevated)
                    .border(1.dp, ImmersiveBorderSubtle, CircleShape)
                    .testTag("bottom_bar_context_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Context Menu",
                    tint = ImmersiveTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            // B. 2-Phase Terminal / Hosts Button (Center, larger)
            val isTerminal = currentTab != AppTab.HOSTS
            val interactionSource = remember { MutableInteractionSource() }

            val trackBgColor by animateColorAsState(
                targetValue = ImmersiveSurfaceElevated,
                animationSpec = tween(250),
                label = "track_bg"
            )

            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(trackBgColor)
                    .border(1.dp, ImmersiveBorderSubtle, RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onToggleTerminalHosts
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("two_phase_tab_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTerminal) {
                    // Terminal is Active: Display active pill on left, disabled track icon on right
                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(ImmersivePrimary)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ImmersiveOnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = terminalLabel,
                            color = ImmersiveOnPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Non-active Hosts icon in the disabled track
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = hostsLabel,
                            tint = ImmersiveTextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                } else {
                    // Hosts is Active: Non-active Terminal icon on left, active pill on right
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = terminalLabel,
                            tint = ImmersiveTextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(ImmersivePrimary)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = ImmersiveOnPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = hostsLabel,
                            color = ImmersiveOnPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            // C. Circular More Icon Menu Button (Smaller, 38dp)
            IconButton(
                onClick = onMoreMenuClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ImmersiveSurfaceElevated)
                    .border(1.dp, ImmersiveBorderSubtle, CircleShape)
                    .testTag("bottom_bar_more_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "More Menu",
                    tint = ImmersiveTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
