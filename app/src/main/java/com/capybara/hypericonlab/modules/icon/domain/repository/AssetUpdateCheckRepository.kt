package com.capybara.hypericonlab.modules.icon.domain.repository

import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState

enum class AssetUpdateCheckTrigger {
    AUTOMATIC,
    MANUAL
}

data class AssetUpdateCheckRecord(
    val lastAutomaticCheckAt: Long? = null,
    val lastManualCheckAt: Long? = null,
    val state: AssetUpdateCheckState = AssetUpdateCheckState.Idle
)

interface AssetUpdateCheckRepository {
    suspend fun read(): AssetUpdateCheckRecord

    suspend fun save(record: AssetUpdateCheckRecord)
}
