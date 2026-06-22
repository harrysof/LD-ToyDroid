package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.repository.PresetRepository
import com.ldtoypad.remote.data.repository.ToyPadRepository
import com.ldtoypad.remote.domain.model.zoneForIndex
import kotlinx.coroutines.flow.first

class LoadPreset(
    private val presetRepository: PresetRepository,
    private val toyPadRepository: ToyPadRepository
) {
    suspend operator fun invoke(presetId: Long) {
        val slots = presetRepository.getSlotsForPreset(presetId).first()
        val allTokens = toyPadRepository.padState.value.allTokens

        for (slot in slots) {
            val token = allTokens.find { it.id == slot.tokenId }
            if (token != null) {
                val currentPadIndex = token.padIndex
                if (currentPadIndex != null) {
                    toyPadRepository.removeToken(token.uid, currentPadIndex)
                }
                toyPadRepository.placeToken(
                    token.uid,
                    token.id,
                    zoneForIndex(slot.padIndex).protocolPosition,
                    slot.padIndex
                )
            }
        }
        toyPadRepository.refreshState()
    }
}
