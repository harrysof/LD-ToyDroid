package com.ldtoypad.remote.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldtoypad.remote.data.repository.CatalogueRepository
import com.ldtoypad.remote.domain.model.CatalogueItem
import com.ldtoypad.remote.domain.model.TokenType
import com.ldtoypad.remote.domain.usecase.CreateToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val catalogueRepository: CatalogueRepository,
    private val createTokenUseCase: CreateToken
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow<TokenType?>(null)
    val filterType = _filterType.asStateFlow()

    val items = combine(
        catalogueRepository.items,
        _searchQuery,
        _filterType
    ) { items, query, typeFilter ->
        items.filter { item ->
            val matchesQuery = item.name.contains(query, ignoreCase = true) ||
                    item.abilities.any { it.contains(query, ignoreCase = true) }
            
            val matchesType = when (typeFilter) {
                TokenType.CHARACTER -> item is com.ldtoypad.remote.domain.model.Character
                TokenType.VEHICLE -> item is com.ldtoypad.remote.domain.model.Vehicle
                else -> true
            }

            matchesQuery && matchesType
        }.sortedBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            catalogueRepository.refresh()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TokenType?) {
        _filterType.value = type
    }

    fun createToken(item: CatalogueItem) {
        viewModelScope.launch {
            val type = if (item is com.ldtoypad.remote.domain.model.Character) TokenType.CHARACTER else TokenType.VEHICLE
            createTokenUseCase(item.id, type)
        }
    }
}
