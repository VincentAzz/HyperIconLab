package com.capybara.hypericonlab.core.designsystem.config

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private object SheetAppearanceConfig {
    const val DefaultCardAlpha = 0.8f
    const val HiddenCardAlpha = 0f
}

val LocalUseSheetCardBackground = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
fun isSheetCardBackgroundEnabled(): Boolean =
    LocalUseSheetCardBackground.current

@Composable
@ReadOnlyComposable
fun sheetCardContainerAlpha(
    visibleAlpha: Float = SheetAppearanceConfig.DefaultCardAlpha
): Float =
    if (isSheetCardBackgroundEnabled()) visibleAlpha
    else SheetAppearanceConfig.HiddenCardAlpha

@Composable
@ReadOnlyComposable
fun sheetCardContainerColor(visibleColor: Color): Color =
    if (isSheetCardBackgroundEnabled()) visibleColor
    else Color.Transparent
