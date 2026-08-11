package com.capybara.hypericonlab.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

// MiuixTheme 桥接
@Composable
fun MiuixThemeBridge(
    content: @Composable () -> Unit
) {
    val m3 = MaterialTheme.colorScheme
    val isDark = AppTheme.isDark
    val usesMiuixDefaultPalette = AppTheme.usesMiuixDefaultPalette
    val miuixColors = if (usesMiuixDefaultPalette) {
        remember(isDark) {
            if (isDark) darkColorScheme() else lightColorScheme()
        }
    } else {
        remember(m3, isDark) {
            (if (isDark) darkColorScheme() else lightColorScheme()).copy(
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
