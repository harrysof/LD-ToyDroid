package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.repository.ConnectionRepository

class ConnectToServer(
    private val connectionRepository: ConnectionRepository
) {
    suspend operator fun invoke(baseUrl: String, remember: Boolean) {
        connectionRepository.connect(baseUrl, remember)
    }
}
