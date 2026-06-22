package com.ldtoypad.remote.domain.model

enum class TokenType {
    CHARACTER,
    VEHICLE,
    UNKNOWN
}

data class RemoteToken(
    val uid: String,
    val id: Int,
    val name: String,
    val type: TokenType,
    val padIndex: Int?,
    val vehicleUpgradesP23: Long,
    val vehicleUpgradesP25: Long
)
