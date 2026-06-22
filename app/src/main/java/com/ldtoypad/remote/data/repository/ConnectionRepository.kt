package com.ldtoypad.remote.data.repository

import com.ldtoypad.remote.core.result.AppError
import com.ldtoypad.remote.data.local.settings.ConnectionSettingsStore
import com.ldtoypad.remote.data.remote.EmulatorHttpClient
import com.ldtoypad.remote.data.remote.EmulatorSocketClient
import com.ldtoypad.remote.domain.model.ConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class ConnectionRepository(
    private val httpClient: EmulatorHttpClient,
    private val socketClient: EmulatorSocketClient,
    private val settingsStore: ConnectionSettingsStore
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    val socketConnected: Flow<Boolean> = socketClient.isConnected

    suspend fun getSavedBaseUrl(): String? {
        return settingsStore.baseUrl.first()
    }

    suspend fun connect(baseUrl: String, remember: Boolean) {
        _connectionState.value = ConnectionState.Connecting
        
        httpClient.baseUrl = baseUrl
        
        // 1. Verify HTTP connection
        val charRes = httpClient.getCharacterMap()
        if (charRes.isFailure) {
            _connectionState.value = ConnectionState.Failed(
                charRes.exceptionOrNull() as? AppError ?: AppError.Unknown(charRes.exceptionOrNull())
            )
            return
        }

        val vehRes = httpClient.getVehicleMap()
        if (vehRes.isFailure) {
            _connectionState.value = ConnectionState.Failed(
                vehRes.exceptionOrNull() as? AppError ?: AppError.Unknown(vehRes.exceptionOrNull())
            )
            return
        }

        // 2. HTTP successful, save connection if needed
        if (remember) {
            settingsStore.saveConnection(baseUrl, true)
        } else {
            settingsStore.clearConnection()
        }

        // 3. Connect Socket.IO
        socketClient.connect(baseUrl)

        // Assume game detection will be updated via socket events
        _connectionState.value = ConnectionState.Connected(
            baseUrl = baseUrl,
            socketConnected = false, // Will be updated by flow
            gameDetected = false // Will be updated by socket
        )
    }

    fun disconnect() {
        socketClient.disconnect()
        httpClient.baseUrl = ""
        _connectionState.value = ConnectionState.Disconnected
    }
}
