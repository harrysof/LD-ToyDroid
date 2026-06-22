package com.ldtoypad.remote.domain.model

enum class PadZone(val protocolPosition: Int) {
    CENTER(1),
    LEFT(2),
    RIGHT(3)
}

fun zoneForIndex(index: Int): PadZone = when (index) {
    2 -> PadZone.CENTER
    1, 4, 5 -> PadZone.LEFT
    3, 6, 7 -> PadZone.RIGHT
    else -> error("Invalid pad index: $index")
}
