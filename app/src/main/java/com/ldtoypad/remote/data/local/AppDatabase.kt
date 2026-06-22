package com.ldtoypad.remote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ldtoypad.remote.data.local.dao.FavoriteDao
import com.ldtoypad.remote.data.local.dao.PresetDao
import com.ldtoypad.remote.data.local.dao.RecentDao
import com.ldtoypad.remote.data.local.entity.FavoriteEntity
import com.ldtoypad.remote.data.local.entity.PresetEntity
import com.ldtoypad.remote.data.local.entity.PresetSlotEntity
import com.ldtoypad.remote.data.local.entity.RecentEntity

@Database(
    entities = [
        FavoriteEntity::class,
        RecentEntity::class,
        PresetEntity::class,
        PresetSlotEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun presetDao(): PresetDao
}
