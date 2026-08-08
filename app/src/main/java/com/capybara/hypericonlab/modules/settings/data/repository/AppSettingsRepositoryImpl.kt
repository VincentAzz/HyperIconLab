package com.capybara.hypericonlab.modules.settings.data.repository

import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.Preferences
import com.capybara.hypericonlab.core.designsystem.liquidglass.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerSize
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.modules.settings.data.local.AppDataStore
import com.capybara.hypericonlab.modules.settings.domain.model.AppPreferences
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import com.capybara.hypericonlab.modules.settings.domain.repository.BooleanSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.IntSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.StringSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class AppSettingsRepositoryImpl(
    private val appDataStore: AppDataStore, appScope: CoroutineScope
) : AppSettingsRepository {

    override val preferencesFlow: Flow<AppPreferences> = appDataStore.data.map { prefs ->
        AppPreferences(
            useSmootherRoundedCorners = prefs[AppDataStore.UI_USE_SMOOTHER_ROUNDED_CORNERS]
                ?: true,
            useCustomCardCornerRadius = prefs[AppDataStore.UI_USE_CUSTOM_CARD_CORNER_RADIUS]
                ?: false,
            cardCornerSize = CardCornerSize.fromValueOrDefault(
                prefs[AppDataStore.UI_CARD_CORNER_SIZE] ?: CardCornerSize.DEFAULT.name
            ),
            useBlur = prefs[AppDataStore.UI_USE_BLUR]
                ?: true,
            useProgressiveBlurTopAppBar = prefs[AppDataStore.UI_USE_PROGRESSIVE_BLUR_TOP_APP_BAR]
                ?: true,
            useTabRowCenterAlignment = prefs[AppDataStore.UI_USE_TAB_ROW_CENTER_ALIGNMENT]
                ?: true,
            useTabRowTransparentBackground = prefs[AppDataStore.UI_USE_TAB_ROW_TRANSPARENT_BACKGROUND]
                ?: false,
            useTabRowFillWidth = prefs[AppDataStore.UI_USE_TAB_ROW_FILL_WIDTH]
                ?: false,
            useLiquidGlassBottomSheet = prefs[AppDataStore.UI_USE_LIQUID_GLASS_BOTTOM_SHEET]
                ?: false,
            liquidGlassEngine = LiquidGlassEngine.fromValueOrDefault(
                prefs[AppDataStore.UI_LIQUID_GLASS_ENGINE] ?: LiquidGlassEngine.MIUIX.name
            ),
            useAppleStyleCard = prefs[AppDataStore.UI_USE_APPLE_STYLE_CARD]
                ?: false,
            useGoogleSansFlex = prefs[AppDataStore.UI_USE_GOOGLE_SANS_FLEX]
                ?: true,
            themeMode = ThemeMode.fromValueOrDefault(
                prefs[AppDataStore.THEME_MODE]
                    ?: ThemeMode.LIGHT.name
            ),
            paletteStyle = PaletteStyle.fromValueOrDefault(
                prefs[AppDataStore.THEME_PALETTE_STYLE]
                    ?: PaletteStyle.Monochrome.name
            ),
            colorSpec = ThemeColorSpec.fromValueOrDefault(
                prefs[AppDataStore.THEME_COLOR_SPEC]
                    ?: ThemeColorSpec.SPEC_2025.name
            ),
            useDynamicColor = prefs[AppDataStore.THEME_USE_DYNAMIC_COLOR]
                ?: true,
            useFloatingBottomBar = prefs[AppDataStore.UI_USE_FLOATING_BOTTOM_BAR]
                ?: true,
            useFloatingBottomBarBlur = prefs[AppDataStore.UI_USE_FLOATING_BOTTOM_BAR_BLUR]
                ?: true,
            useFloatingBottomBarCompact = prefs[AppDataStore.UI_USE_FLOATING_BAR_COMPACT]
                ?: true,
            floatingBottomBarCompactType = FloatingBottomBarCompactType.fromValueOrDefault(
                prefs[AppDataStore.UI_FLOATING_BAR_COMPACT_TYPE]
                    ?: FloatingBottomBarCompactType.MIXED_ICON.name
            ),
            seedColorInt = prefs[AppDataStore.THEME_SEED_COLOR]
                ?: PresetColors.first().color.toArgb(),
            useDownloadProxy = prefs[AppDataStore.UI_USE_DOWNLOAD_PROXY]
                ?: true,
            lastMainPageIndex = prefs[AppDataStore.LAST_MAIN_PAGE_INDEX]
                ?: 3
        )
    }.shareIn(
        scope = appScope, started = SharingStarted.Eagerly, replay = 1
    )

    override val useDownloadProxy: StateFlow<Boolean> = preferencesFlow
        .map { it.useDownloadProxy }
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    override suspend fun putString(setting: StringSetting, value: String) =
        appDataStore.putString(stringKey(setting), value)

    override fun getString(setting: StringSetting, default: String): Flow<String> =
        appDataStore.getString(stringKey(setting), default)

    override suspend fun putInt(setting: IntSetting, value: Int) =
        appDataStore.putInt(intKey(setting), value)

    override fun getInt(setting: IntSetting, default: Int): Flow<Int> =
        appDataStore.getInt(intKey(setting), default)

    override suspend fun putBoolean(setting: BooleanSetting, value: Boolean) =
        appDataStore.putBoolean(booleanKey(setting), value)

    override fun getBoolean(setting: BooleanSetting, default: Boolean): Flow<Boolean> =
        appDataStore.getBoolean(booleanKey(setting), default)

    private fun stringKey(setting: StringSetting): Preferences.Key<String> = when (setting) {
        StringSetting.ThemeMode -> AppDataStore.THEME_MODE
        StringSetting.ThemePaletteStyle -> AppDataStore.THEME_PALETTE_STYLE
        StringSetting.ThemeColorSpec -> AppDataStore.THEME_COLOR_SPEC
        StringSetting.FloatingBottomBarCompactType -> AppDataStore.UI_FLOATING_BAR_COMPACT_TYPE
        StringSetting.CardCornerSize -> AppDataStore.UI_CARD_CORNER_SIZE
        StringSetting.LiquidGlassEngine -> AppDataStore.UI_LIQUID_GLASS_ENGINE
    }

    private fun intKey(setting: IntSetting): Preferences.Key<Int> = when (setting) {
        IntSetting.ThemeSeedColor -> AppDataStore.THEME_SEED_COLOR
        IntSetting.LastMainPageIndex -> AppDataStore.LAST_MAIN_PAGE_INDEX
    }

    private fun booleanKey(setting: BooleanSetting): Preferences.Key<Boolean> = when (setting) {
        BooleanSetting.UiUseSmootherRoundedCorners -> AppDataStore.UI_USE_SMOOTHER_ROUNDED_CORNERS
        BooleanSetting.UiUseCustomCardCornerRadius -> AppDataStore.UI_USE_CUSTOM_CARD_CORNER_RADIUS
        BooleanSetting.UiUseBlur -> AppDataStore.UI_USE_BLUR
        BooleanSetting.UiUseLiquidGlassBottomSheet -> AppDataStore.UI_USE_LIQUID_GLASS_BOTTOM_SHEET
        BooleanSetting.ThemeUseDynamicColor -> AppDataStore.THEME_USE_DYNAMIC_COLOR
        BooleanSetting.UiUseFloatingBottomBar -> AppDataStore.UI_USE_FLOATING_BOTTOM_BAR
        BooleanSetting.UiUseFloatingBottomBarBlur -> AppDataStore.UI_USE_FLOATING_BOTTOM_BAR_BLUR
        BooleanSetting.UiUseFloatingBottomBarCompact -> AppDataStore.UI_USE_FLOATING_BAR_COMPACT
        BooleanSetting.UiUseProgressiveBlurTopAppBar -> AppDataStore.UI_USE_PROGRESSIVE_BLUR_TOP_APP_BAR
        BooleanSetting.UiUseTabRowCenterAlignment -> AppDataStore.UI_USE_TAB_ROW_CENTER_ALIGNMENT
        BooleanSetting.UiUseTabRowTransparentBackground -> AppDataStore.UI_USE_TAB_ROW_TRANSPARENT_BACKGROUND
        BooleanSetting.UiUseTabRowFillWidth -> AppDataStore.UI_USE_TAB_ROW_FILL_WIDTH
        BooleanSetting.UiUseAppleStyleCard -> AppDataStore.UI_USE_APPLE_STYLE_CARD
        BooleanSetting.UiUseGoogleSansFlex -> AppDataStore.UI_USE_GOOGLE_SANS_FLEX
        BooleanSetting.UiUseDownloadProxy -> AppDataStore.UI_USE_DOWNLOAD_PROXY
    }
}
