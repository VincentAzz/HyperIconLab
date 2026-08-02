package com.capybara.hypericonlab.iconpack.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.shape.RoundedCornerStyle
import com.capybara.hypericonlab.iconpack.ui.shape.RoundedRectangle

// 与 HyperIconLab 主应用保持一致的圆角参数。
val CornerRadius = 16.dp
val ConnectionRadius = 6.dp
val CardCornerRadius = 16.dp
val ChipCornerRadius = 8.dp
val PreviewCornerRadius = 10.dp
val SwatchPreviewCornerRadius = 4.dp
val LargeCardRadius = 20.dp
val ExtraLargeRadius = 32.dp

/**
 * 创建与主应用一致的连续曲率圆角矩形。
 */
@Composable
fun rememberKyantRoundedRectangleShape(
    cornerRadius: Dp,
    style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape = remember(cornerRadius, style) {
    RoundedRectangle(cornerRadius = cornerRadius, style = style)
}
