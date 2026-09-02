package com.inscopelabs.abx.binbox.ui.components.terminal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.abx.binbox.terminal.model.TerminalLine
import com.inscopelabs.abx.binbox.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TerminalSearchBar(
    searchQuery: String,
    sessionLines: List<TerminalLine>,
    listState: LazyListState,
    scope: CoroutineScope,
    onQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCaseSensitive by remember { mutableStateOf(false) }
    val matchingLineIndices = remember(sessionLines, searchQuery, isCaseSensitive) {
        if (searchQuery.isBlank()) emptyList<Int>()
        else sessionLines.mapIndexedNotNull { index, line ->
            if (line.rawText.contains(searchQuery, ignoreCase = !isCaseSensitive)) index else null
        }
    }
    var currentMatchIdx by remember { mutableIntStateOf(0) }

    Surface(
        color = ImmersiveSurface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = ImmersivePrimary, modifier = Modifier.size(16.dp))

            TextField(
                value = searchQuery,
                onValueChange = {
                    onQueryChange(it)
                    currentMatchIdx = 0
                },
                placeholder = { Text("Search terminal scrollback...", color = ImmersiveTextMuted, fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = ImmersiveTextPrimary,
                    unfocusedTextColor = ImmersiveTextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Clear", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )

            // Case Sensitivity Toggle (Aa)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCaseSensitive) ImmersivePrimary else ImmersiveComponent,
                modifier = Modifier
                    .clickable { isCaseSensitive = !isCaseSensitive }
                    .size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "Aa",
                        color = if (isCaseSensitive) ImmersiveOnPrimary else ImmersiveTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Match Count Badge
            if (searchQuery.isNotBlank()) {
                Text(
                    text = if (matchingLineIndices.isEmpty()) "0" else "${currentMatchIdx + 1}/${matchingLineIndices.size}",
                    color = if (matchingLineIndices.isEmpty()) ImmersiveStatusRed else ImmersiveStatusGreen,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                // Prev Match Button
                IconButton(
                    onClick = {
                        if (matchingLineIndices.isNotEmpty()) {
                            currentMatchIdx = if (currentMatchIdx > 0) currentMatchIdx - 1 else matchingLineIndices.size - 1
                            scope.launch {
                                listState.animateScrollToItem(matchingLineIndices[currentMatchIdx])
                            }
                        }
                    },
                    enabled = matchingLineIndices.isNotEmpty(),
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Previous match", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                }

                // Next Match Button
                IconButton(
                    onClick = {
                        if (matchingLineIndices.isNotEmpty()) {
                            currentMatchIdx = if (currentMatchIdx < matchingLineIndices.size - 1) currentMatchIdx + 1 else 0
                            scope.launch {
                                listState.animateScrollToItem(matchingLineIndices[currentMatchIdx])
                            }
                        }
                    },
                    enabled = matchingLineIndices.isNotEmpty(),
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, "Next match", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            // Close Search Button
            IconButton(
                onClick = onCloseSearch,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(Icons.Default.Close, "Close search", tint = ImmersiveTextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
