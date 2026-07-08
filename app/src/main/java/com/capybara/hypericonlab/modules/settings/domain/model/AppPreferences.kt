package com.capybara.hypericonlab.modules.settings.domain.model

import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode

data class AppPreferences(
    val useMiuixSquircle: Boolean = true,
    val useBlur: Boolean,
    val useLiquidGlassBottomSheet: Boolean = false,
    val liquidGlassBlurRadius: Int = 24,
    val themeMode: ThemeMode,
    val paletteStyle: PaletteStyle,
    val colorSpec: ThemeColorSpec,
    val useDynamicColor: Boolean,
    val useFloatingBottomBar: Boolean,
    val useFloatingBottomBarBlur: Boolean,
    val useFloatingBottomBarCompact: Boolean = false,
    val floatingBottomBarCompactType: FloatingBottomBarCompactType = FloatingBottomBarCompactType.MIXED_ICON,
    val seedColorInt: Int,
    // 暂时设置tab
    val lastMainPageIndex: Int = 3
)
