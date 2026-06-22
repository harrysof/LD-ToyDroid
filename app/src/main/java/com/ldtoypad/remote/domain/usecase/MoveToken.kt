package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.domain.model.zoneForIndex

class MoveToken(
    private val removeToken: RemoveToken,
    private val placeToken: PlaceToken
) {
    suspend operator fun invoke(uid: String, id: Int, oldIndex: Int, newIndex: Int) {
        removeToken(uid, oldIndex)
        placeToken(uid, id, zoneForIndex(newIndex).protocolPosition, newIndex)
    }
}
