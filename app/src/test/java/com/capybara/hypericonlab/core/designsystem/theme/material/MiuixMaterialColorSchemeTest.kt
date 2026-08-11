package com.capybara.hypericonlab.core.designsystem.theme.material

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

class MiuixMaterialColorSchemeTest {

    @Test
    fun lightSchemeMapsKeyMiuixRoles() {
        val miuix = miuixLightColorScheme()
        val material = miuixDefaultMaterialColorScheme(isDark = false)

        assertEquals(miuix.primary, material.primary)
        assertEquals(miuix.surface, material.background)
        assertEquals(miuix.surface, material.surfaceContainer)
        assertEquals(miuix.surfaceContainer, material.surfaceBright)
        assertEquals(miuix.secondaryContainer, material.surfaceVariant)
        assertEquals(miuix.onSecondaryVariant, material.onSecondaryContainer)
        assertEquals(miuix.onSurfaceVariantSummary, material.onSurfaceVariant)
        assertEquals(miuix.dividerLine, material.outlineVariant)
        assertEquals(miuix.windowDimming, material.scrim)
        assertEquals(Color.Transparent, material.surfaceTint)
    }

    @Test
    fun darkSchemeMapsKeyMiuixRoles() {
        val miuix = miuixDarkColorScheme()
        val material = miuixDefaultMaterialColorScheme(isDark = true)

        assertEquals(miuix.primary, material.primary)
        assertEquals(miuix.surface, material.background)
        assertEquals(miuix.surface, material.surfaceContainer)
        assertEquals(miuix.surfaceContainer, material.surfaceBright)
        assertEquals(miuix.surfaceContainerHighest, material.surfaceContainerHighest)
        assertEquals(miuix.dividerLine, material.outlineVariant)
    }

    @Test
    fun inverseRolesComeFromOppositeDefaultScheme() {
        val light = miuixDefaultMaterialColorScheme(isDark = false)
        val dark = miuixDefaultMaterialColorScheme(isDark = true)

        assertEquals(dark.surface, light.inverseSurface)
        assertEquals(dark.onSurface, light.inverseOnSurface)
        assertEquals(light.surface, dark.inverseSurface)
        assertEquals(light.primary, dark.inversePrimary)
    }

    @Test
    fun primaryTextAndSurfaceTextKeepBasicContrast() {
        listOf(false, true).forEach { isDark ->
            val scheme = miuixDefaultMaterialColorScheme(isDark)

            assertTrue(contrastRatio(scheme.onPrimary, scheme.primary) >= 3.0f)
            assertTrue(contrastRatio(scheme.onSurface, scheme.surface) >= 4.5f)
            assertTrue(contrastRatio(scheme.onSecondary, scheme.secondary) >= 4.5f)
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val opaqueForeground = foreground.compositeOver(background)
        val lighter = maxOf(opaqueForeground.luminance(), background.luminance())
        val darker = minOf(opaqueForeground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
