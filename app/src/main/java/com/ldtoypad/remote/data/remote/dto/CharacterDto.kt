package com.ldtoypad.remote.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterDto(
    val id: Int,
    val name: String,
    val world: String,
    val abilities: String
)
