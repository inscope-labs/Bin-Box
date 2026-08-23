package com.inscopelabs.abx.binbox.data.repository

import com.inscopelabs.abx.binbox.core.dispatcher.CoroutineDispatchersProvider
import com.inscopelabs.abx.binbox.core.dispatcher.DefaultCoroutineDispatchers
import com.inscopelabs.abx.binbox.core.error.AppError
import com.inscopelabs.abx.binbox.core.logging.BinBoxLogger
import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.dao.SnippetDao
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.Snippet
import com.inscopelabs.abx.binbox.domain.repository.ISnippetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SnippetRepositoryImpl(
    private val snippetDao: SnippetDao,
    private val dispatchers: CoroutineDispatchersProvider = DefaultCoroutineDispatchers
) : ISnippetRepository {

    override fun getAllSnippets(): Flow<List<Snippet>> {
        return snippetDao.getAllSnippets()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getSnippetById(id: Long): Snippet? = withContext(dispatchers.io) {
        snippetDao.getAllSnippets() // If needed or direct
        null
    }

    override suspend fun saveSnippet(snippet: Snippet): AppResult<Long> = withContext(dispatchers.io) {
        try {
            val entity = snippet.toEntity()
            val id = if (entity.id == 0L) {
                snippetDao.insertSnippet(entity)
            } else {
                snippetDao.updateSnippet(entity)
                entity.id
            }
            BinBoxLogger.d("SnippetRepository", "Saved snippet: ${snippet.title} (ID: $id)")
            AppResult.Success(id)
        } catch (e: Throwable) {
            BinBoxLogger.e("SnippetRepository", "Failed to save snippet: ${snippet.title}", e)
            AppResult.Error(AppError.IoError("Failed to save snippet", e))
        }
    }

    override suspend fun deleteSnippet(snippet: Snippet): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            snippetDao.deleteSnippet(snippet.toEntity())
            BinBoxLogger.d("SnippetRepository", "Deleted snippet: ${snippet.title}")
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            BinBoxLogger.e("SnippetRepository", "Failed to delete snippet: ${snippet.title}", e)
            AppResult.Error(AppError.IoError("Failed to delete snippet", e))
        }
    }

    override suspend fun incrementUsage(id: Long): AppResult<Unit> = withContext(dispatchers.io) {
        try {
            snippetDao.incrementUsage(id)
            AppResult.Success(Unit)
        } catch (e: Throwable) {
            AppResult.Error(AppError.IoError("Failed to increment snippet usage", e))
        }
    }
}
