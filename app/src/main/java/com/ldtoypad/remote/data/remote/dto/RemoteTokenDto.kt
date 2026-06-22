package com.ldtoypad.remote.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RemoteTokenDto(
    val name: String? = null,
    val id: Int? = null,
    val uid: String,
    @Serializable(with = FlexibleIntSerializer::class)
    val index: Int = -1,
    val type: String? = null,
    val vehicleUpgradesP23: Long = 0,
    val vehicleUpgradesP25: Long = 0
)
