package com.ldtoypad.remote.data.repository

import com.ldtoypad.remote.data.remote.EmulatorHttpClient
import com.ldtoypad.remote.data.remote.EmulatorSocketClient
import com.ldtoypad.remote.domain.model.PadSlot
import com.ldtoypad.remote.domain.model.RemoteToken
import com.ldtoypad.remote.domain.model.TokenType
import com.ldtoypad.remote.domain.model.ToyPadState
import com.ldtoypad.remote.domain.model.zoneForIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class ToyPadRepository(
    private val httpClient: EmulatorHttpClient,
    private val socketClient: EmulatorSocketClient,
    private val scope: CoroutineScope
) {
    private val _padState = MutableStateFlow(
        ToyPadState(
            slots = (1..7).map { PadSlot(it, zoneForIndex(it), null) },
            toyBoxTokens = emptyList(),
            allTokens = emptyList(),
            lastUpdatedAt = Instant.now()
        )
    )
    val padState = _padState.asStateFlow()

    init {
        scope.launch {
            socketClient.refreshTokensEvent.collect {
                refreshState()
            }
        }
    }

    suspend fun refreshState() {
        val tokensResult = httpClient.getTokens()
        val dtos = tokensResult.getOrNull() ?: return

        val tokens = dtos.map { dto ->
            val type = when (dto.type?.lowercase()) {
                "character" -> TokenType.CHARACTER
                "vehicle" -> TokenType.VEHICLE
                else -> {
                    if ((dto.id ?: 0) >= 1000) TokenType.VEHICLE else TokenType.CHARACTER
                }
            }

            RemoteToken(
                uid = dto.uid,
                id = dto.id ?: 0,
                name = dto.name ?: "Unknown",
                type = type,
                padIndex = if (dto.index in 1..7) dto.index else null,
                vehicleUpgradesP23 = dto.vehicleUpgradesP23,
                vehicleUpgradesP25 = dto.vehicleUpgradesP25
            )
        }

        val slots = (1..7).map { index ->
            val tokenOnSlot = tokens.find { it.padIndex == index }
            PadSlot(index, zoneForIndex(index), tokenOnSlot)
        }

        val toyBox = tokens.filter { it.padIndex == null }

        _padState.value = ToyPadState(
            slots = slots,
            toyBoxTokens = toyBox,
            allTokens = tokens,
            lastUpdatedAt = Instant.now()
        )
    }

    suspend fun createCharacter(id: Int) = httpClient.createCharacter(id)
    suspend fun createVehicle(id: Int) = httpClient.createVehicle(id)
    suspend fun placeToken(uid: String, id: Int, position: Int, index: Int) = httpClient.placeToken(uid, id, position, index)
    suspend fun removeToken(uid: String, index: Int) = httpClient.removeToken(uid, index)
    
    fun syncToyPad() = socketClient.syncToyPad()
    fun deleteToken(uid: String) = socketClient.deleteToken(uid)
}
