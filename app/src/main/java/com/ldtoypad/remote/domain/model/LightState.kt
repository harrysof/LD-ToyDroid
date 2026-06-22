package com.ldtoypad.remote.domain.model

import androidx.compose.ui.graphics.Color

data class ZoneLight(
    val steadyColor: Color,
    val displayedColor: Color,
    val animationJobId: Long?
)

data class ToyPadLights(
    val center: ZoneLight,
    val left: ZoneLight,
    val right: ZoneLight
)
