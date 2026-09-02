package com.inscopelabs.abx.binbox.ui.viewmodel.delegates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchStateManager {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSearchCaseSensitive = MutableStateFlow(false)
    val isSearchCaseSensitive: StateFlow<Boolean> = _isSearchCaseSensitive.asStateFlow()

    private val _isSearchRegex = MutableStateFlow(false)
    val isSearchRegex: StateFlow<Boolean> = _isSearchRegex.asStateFlow()

    private val _searchMatchIndex = MutableStateFlow(0)
    val searchMatchIndex: StateFlow<Int> = _searchMatchIndex.asStateFlow()

    private val _searchMatchTotal = MutableStateFlow(0)
    val searchMatchTotal: StateFlow<Int> = _searchMatchTotal.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearching(searching: Boolean) {
        _isSearching.value = searching
        if (!searching) {
            _searchQuery.value = ""
        }
    }

    fun toggleSearchCaseSensitive() {
        _isSearchCaseSensitive.value = !_isSearchCaseSensitive.value
    }

    fun toggleSearchRegex() {
        _isSearchRegex.value = !_isSearchRegex.value
    }

    fun nextSearchMatch() {
        if (_searchMatchTotal.value > 0) {
            _searchMatchIndex.value = (_searchMatchIndex.value + 1) % _searchMatchTotal.value
        }
    }

    fun prevSearchMatch() {
        if (_searchMatchTotal.value > 0) {
            _searchMatchIndex.value = if (_searchMatchIndex.value - 1 < 0) _searchMatchTotal.value - 1 else _searchMatchIndex.value - 1
        }
    }

    fun setSearchMatchStats(current: Int, total: Int) {
        _searchMatchIndex.value = current
        _searchMatchTotal.value = total
    }
}
