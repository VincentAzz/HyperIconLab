package com.capybara.hypericonlab.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.shape.Capsule
import com.capybara.hypericonlab.core.designsystem.shape.RoundedCornerStyle
import com.capybara.hypericonlab.core.designsystem.shape.RoundedRectangle
import com.capybara.hypericonlab.core.designsystem.shape.UnevenRoundedRectangle
import top.yukonga.miuix.kmp.squircle.isSquircleEnabled

// 基础圆角大小，常用于主要组件
val CornerRadius = 16.dp

// 连接处的较小圆角
val ConnectionRadius = 6.dp

// BottomSheet 圆角
val BottomSheetCornerRadius = 24.dp

// 对话框或卡片的默认圆角
val CardCornerRadius = 16.dp

// 分段控制栏容器的圆角
val SegmentedContainerRadius = 12.dp

// 按钮或 Chip 的默认圆角
val ChipCornerRadius = 8.dp

val PreviewCornerRadius = 10.dp

// 颜色色块预览（如设置项尾部的小色块）的圆角
val SwatchPreviewCornerRadius = 4.dp

// 颜色预览项或较大容器的圆角
val LargeCardRadius = 20.dp

// 特殊形状选择器或大面板的圆角
val ExtraLargeRadius = 32.dp

object TabRowRoundedCorner {
    val TabRowIndicatorCornerRadius = 24.dp

    val TabRowBarCornerRadius = TabRowIndicatorCornerRadius + 4.dp
}

// kyant capsule shape
@Composable
fun rememberKyantCapsuleShape(
    style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape {
    val enabled = isSquircleEnabled()
    return remember(enabled, style) {
        if (enabled) Capsule(style = style) else RoundedCornerShape(50)
    }
}

// kyant rounded rectangle shape
@Composable
fun rememberKyantRoundedRectangleShape(
    cornerRadius: Dp,
    style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape {
    val enabled = isSquircleEnabled()
    return remember(cornerRadius, style, enabled) {
        if (enabled) RoundedRectangle(cornerRadius = cornerRadius, style = style)
        else RoundedCornerShape(cornerRadius)
    }
}

// kyant 非对称圆角
fun kyantUnevenRoundedShape(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    enabled: Boolean,
    style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape {
    return if (enabled) {
        UnevenRoundedRectangle(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart,
            style = style
        )
    } else {
        RoundedCornerShape(
            topStart = topStart,
            topEnd = topEnd,
            bottomEnd = bottomEnd,
            bottomStart = bottomStart
        )
    }
}
