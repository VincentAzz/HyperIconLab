package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class AppleStyleControls(
    val useToggle: Boolean = false,
    val useSlider: Boolean = false
)

val LocalAppleStyleControls = staticCompositionLocalOf { AppleStyleControls() }
