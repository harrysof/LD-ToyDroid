package com.ldtoypad.remote.domain.model

import com.ldtoypad.remote.core.result.AppError

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(
        val baseUrl: String,
        val socketConnected: Boolean,
        val gameDetected: Boolean
    ) : ConnectionState
    data class Reconnecting(val attempt: Int) : ConnectionState
    data class Failed(val error: AppError) : ConnectionState
}
