package com.ldtoypad.remote.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ldtoypad.remote.data.local.entity.FavoriteEntity
import com.ldtoypad.remote.data.local.entity.PresetEntity
import com.ldtoypad.remote.data.local.entity.PresetSlotEntity
import com.ldtoypad.remote.data.local.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites")
    fun getAll(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Delete
    suspend fun delete(favorite: FavoriteEntity)
}

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY timestamp DESC LIMIT 20")
    fun getAll(): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recent: RecentEntity)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity): Long

    @Delete
    suspend fun deletePreset(preset: PresetEntity)

    @Query("SELECT * FROM preset_slots WHERE presetId = :presetId")
    fun getSlotsForPreset(presetId: Long): Flow<List<PresetSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<PresetSlotEntity>)

    @Query("DELETE FROM preset_slots WHERE presetId = :presetId")
    suspend fun deleteSlotsForPreset(presetId: Long)
}
