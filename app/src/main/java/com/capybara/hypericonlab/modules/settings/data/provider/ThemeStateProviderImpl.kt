package com.capybara.hypericonlab.modules.settings.data.provider

import android.os.Build
import androidx.compose.ui.graphics.Color
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.domain.provider.SystemEnvProvider
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ThemeStateProviderImpl(
    appSettingsRepo: AppSettingsRepository,
    private val systemEnvProvider: SystemEnvProvider,
    appScope: CoroutineScope
) : ThemeStateProvider {
    override val themeStateFlow: StateFlow<ThemeState> = combine(
        appSettingsRepo.preferencesFlow,
        systemEnvProvider.getWallpaperColorsFlow()
    ) { prefs, wallpaperColors ->
        val manualSeedColor = Color(prefs.seedColorInt)
        val effectiveSeedColor =
            if (prefs.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) Color(wallpaperColors[0]) else manualSeedColor
            } else manualSeedColor

        ThemeState(
            isLoaded = true,
            themeMode = prefs.themeMode,
            paletteStyle = prefs.paletteStyle,
            colorSpec = prefs.colorSpec,
            useDynamicColor = prefs.useDynamicColor,
            useFloatingBottomBar = prefs.useFloatingBottomBar,
            useFloatingBottomBarBlur = prefs.useFloatingBottomBarBlur,
            useFloatingBottomBarCompact = prefs.useFloatingBottomBarCompact,
            floatingBottomBarCompactType = prefs.floatingBottomBarCompactType,
            seedColor = effectiveSeedColor,
            useSmootherRoundedCorners = prefs.useSmootherRoundedCorners,
            useBlur = prefs.useBlur,
            useProgressiveBlurTopAppBar = prefs.useProgressiveBlurTopAppBar,
            useTabRowCenterAlignment = prefs.useTabRowCenterAlignment,
            useTabRowTransparentBackground = prefs.useTabRowTransparentBackground,
            useTabRowFillWidth = prefs.useTabRowFillWidth,
            useLiquidGlassBottomSheet = prefs.useLiquidGlassBottomSheet,
            liquidGlassBlurRadius = prefs.liquidGlassBlurRadius,
            useGoogleSansFlex = prefs.useGoogleSansFlex,
        )
    }.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeState()
    )
}
