package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.repository.ToyPadRepository
import kotlinx.coroutines.delay

class PlaceToken(
    private val toyPadRepository: ToyPadRepository
) {
    suspend operator fun invoke(uid: String, id: Int, position: Int, index: Int) {
        toyPadRepository.placeToken(uid, id, position, index)
        // Ensure state is fetched after mutation
        delay(300)
        toyPadRepository.refreshState()
    }
}
