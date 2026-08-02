package com.capybara.hypericonlab.iconpack.ui.shape

// from Kyant0/Shapes - https://github.com/kyant0/Shapes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class RectangleCornerRadii(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp
)
