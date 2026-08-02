package com.capybara.hypericonlab.iconpack.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.shape.Capsule
import com.capybara.hypericonlab.iconpack.ui.shape.RoundedCornerStyle
import com.capybara.hypericonlab.iconpack.ui.shape.RoundedRectangle
import com.capybara.hypericonlab.iconpack.ui.shape.UnevenRoundedRectangle

val CornerRadius = 16.dp
val ConnectionRadius = 6.dp
val CardCornerRadius = 16.dp
val ChipCornerRadius = 8.dp
val PreviewCornerRadius = 10.dp
val SwatchPreviewCornerRadius = 4.dp
val LargeCardRadius = 20.dp
val ExtraLargeRadius = 32.dp


@Composable
fun rememberKyantRoundedRectangleShape(
    cornerRadius: Dp, style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape = remember(cornerRadius, style) {
    RoundedRectangle(cornerRadius = cornerRadius, style = style)
}


@Composable
fun rememberKyantCapsuleShape(
    style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape = remember(style) {
    Capsule(style = style)
}


@Composable
fun rememberKyantUnevenRoundedRectangleShape(
    topStart: Dp, topEnd: Dp, bottomEnd: Dp, bottomStart: Dp
): Shape = remember(topStart, topEnd, bottomEnd, bottomStart) {
    UnevenRoundedRectangle(
        topStart = topStart, topEnd = topEnd, bottomEnd = bottomEnd, bottomStart = bottomStart
    )
}
