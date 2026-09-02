package com.inscopelabs.abx.binbox.ui.viewmodel

import android.app.Application
import com.inscopelabs.abx.binbox.data.database.AppDatabase
import com.inscopelabs.abx.binbox.data.repository.HistoryRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.HostRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.KeyRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.SessionRepositoryImpl
import com.inscopelabs.abx.binbox.data.repository.SnippetRepositoryImpl
import com.inscopelabs.abx.binbox.domain.usecase.HistoryUseCases
import com.inscopelabs.abx.binbox.domain.usecase.HostUseCases
import com.inscopelabs.abx.binbox.domain.usecase.KeyUseCases
import com.inscopelabs.abx.binbox.domain.usecase.ManageSessionUseCase
import com.inscopelabs.abx.binbox.domain.usecase.SnippetUseCases
import com.inscopelabs.abx.binbox.security.HostKeyStore
import com.inscopelabs.abx.binbox.security.SecureStorageService
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionFactory
import com.inscopelabs.abx.binbox.terminal.engine.TerminalSessionManager

data class DependencyGraph(
    val hostUseCases: HostUseCases,
    val keyUseCases: KeyUseCases,
    val snippetUseCases: SnippetUseCases,
    val historyUseCases: HistoryUseCases,
    val manageSessionUseCase: ManageSessionUseCase,
    val sessionManager: TerminalSessionManager
) {
    companion object {
        fun create(application: Application): DependencyGraph {
            val db = AppDatabase.getInstance(application)
            val secureStorage = SecureStorageService(application)
            val hostRepo = HostRepositoryImpl(db.hostDao(), secureStorage)
            val keyRepo = KeyRepositoryImpl(db.keyDao(), secureStorage)
            val snippetRepo = SnippetRepositoryImpl(db.snippetDao())
            val historyRepo = HistoryRepositoryImpl(db.historyDao())
            val sessionRepo = SessionRepositoryImpl()
            val hostKeyStore = HostKeyStore(db.knownHostKeyDao())

            val hostUseCases = HostUseCases.create(hostRepo)
            val keyUseCases = KeyUseCases.create(keyRepo)
            val snippetUseCases = SnippetUseCases.create(snippetRepo, historyRepo)
            val historyUseCases = HistoryUseCases.create(historyRepo)
            val manageSessionUseCase = ManageSessionUseCase(sessionRepo)

            val sessionFactory = TerminalSessionFactory(keyRepository = keyRepo, hostKeyStore = hostKeyStore)
            val sessionManager = TerminalSessionManager(
                sessionFactory = sessionFactory,
                sessionRepository = sessionRepo
            )

            return DependencyGraph(
                hostUseCases = hostUseCases,
                keyUseCases = keyUseCases,
                snippetUseCases = snippetUseCases,
                historyUseCases = historyUseCases,
                manageSessionUseCase = manageSessionUseCase,
                sessionManager = sessionManager
            )
        }
    }
}
