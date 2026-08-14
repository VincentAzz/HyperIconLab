package com.capybara.hypericonlab.modules.settings.ui.page.settings

import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.color.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.color.material.RawColor
import com.capybara.hypericonlab.core.designsystem.config.CardCornerSize
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsAction
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsState
import com.capybara.hypericonlab.modules.settings.domain.provider.KyantGlassTuningController
import com.capybara.hypericonlab.modules.settings.domain.provider.SystemEnvProvider
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import com.capybara.hypericonlab.modules.settings.domain.repository.BooleanSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.FloatSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.IntSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.StringSetting
import com.capybara.hypericonlab.modules.settings.domain.usecase.UpdateSettingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalStdlibApi::class)
class SettingsViewModel(
    themeStateProvider: ThemeStateProvider,
    systemEnvProvider: SystemEnvProvider,
    private val updateSetting: UpdateSettingUseCase,
    private val kyantGlassTuningController: KyantGlassTuningController
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val state: StateFlow<ThemeSettingsState> = combine(
        themeStateProvider.themeStateFlow,
        systemEnvProvider.getWallpaperColorsFlow().onStart { emit(emptyList()) },
        _selectedTab
    ) { themeState, wallpaperColors, selectedTab ->
        val manualSeedColor = themeState.seedColor
        val effectiveSeedColor: Color =
            if (themeState.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) {
                    if (wallpaperColors.contains(manualSeedColor.toArgb())) {
                        manualSeedColor
                    } else Color(wallpaperColors[0])
                } else manualSeedColor
            } else {
                if (PresetColors.any { it.color == manualSeedColor }) manualSeedColor else PresetColors[0].color
            }

        val availableColors: List<RawColor> =
            if (themeState.useDynamicColor && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (!wallpaperColors.isNullOrEmpty()) {
                    wallpaperColors.map { colorInt ->
                        RawColor(key = colorInt.toHexString(), color = Color(colorInt))
                    }
                } else PresetColors
            } else PresetColors

        ThemeSettingsState(
            useSmootherRoundedCorners = themeState.useSmootherRoundedCorners,
            useCustomCardCornerRadius = themeState.useCustomCardCornerRadius,
            cardCornerSize = themeState.cardCornerSize,
            useBlur = themeState.useBlur,
            useProgressiveBlurTopAppBar = themeState.useProgressiveBlurTopAppBar,
            useTabRowCenterAlignment = themeState.useTabRowCenterAlignment,
            useTabRowTransparentBackground = themeState.useTabRowTransparentBackground,
            useTabRowFillWidth = themeState.useTabRowFillWidth,
            useLiquidGlassBottomSheet = themeState.useLiquidGlassBottomSheet,
            useCustomLiquidGlassEngine = themeState.useCustomLiquidGlassEngine,
            liquidGlassEngine = themeState.liquidGlassEngine,
            kyantGlassTuning = themeState.kyantGlassTuning,
            useAppleStyleCard = themeState.useAppleStyleCard,
            useSheetCardBackground = themeState.useSheetCardBackground,
            useAppleStyleToggle = themeState.useAppleStyleToggle,
            useAppleStyleSlider = themeState.useAppleStyleSlider,
            useGoogleSansFlex = themeState.useGoogleSansFlex,
            themeMode = themeState.themeMode,
            paletteStyle = themeState.paletteStyle,
            colorSpec = themeState.colorSpec,
            useDynamicColor = themeState.useDynamicColor,
            useFloatingBottomBar = themeState.useFloatingBottomBar,
            useFloatingBottomBarBlur = themeState.useFloatingBottomBarBlur,
            useFloatingBottomBarCompact = themeState.useFloatingBottomBarCompact,
            floatingBottomBarCompactType = themeState.floatingBottomBarCompactType,
            seedColor = effectiveSeedColor,
            availableColors = availableColors,
            selectedTab = selectedTab,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ThemeSettingsState()
    )

    fun dispatch(action: ThemeSettingsAction) {
        when (action) {
            is ThemeSettingsAction.SetUseSmootherRoundedCorners -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseSmootherRoundedCorners,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseCustomCardCornerRadius -> viewModelScope.launch {
                if (!action.enable) {
                    updateSetting(
                        StringSetting.CardCornerSize,
                        CardCornerSize.DEFAULT.name
                    )
                }
                updateSetting(
                    BooleanSetting.UiUseCustomCardCornerRadius,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetCardCornerSize -> viewModelScope.launch {
                updateSetting(
                    StringSetting.CardCornerSize,
                    action.size.name
                )
            }

            is ThemeSettingsAction.SetUseBlur -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseBlur,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseProgressiveBlurTopAppBar -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseProgressiveBlurTopAppBar,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseTabRowCenterAlignment -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseTabRowCenterAlignment,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseTabRowTransparentBackground -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseTabRowTransparentBackground,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseTabRowFillWidth -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseTabRowFillWidth,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseLiquidGlassBottomSheet -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseLiquidGlassBottomSheet,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseCustomLiquidGlassEngine -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseCustomLiquidGlassEngine,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetLiquidGlassEngine -> viewModelScope.launch {
                updateSetting(
                    StringSetting.LiquidGlassEngine,
                    action.engine.name
                )
                if (action.engine == LiquidGlassEngine.MIUIX) {
                    updateSetting(BooleanSetting.UiUseAppleStyleToggle, false)
                    updateSetting(BooleanSetting.UiUseAppleStyleSlider, false)
                }
            }

            is ThemeSettingsAction.PreviewKyantGlassTuning -> {
                kyantGlassTuningController.updatePreview(action.tuning)
            }

            is ThemeSettingsAction.PreviewKyantGlassTuningParameter -> {
                kyantGlassTuningController.updatePreview(
                    parameter = action.parameter,
                    value = action.value,
                    fallback = state.value.kyantGlassTuning
                )
            }

            ThemeSettingsAction.PersistKyantGlassTuning -> viewModelScope.launch {
                persistKyantGlassTuning(
                    kyantGlassTuningController.currentOr(state.value.kyantGlassTuning)
                )
            }

            ThemeSettingsAction.ClearKyantGlassTuningPreview -> {
                kyantGlassTuningController.clearPreview()
            }

            is ThemeSettingsAction.SetUseAppleStyleCard -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseAppleStyleCard,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseSheetCardBackground -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseSheetCardBackground,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseAppleStyleToggle -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseAppleStyleToggle,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseAppleStyleSlider -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseAppleStyleSlider,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetUseGoogleSansFlex -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseGoogleSansFlex,
                    action.enable
                )
            }

            is ThemeSettingsAction.SetThemeMode -> viewModelScope.launch {
                updateSetting(
                    StringSetting.ThemeMode,
                    action.mode.name
                )
            }

            is ThemeSettingsAction.SetPaletteStyle -> viewModelScope.launch {
                updateSetting(
                    StringSetting.ThemePaletteStyle,
                    action.style.name
                )
            }

            is ThemeSettingsAction.SetColorSpec -> viewModelScope.launch {
                updateSetting(
                    StringSetting.ThemeColorSpec,
                    action.spec.name
                )
            }

            is ThemeSettingsAction.SetUseDynamicColor -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.ThemeUseDynamicColor,
                    action.use
                )
            }

            is ThemeSettingsAction.SetUseFloatingBottomBar -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseFloatingBottomBar,
                    action.use
                )
            }

            is ThemeSettingsAction.SetUseFloatingBottomBarBlur -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseFloatingBottomBarBlur,
                    action.use
                )
            }

            is ThemeSettingsAction.SetUseFloatingBarCompact -> viewModelScope.launch {
                updateSetting(
                    BooleanSetting.UiUseFloatingBottomBarCompact,
                    action.use
                )
            }

            is ThemeSettingsAction.SetFloatingBottomBarCompactType -> viewModelScope.launch {
                updateSetting(
                    StringSetting.FloatingBottomBarCompactType,
                    action.type.name
                )
            }

            is ThemeSettingsAction.SetSeedColor -> viewModelScope.launch {
                updateSetting(
                    IntSetting.ThemeSeedColor,
                    action.color.toArgb()
                )
            }

            is ThemeSettingsAction.SetSelectedTab -> {
                _selectedTab.value = action.index
            }
        }
    }

    private suspend fun persistKyantGlassTuning(tuning: KyantGlassTuning) {
        val normalized = tuning.normalized()
        updateSetting(FloatSetting.KyantGlassBlurScale, normalized.blurScale)
        updateSetting(
            FloatSetting.KyantGlassRefractionHeightScale,
            normalized.refractionHeightScale
        )
        updateSetting(
            FloatSetting.KyantGlassRefractionAmountScale,
            normalized.refractionAmountScale
        )
        updateSetting(
            FloatSetting.KyantGlassChromaticAberration,
            normalized.chromaticAberration
        )
    }
}
