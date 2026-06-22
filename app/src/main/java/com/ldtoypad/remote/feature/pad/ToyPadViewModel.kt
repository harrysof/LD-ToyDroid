package com.ldtoypad.remote.feature.pad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ldtoypad.remote.data.repository.CatalogueRepository
import com.ldtoypad.remote.data.repository.ToyPadRepository
import com.ldtoypad.remote.domain.model.RemoteToken
import com.ldtoypad.remote.domain.usecase.MoveToken
import com.ldtoypad.remote.domain.usecase.PlaceToken
import com.ldtoypad.remote.domain.usecase.RemoveToken
import kotlinx.coroutines.launch

class ToyPadViewModel(
    private val toyPadRepository: ToyPadRepository,
    private val catalogueRepository: CatalogueRepository,
    private val placeTokenUseCase: PlaceToken,
    private val removeTokenUseCase: RemoveToken,
    private val moveTokenUseCase: MoveToken
) : ViewModel() {

    val padState = toyPadRepository.padState

    init {
        viewModelScope.launch {
            toyPadRepository.refreshState()
            catalogueRepository.refresh()
        }
    }

    fun syncToyPad() {
        toyPadRepository.syncToyPad()
        viewModelScope.launch {
            toyPadRepository.refreshState()
        }
    }

    fun placeToken(token: RemoteToken, index: Int) {
        viewModelScope.launch {
            val position = com.ldtoypad.remote.domain.model.zoneForIndex(index).protocolPosition
            placeTokenUseCase(token.uid, token.id, position, index)
        }
    }

    fun removeToken(token: RemoteToken, index: Int) {
        viewModelScope.launch {
            removeTokenUseCase(token.uid, index)
        }
    }

    fun moveToken(token: RemoteToken, oldIndex: Int, newIndex: Int) {
        viewModelScope.launch {
            moveTokenUseCase(token.uid, token.id, oldIndex, newIndex)
        }
    }
}
