package com.capybara.hypericonlab.modules.settings.domain.model

import androidx.compose.ui.graphics.Color
import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerSize
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.theme.material.RawColor
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode

data class ThemeSettingsState(
    val useSmootherRoundedCorners: Boolean = true,
    val useCustomCardCornerRadius: Boolean = false,
    val cardCornerSize: CardCornerSize = CardCornerSize.DEFAULT,
    val useBlur: Boolean = false,
    val useProgressiveBlurTopAppBar: Boolean = false,
    val useTabRowCenterAlignment: Boolean = false,
    val useTabRowTransparentBackground: Boolean = false,
    val useTabRowFillWidth: Boolean = false,
    val useLiquidGlassBottomSheet: Boolean = false,
    val useCustomLiquidGlassEngine: Boolean = false,
    val liquidGlassEngine: LiquidGlassEngine = LiquidGlassEngine.KYANT,
    val kyantGlassTuning: KyantGlassTuning = KyantGlassTuning(),
    val useAppleStyleCard: Boolean = false,
    val useSheetCardBackground: Boolean = true,
    val useAppleStyleToggle: Boolean = false,
    val useAppleStyleSlider: Boolean = false,
    val useGoogleSansFlex: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.Expressive,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = false,
    val useFloatingBottomBar: Boolean = false,
    val useFloatingBottomBarBlur: Boolean = true,
    val useFloatingBottomBarCompact: Boolean = false,
    val floatingBottomBarCompactType: FloatingBottomBarCompactType = FloatingBottomBarCompactType.MIXED_ICON,
    val seedColor: Color = PresetColors[0].color,
    val availableColors: List<RawColor> = PresetColors,
    val selectedTab: Int = 0
)
