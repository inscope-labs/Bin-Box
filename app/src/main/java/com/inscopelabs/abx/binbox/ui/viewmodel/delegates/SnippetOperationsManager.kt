package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.entity.HistoryEntity
import com.inscopelabs.abx.binbox.data.entity.SnippetEntity
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.CommandHistory
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.usecase.HistoryUseCases
import com.inscopelabs.abx.binbox.domain.usecase.SnippetUseCases
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SnippetOperationsManager(
    private val snippetUseCases: SnippetUseCases,
    private val historyUseCases: HistoryUseCases,
    private val coroutineScope: CoroutineScope,
    private val showSnackbar: (String) -> Unit
) {
    val domainSnippets: StateFlow<List<Snippet>> = snippetUseCases.getSnippets()
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val snippets: StateFlow<List<SnippetEntity>> = domainSnippets
        .map { list -> list.map { it.toEntity() } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val domainHistory: StateFlow<List<CommandHistory>> = historyUseCases.getHistory()
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<HistoryEntity>> = domainHistory
        .map { list -> list.map { it.toEntity() } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    private val _selectedSnippetForRun = MutableStateFlow<SnippetEntity?>(null)
    val selectedSnippetForRun: StateFlow<SnippetEntity?> = _selectedSnippetForRun.asStateFlow()

    fun openSnippetDialog(snippet: SnippetEntity) {
        _selectedSnippetForRun.value = snippet
    }

    fun dismissSnippetDialog() {
        _selectedSnippetForRun.value = null
    }

    fun executeSnippet(
        snippet: SnippetEntity,
        resolvedCommand: String,
        targetHostLabel: String,
        sessionManager: TerminalSessionManager,
        onExecuted: () -> Unit
    ) {
        _selectedSnippetForRun.value = null
        coroutineScope.launch {
            val domainSnippet = snippet.toDomain()
            snippetUseCases.executeSnippet(
                snippet = domainSnippet,
                targetHostLabel = targetHostLabel
            )
        }

        onExecuted()
        sessionManager.sendInputToActive(resolvedCommand + "\n")
        showSnackbar("Executed: ${snippet.title}")
    }

    fun saveSnippet(snippet: SnippetEntity) {
        coroutineScope.launch {
            val result = snippetUseCases.saveSnippet(snippet.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Saved snippet: ${snippet.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to save snippet: ${result.error.userMessage}")
            }
        }
    }

    fun deleteSnippet(snippet: SnippetEntity) {
        coroutineScope.launch {
            val result = snippetUseCases.deleteSnippet(snippet.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Deleted snippet: ${snippet.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to delete snippet: ${result.error.userMessage}")
            }
        }
    }
}
