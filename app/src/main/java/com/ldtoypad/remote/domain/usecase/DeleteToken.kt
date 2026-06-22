package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.repository.ToyPadRepository

class DeleteToken(
    private val toyPadRepository: ToyPadRepository
) {
    operator fun invoke(uid: String) {
        toyPadRepository.deleteToken(uid)
    }
}
