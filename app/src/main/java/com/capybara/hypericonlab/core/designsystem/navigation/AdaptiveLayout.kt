package com.capybara.hypericonlab.core.designsystem.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

data class WindowLayoutInfo(
    val isLandscape: Boolean,
    val isMediumPortrait: Boolean
)

val LocalWindowLayoutInfo = compositionLocalOf<WindowLayoutInfo> {
    error("WindowLayoutInfo not provided")
}

@Composable
fun rememberWindowLayoutInfo(): WindowLayoutInfo {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val screenWidthDp = with(density) { containerSize.width.toDp().value }
    val screenHeightDp = with(density) { containerSize.height.toDp().value }
    val isLandscape = screenWidthDp > screenHeightDp

    val isMediumPortrait = screenWidthDp in 600f..<840f && !isLandscape

    return WindowLayoutInfo(
        isLandscape = isLandscape,
        isMediumPortrait = isMediumPortrait
    )
}
