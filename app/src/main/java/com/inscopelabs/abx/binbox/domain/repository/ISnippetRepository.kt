package com.inscopelabs.abx.binbox.domain.repository

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.domain.model.Snippet
import kotlinx.coroutines.flow.Flow

interface ISnippetRepository {
    fun getAllSnippets(): Flow<List<Snippet>>
    suspend fun getSnippetById(id: Long): Snippet?
    suspend fun saveSnippet(snippet: Snippet): AppResult<Long>
    suspend fun deleteSnippet(snippet: Snippet): AppResult<Unit>
    suspend fun incrementUsage(id: Long): AppResult<Unit>
}
