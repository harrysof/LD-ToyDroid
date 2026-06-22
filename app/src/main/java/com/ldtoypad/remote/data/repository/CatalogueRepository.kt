package com.ldtoypad.remote.data.repository

import com.ldtoypad.remote.data.remote.EmulatorHttpClient
import com.ldtoypad.remote.domain.model.CatalogueItem
import com.ldtoypad.remote.domain.model.Character
import com.ldtoypad.remote.domain.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CatalogueRepository(
    private val httpClient: EmulatorHttpClient
) {
    private val _items = MutableStateFlow<List<CatalogueItem>>(emptyList())
    val items = _items.asStateFlow()

    suspend fun refresh() {
        val charsResult = httpClient.getCharacterMap()
        val vehsResult = httpClient.getVehicleMap()

        val chars = charsResult.getOrNull()?.map { dto ->
            Character(
                id = dto.id,
                name = dto.name,
                world = dto.world,
                abilities = parseAbilities(dto.abilities)
            )
        } ?: emptyList()

        val vehicles = vehsResult.getOrNull()?.map { dto ->
            Vehicle(
                id = dto.id,
                name = dto.name,
                world = dto.world,
                abilities = parseAbilities(dto.abilities),
                upgradeMap = dto.upgradeMap,
                rebuild = dto.rebuild
            )
        } ?: emptyList()

        _items.value = chars + vehicles
    }

    private fun parseAbilities(abilitiesStr: String): List<String> {
        return abilitiesStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
