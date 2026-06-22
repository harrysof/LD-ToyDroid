package com.ldtoypad.remote.domain.model

sealed interface CatalogueItem {
    val id: Int
    val name: String
    val world: String
    val abilities: List<String>
}

data class Character(
    override val id: Int,
    override val name: String,
    override val world: String,
    override val abilities: List<String>
) : CatalogueItem

data class Vehicle(
    override val id: Int,
    override val name: String,
    override val world: String,
    override val abilities: List<String>,
    val upgradeMap: Int,
    val rebuild: Int
) : CatalogueItem
