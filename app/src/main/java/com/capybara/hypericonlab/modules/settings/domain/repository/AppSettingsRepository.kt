package com.capybara.hypericonlab.modules.settings.domain.repository

import com.capybara.hypericonlab.modules.settings.domain.model.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class StringSetting {
    ThemeMode,
    ThemePaletteStyle,
    ThemeColorSpec,
    FloatingBottomBarCompactType,
    CardCornerSize,
    LiquidGlassEngine
}

enum class IntSetting {
    ThemeSeedColor,
    LastMainPageIndex
}

enum class FloatSetting {
    KyantGlassBlurScale,
    KyantGlassRefractionHeightScale,
    KyantGlassRefractionAmountScale,
    KyantGlassChromaticAberration
}

enum class BooleanSetting {
    UiUseSmootherRoundedCorners,
    UiUseCustomCardCornerRadius,
    UiUseBlur,
    UiUseLiquidGlassBottomSheet,
    UiUseCustomLiquidGlassEngine,
    ThemeUseDynamicColor,
    UiUseFloatingBottomBar,
    UiUseFloatingBottomBarBlur,
    UiUseFloatingBottomBarCompact,
    UiUseProgressiveBlurTopAppBar,
    UiUseTabRowCenterAlignment,
    UiUseTabRowTransparentBackground,
    UiUseTabRowFillWidth,
    UiUseAppleStyleCard,
    UiUseSheetCardBackground,
    UiUseAppleStyleToggle,
    UiUseAppleStyleSlider,
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

    suspend fun putFloat(setting: FloatSetting, value: Float)
    fun getFloat(setting: FloatSetting, default: Float = 0f): Flow<Float>

    suspend fun putBoolean(setting: BooleanSetting, value: Boolean)
    fun getBoolean(setting: BooleanSetting, default: Boolean = false): Flow<Boolean>
}
