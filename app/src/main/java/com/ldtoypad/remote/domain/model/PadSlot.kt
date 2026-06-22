package com.ldtoypad.remote.domain.model

data class PadSlot(
    val index: Int,
    val zone: PadZone,
    val token: RemoteToken?
)
