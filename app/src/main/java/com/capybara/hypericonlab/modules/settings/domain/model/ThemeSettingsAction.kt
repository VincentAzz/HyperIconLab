package com.capybara.hypericonlab.modules.settings.domain.model

import androidx.compose.ui.graphics.Color
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerSize
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode

sealed interface ThemeSettingsAction {
    data class SetUseSmootherRoundedCorners(val enable: Boolean) : ThemeSettingsAction
    data class SetUseCustomCardCornerRadius(val enable: Boolean) : ThemeSettingsAction
    data class SetCardCornerSize(val size: CardCornerSize) : ThemeSettingsAction
    data class SetUseBlur(val enable: Boolean) : ThemeSettingsAction
    data class SetUseProgressiveBlurTopAppBar(val enable: Boolean) : ThemeSettingsAction
    data class SetUseTabRowCenterAlignment(val enable: Boolean) : ThemeSettingsAction
    data class SetUseTabRowTransparentBackground(val enable: Boolean) : ThemeSettingsAction
    data class SetUseTabRowFillWidth(val enable: Boolean) : ThemeSettingsAction
    data class SetUseLiquidGlassBottomSheet(val enable: Boolean) : ThemeSettingsAction
    data class SetLiquidGlassBlurRadius(val value: Int) : ThemeSettingsAction
    data class SetUseAppleStyleCard(val enable: Boolean) : ThemeSettingsAction
    data class SetUseGoogleSansFlex(val enable: Boolean) : ThemeSettingsAction
    data class SetThemeMode(val mode: ThemeMode) : ThemeSettingsAction
    data class SetPaletteStyle(val style: PaletteStyle) : ThemeSettingsAction
    data class SetColorSpec(val spec: ThemeColorSpec) : ThemeSettingsAction
    data class SetUseDynamicColor(val use: Boolean) : ThemeSettingsAction

    data class SetUseFloatingBottomBar(val use: Boolean) : ThemeSettingsAction
    data class SetUseFloatingBottomBarBlur(val use: Boolean) : ThemeSettingsAction
    data class SetUseFloatingBarCompact(val use: Boolean) : ThemeSettingsAction
    data class SetFloatingBottomBarCompactType(val type: FloatingBottomBarCompactType) :
        ThemeSettingsAction

    data class SetSeedColor(val color: Color) : ThemeSettingsAction
    data class SetSelectedTab(val index: Int) : ThemeSettingsAction
}
