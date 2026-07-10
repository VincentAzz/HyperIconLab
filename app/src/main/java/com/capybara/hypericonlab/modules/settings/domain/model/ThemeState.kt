package com.capybara.hypericonlab.modules.settings.domain.model

import android.os.Build
import androidx.compose.ui.graphics.Color
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode

data class ThemeState(
    val isLoaded: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
    val useDynamicColor: Boolean = true,
    val useFloatingBottomBar: Boolean = false,
    val useFloatingBottomBarBlur: Boolean = true,
    val useFloatingBottomBarCompact: Boolean = false,
    val floatingBottomBarCompactType: FloatingBottomBarCompactType = FloatingBottomBarCompactType.MIXED_ICON,
    val seedColor: Color = PresetColors.first().color,
    val useSmootherRoundedCorners: Boolean = true,
    val useBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    val useProgressiveBlurTopAppBar: Boolean = false,
    val useTabRowCenterAlignment: Boolean = false,
    val useTabRowTransparentBackground: Boolean = false,
    val useTabRowFillWidth: Boolean = false,
    val useLiquidGlassBottomSheet: Boolean = false,
    val liquidGlassBlurRadius: Int = 24,
    val useGoogleSansFlex: Boolean = false,
)
