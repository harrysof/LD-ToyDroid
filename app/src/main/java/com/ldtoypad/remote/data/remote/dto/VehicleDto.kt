package com.ldtoypad.remote.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleDto(
    val id: Int,
    val name: String,
    val world: String,
    val abilities: String,
    @SerialName("upgrademap")
    val upgradeMap: Int = 0,
    val rebuild: Int = 0
)
