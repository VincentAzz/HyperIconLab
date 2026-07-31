package com.capybara.hypericonlab.modules.settings.domain.repository

import com.capybara.hypericonlab.modules.settings.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class StringSetting {
    ThemeMode,
    ThemePaletteStyle,
    ThemeColorSpec,
    FloatingBottomBarCompactType
}

enum class IntSetting {
    ThemeSeedColor,
    LastMainPageIndex,
    UiLiquidGlassBlurRadius
}

enum class BooleanSetting {
    UiUseSmootherRoundedCorners,
    UiUseBlur,
    UiUseLiquidGlassBottomSheet,
    ThemeUseDynamicColor,
    UiUseFloatingBottomBar,
    UiUseFloatingBottomBarBlur,
    UiUseFloatingBottomBarCompact,
    UiUseProgressiveBlurTopAppBar,
    UiUseTabRowCenterAlignment,
    UiUseTabRowTransparentBackground,
    UiUseTabRowFillWidth,
    UiUseGoogleSansFlex,
    UiUseDownloadProxy
}

interface AppSettingsRepository {
    val preferencesFlow: Flow<AppPreferences>
    val useDownloadProxy: StateFlow<Boolean>

    suspend fun putString(setting: StringSetting, value: String)
    fun getString(setting: StringSetting, default: String = ""): Flow<String>

    suspend fun putInt(setting: IntSetting, value: Int)
    fun getInt(setting: IntSetting, default: Int = 0): Flow<Int>

    suspend fun putBoolean(setting: BooleanSetting, value: Boolean)
    fun getBoolean(setting: BooleanSetting, default: Boolean = false): Flow<Boolean>
}
