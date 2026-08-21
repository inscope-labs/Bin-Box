package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SnippetEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BinBoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(
    viewModel: BinBoxViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snippets by viewModel.snippets.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var snippetToEdit by remember { mutableStateOf<SnippetEntity?>(null) }

    val categories = listOf("All", "Favorites", "System", "Docker", "Git", "Network", "Database", "Custom")

    val filteredSnippets = snippets.filter { snippet ->
        val matchesQuery = snippet.title.contains(searchQuery, ignoreCase = true) ||
                snippet.commandTemplate.contains(searchQuery, ignoreCase = true) ||
                snippet.description.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (selectedCategory) {
            "All" -> true
            "Favorites" -> snippet.isFavorite
            else -> snippet.category.equals(selectedCategory, ignoreCase = true)
        }

        matchesQuery && matchesCategory
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    snippetToEdit = null
                    showAddEditDialog = true
                },
                containerColor = CyanAccent,
                contentColor = Slate950,
                modifier = Modifier.testTag("add_snippet_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add snippet")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Script & Snippet Vault",
                        color = Slate100,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quick parameterized commands for terminal sessions",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search scripts by name or command...", color = ImmersiveTextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = ImmersiveTextSecondary, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ImmersiveComponent,
                    unfocusedContainerColor = ImmersiveComponent,
                    focusedTextColor = ImmersiveTextPrimary,
                    unfocusedTextColor = ImmersiveTextPrimary,
                    cursorColor = ImmersivePrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        shape = RoundedCornerShape(10.dp),
                        label = {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ImmersivePrimary,
                            selectedLabelColor = ImmersiveOnPrimary,
                            containerColor = ImmersiveComponent,
                            labelColor = ImmersiveTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ImmersiveBorderVerySubtle,
                            selectedBorderColor = ImmersivePrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Snippets List
            if (filteredSnippets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = ImmersiveTextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No scripts found", color = ImmersiveTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredSnippets, key = { it.id }) { snippet ->
                        SnippetCard(
                            snippet = snippet,
                            onRun = {
                                viewModel.openSnippetDialog(snippet)
                            },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Command", snippet.commandTemplate))
                                viewModel.showSnackbar("Copied script: ${snippet.title}")
                            },
                            onToggleFavorite = {
                                viewModel.saveSnippet(snippet.copy(isFavorite = !snippet.isFavorite))
                            },
                            onEdit = {
                                snippetToEdit = snippet
                                showAddEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteSnippet(snippet)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Snippet Dialog
    if (showAddEditDialog) {
        AddEditSnippetDialog(
            initialSnippet = snippetToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { snippet ->
                viewModel.saveSnippet(snippet)
                showAddEditDialog = false
            }
        )
    }
}

@Composable
fun SnippetCard(
    snippet: SnippetEntity,
    onRun: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ImmersiveSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Category Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ImmersiveComponent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle)
                    ) {
                        Text(
                            text = snippet.category,
                            color = ImmersivePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = snippet.title,
                        color = ImmersiveTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (snippet.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (snippet.isFavorite) ImmersiveStatusAmber else ImmersiveTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (snippet.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = snippet.description,
                    color = ImmersiveTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Command Code Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ImmersiveComponent,
                border = androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = snippet.commandTemplate,
                    color = ImmersiveStatusGreen,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.Edit, "Edit", tint = ImmersiveTextSecondary, modifier = Modifier.size(14.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ImmersiveComponent)
                    ) {
                        Icon(Icons.Default.DeleteOutline, "Delete", tint = ImmersiveStatusRed.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    }
                }

                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImmersivePrimary,
                        contentColor = ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = ImmersiveOnPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run in Terminal", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSnippetDialog(
    initialSnippet: SnippetEntity?,
    onDismiss: () -> Unit,
    onSave: (SnippetEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialSnippet?.title ?: "") }
    var command by remember { mutableStateOf(initialSnippet?.commandTemplate ?: "") }
    var category by remember { mutableStateOf(initialSnippet?.category ?: "Custom") }
    var description by remember { mutableStateOf(initialSnippet?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ImmersiveSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (initialSnippet == null) "New Terminal Script" else "Edit Script",
                color = ImmersiveTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Title", color = ImmersiveTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Restart Docker App", color = ImmersiveTextMuted) },
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
                    ),
                    singleLine = true
                )

                Text("Command Template", color = ImmersiveTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = { Text("docker restart {{container:web-proxy}}", color = ImmersiveTextMuted) },
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
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    minLines = 2,
                    maxLines = 4
                )
                Text(
                    text = "Tip: Use {{variable_name}} or {{var:default_value}} for dynamic prompts when running.",
                    color = ImmersiveStatusAmber,
                    fontSize = 10.sp
                )

                Text("Category", color = ImmersiveTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Custom", "System", "Docker", "Git", "Network", "Database")) { cat ->
                        val isSelected = category == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ImmersivePrimary else ImmersiveComponent,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, ImmersiveBorderVerySubtle),
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) ImmersiveOnPrimary else ImmersiveTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text("Description (Optional)", color = ImmersiveTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Notes on what this script does", color = ImmersiveTextMuted) },
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
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = (initialSnippet ?: SnippetEntity(
                        title = title.ifBlank { "Untitled Script" },
                        commandTemplate = command
                    )).copy(
                        title = title.ifBlank { "Untitled Script" },
                        commandTemplate = command,
                        category = category,
                        description = description
                    )
                    onSave(entity)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                )
            ) {
                Text("Save Script", color = ImmersiveOnPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersiveTextSecondary)
            }
        }
    )
}
