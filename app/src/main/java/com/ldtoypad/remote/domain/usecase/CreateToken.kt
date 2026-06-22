package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.repository.ToyPadRepository
import com.ldtoypad.remote.domain.model.TokenType
import kotlinx.coroutines.delay

class CreateToken(
    private val toyPadRepository: ToyPadRepository
) {
    suspend operator fun invoke(id: Int, type: TokenType) {
        if (type == TokenType.CHARACTER) {
            toyPadRepository.createCharacter(id)
        } else {
            toyPadRepository.createVehicle(id)
        }
        
        // Wait and refresh to get the new token
        delay(500)
        toyPadRepository.refreshState()
    }
}
