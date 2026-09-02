package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import com.inscopelabs.abx.binbox.core.result.AppResult
import com.inscopelabs.abx.binbox.data.entity.KeyEntity
import com.inscopelabs.abx.binbox.data.mapper.toDomain
import com.inscopelabs.abx.binbox.data.mapper.toEntity
import com.inscopelabs.abx.binbox.domain.model.SshKey
import com.inscopelabs.abx.binbox.domain.usecase.KeyUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyOperationsManager(
    private val keyUseCases: KeyUseCases,
    private val coroutineScope: CoroutineScope,
    private val showSnackbar: (String) -> Unit
) {
    val domainKeys: StateFlow<List<SshKey>> = keyUseCases.getKeys()
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    val keys: StateFlow<List<KeyEntity>> = domainKeys
        .map { list -> list.map { it.toEntity() } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    fun generateRsaKey(title: String, keySize: Int = 2048) {
        coroutineScope.launch {
            when (val result = keyUseCases.generateKeyPair(title, keySize)) {
                is AppResult.Success -> showSnackbar("Generated SSH Key: ${result.data.title}")
                is AppResult.Error -> showSnackbar("Key generation failed: ${result.error.userMessage}")
                is AppResult.Loading -> {}
            }
        }
    }

    fun saveCustomKey(key: KeyEntity) {
        coroutineScope.launch {
            val result = keyUseCases.saveKey(key.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Saved key: ${key.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to save key: ${result.error.userMessage}")
            }
        }
    }

    fun deleteKey(key: KeyEntity) {
        coroutineScope.launch {
            val result = keyUseCases.deleteKey(key.toDomain())
            if (result is AppResult.Success) {
                showSnackbar("Deleted key: ${key.title}")
            } else if (result is AppResult.Error) {
                showSnackbar("Failed to delete key: ${result.error.userMessage}")
            }
        }
    }
}
