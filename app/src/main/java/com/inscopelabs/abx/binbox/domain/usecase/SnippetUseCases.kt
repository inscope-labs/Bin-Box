package com.inscopelabs.abx.binbox.domain.usecase

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.repository.IHistoryRepository
import com.inscopelabs.abx.binbox.domain.repository.ISnippetRepository
import kotlinx.coroutines.flow.Flow

class GetSnippetsUseCase(private val snippetRepository: ISnippetRepository) {
    operator fun invoke(): Flow<List<Snippet>> = snippetRepository.getAllSnippets()
}

class SaveSnippetUseCase(private val snippetRepository: ISnippetRepository) {
    suspend operator fun invoke(snippet: Snippet): AppResult<Long> =
        snippetRepository.saveSnippet(snippet)
}

class DeleteSnippetUseCase(private val snippetRepository: ISnippetRepository) {
    suspend operator fun invoke(snippet: Snippet): AppResult<Unit> =
        snippetRepository.deleteSnippet(snippet)
}

class ExecuteSnippetUseCase(
    private val snippetRepository: ISnippetRepository,
    private val historyRepository: IHistoryRepository
) {
    private val placeholderRegex = Regex("""\{\{([^:}]+)(?::([^}]*))?\}\}""")

    /**
     * Resolves variables in snippet template and tracks metrics.
     * Example: "ping -c 4 {{host:1.1.1.1}}" with param ["host" -> "8.8.8.8"] resolves to "ping -c 4 8.8.8.8".
     */
    suspend operator fun invoke(
        snippet: Snippet,
        params: Map<String, String> = emptyMap(),
        targetHostLabel: String = "Active Terminal"
    ): AppResult<String> {
        val resolvedCommand = placeholderRegex.replace(snippet.commandTemplate) { matchResult ->
            val key = matchResult.groupValues[1].trim()
            val defaultValue = matchResult.groupValues.getOrNull(2) ?: ""
            params[key]?.takeIf { it.isNotBlank() } ?: defaultValue
        }

        if (snippet.id > 0) {
            snippetRepository.incrementUsage(snippet.id)
        }
        historyRepository.recordHistory(resolvedCommand, targetHostLabel)

        return AppResult.Success(resolvedCommand)
    }
}
