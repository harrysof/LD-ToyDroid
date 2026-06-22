package com.ldtoypad.remote.data.repository

import com.ldtoypad.remote.data.local.dao.PresetDao
import com.ldtoypad.remote.data.local.entity.PresetEntity
import com.ldtoypad.remote.data.local.entity.PresetSlotEntity

class PresetRepository(private val presetDao: PresetDao) {
    fun getAllPresets() = presetDao.getAllPresets()

    fun getSlotsForPreset(presetId: Long) = presetDao.getSlotsForPreset(presetId)

    suspend fun savePreset(name: String, slots: List<PresetSlotEntity>) {
        val presetId = presetDao.insertPreset(PresetEntity(name = name))
        presetDao.insertSlots(slots.map { it.copy(presetId = presetId) })
    }

    suspend fun deletePreset(preset: PresetEntity) {
        presetDao.deleteSlotsForPreset(preset.presetId)
        presetDao.deletePreset(preset)
    }
}
