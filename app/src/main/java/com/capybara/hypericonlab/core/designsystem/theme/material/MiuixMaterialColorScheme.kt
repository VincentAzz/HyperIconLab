package com.capybara.hypericonlab.core.designsystem.theme.material

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/**
 * 将不带 Monet 种子色的 miuix 默认配色转换为 Material 3 配色。
 */
fun miuixDefaultMaterialColorScheme(isDark: Boolean): ColorScheme {
    val colors = if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()
    val inverseColors = if (isDark) miuixLightColorScheme() else miuixDarkColorScheme()
    return colors.toMaterialColorScheme(inverseColors)
}

private fun Colors.toMaterialColorScheme(inverseColors: Colors): ColorScheme = ColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inverseColors.primary,
    secondary = secondaryVariant,
    onSecondary = onSecondaryVariant,
    secondaryContainer = secondaryVariant,
    onSecondaryContainer = onSecondaryVariant,
    tertiary = primaryVariant,
    onTertiary = onPrimaryVariant,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    background = surface,
    onBackground = onSurface,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = secondaryContainer,
    onSurfaceVariant = onSurfaceVariantSummary,
    surfaceTint = Color.Transparent,
    inverseSurface = inverseColors.surface,
    inverseOnSurface = inverseColors.onSurface,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    outline = outline,
    outlineVariant = dividerLine,
    scrim = windowDimming,
    surfaceBright = surfaceContainer,
    surfaceDim = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,
    surfaceContainerLow = surface,
    surfaceContainerLowest = surface,
    primaryFixed = primary,
    primaryFixedDim = primaryContainer,
    onPrimaryFixed = onPrimary,
    onPrimaryFixedVariant = onPrimaryVariant,
    secondaryFixed = secondaryVariant,
    secondaryFixedDim = secondaryContainer,
    onSecondaryFixed = onSecondaryVariant,
    onSecondaryFixedVariant = onSecondaryContainerVariant,
    tertiaryFixed = primaryVariant,
    tertiaryFixedDim = tertiaryContainer,
    onTertiaryFixed = onPrimaryVariant,
    onTertiaryFixedVariant = onTertiaryContainer
)
