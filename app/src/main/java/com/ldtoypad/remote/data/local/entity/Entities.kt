package com.ldtoypad.remote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val id: Int,
    val timestamp: Long
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val presetId: Long = 0,
    val name: String
)

@Entity(tableName = "preset_slots")
data class PresetSlotEntity(
    @PrimaryKey(autoGenerate = true) val slotId: Long = 0,
    val presetId: Long,
    val padIndex: Int,
    val tokenId: Int
)
