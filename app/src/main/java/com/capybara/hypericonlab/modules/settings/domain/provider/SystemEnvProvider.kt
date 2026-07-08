package com.capybara.hypericonlab.modules.settings.domain.provider

import kotlinx.coroutines.flow.Flow

interface SystemEnvProvider {
    fun getWallpaperColorsFlow(): Flow<List<Int>?>
}
