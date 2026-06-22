package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.repository.ToyPadRepository
import kotlinx.coroutines.delay

class RemoveToken(
    private val toyPadRepository: ToyPadRepository
) {
    suspend operator fun invoke(uid: String, index: Int) {
        toyPadRepository.removeToken(uid, index)
        delay(300)
        toyPadRepository.refreshState()
    }
}
