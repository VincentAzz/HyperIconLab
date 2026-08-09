package com.capybara.hypericonlab.modules.settings.domain.model

import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerSize
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode

data class AppPreferences(
    val useSmootherRoundedCorners: Boolean = true,
    val useCustomCardCornerRadius: Boolean = false,
    val cardCornerSize: CardCornerSize = CardCornerSize.DEFAULT,
    val useBlur: Boolean,
    val useProgressiveBlurTopAppBar: Boolean = false,
    val useTabRowCenterAlignment: Boolean = false,
    val useTabRowTransparentBackground: Boolean = false,
    val useTabRowFillWidth: Boolean = false,
    val useLiquidGlassBottomSheet: Boolean = false,
    val useCustomLiquidGlassEngine: Boolean = false,
    val liquidGlassEngine: LiquidGlassEngine = LiquidGlassEngine.KYANT,
    val kyantGlassTuning: KyantGlassTuning = KyantGlassTuning(),
    val useGoogleSansFlex: Boolean = false,
    val themeMode: ThemeMode,
    val paletteStyle: PaletteStyle,
    val colorSpec: ThemeColorSpec,
    val useDynamicColor: Boolean,
    val useFloatingBottomBar: Boolean,
    val useFloatingBottomBarBlur: Boolean,
    val useFloatingBottomBarCompact: Boolean = false,
    val floatingBottomBarCompactType: FloatingBottomBarCompactType = FloatingBottomBarCompactType.MIXED_ICON,
    val seedColorInt: Int,
    val useAppleStyleCard: Boolean = false,
    val useAppleStyleToggle: Boolean = false,
    val useAppleStyleSlider: Boolean = false,
    val useDownloadProxy: Boolean = false,
    val lastMainPageIndex: Int = 3
)
