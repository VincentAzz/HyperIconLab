package com.capybara.hypericonlab.core.designsystem.color.miuix

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

// Material to miuix
@Composable
fun MiuixThemeBridge(
    content: @Composable () -> Unit
) {
    val m3 = MaterialTheme.colorScheme
    val isDark = AppTheme.isDark
    val usesMiuixDefaultPalette = AppTheme.usesMiuixDefaultPalette
    val miuixColors = if (usesMiuixDefaultPalette) {
        remember(isDark) {
            if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()
        }
    } else {
        remember(m3, isDark) {
            (if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()).copy(
                primary = m3.primary,
                onPrimary = m3.onPrimary,
                primaryContainer = m3.primaryContainer,
                onPrimaryContainer = m3.onPrimaryContainer,
                secondary = m3.secondary,
                onSecondary = m3.onSecondary,
                secondaryContainer = m3.secondaryContainer,
                onSecondaryContainer = m3.onSecondaryContainer,
                tertiaryContainer = m3.tertiaryContainer,
                onTertiaryContainer = m3.onTertiaryContainer,
                background = m3.background,
                onBackground = m3.onBackground,
                surface = m3.surface,
                onSurface = m3.onSurface,
                surfaceVariant = m3.surfaceVariant,
                surfaceContainer = m3.surfaceContainer,
                surfaceContainerHigh = m3.surfaceContainerHigh,
                surfaceContainerHighest = m3.surfaceContainerHighest,
                error = m3.error,
                onError = m3.onError,
                errorContainer = m3.errorContainer,
                onErrorContainer = m3.onErrorContainer,
                outline = m3.outline,
                dividerLine = m3.outlineVariant,
                windowDimming = m3.scrim
            )
        }
    }
    MiuixTheme(colors = miuixColors) {
        content()
    }
}


fun miuixDefaultMaterialColorScheme(isDark: Boolean): ColorScheme {
    val colors = if (isDark) miuixDarkColorScheme() else miuixLightColorScheme()
    val inverseColors = if (isDark) miuixLightColorScheme() else miuixDarkColorScheme()
    return colors.toMaterialColorScheme(inverseColors)
}

// miuix to Material
fun Colors.toMaterialColorScheme(inverseColors: Colors): ColorScheme = ColorScheme(
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
