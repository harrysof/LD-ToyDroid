package com.ldtoypad.remote.domain.usecase

import com.ldtoypad.remote.data.local.entity.PresetSlotEntity
import com.ldtoypad.remote.data.repository.PresetRepository

class SavePreset(
    private val presetRepository: PresetRepository
) {
    suspend operator fun invoke(name: String, slots: List<PresetSlotEntity>) {
        presetRepository.savePreset(name, slots)
    }
}
