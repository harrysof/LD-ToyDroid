package com.ldtoypad.remote.domain.model

import java.time.Instant

data class ToyPadState(
    val slots: List<PadSlot>,
    val toyBoxTokens: List<RemoteToken>,
    val allTokens: List<RemoteToken>,
    val lastUpdatedAt: Instant
)
