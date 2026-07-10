package com.capybara.hypericonlab.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.shape.Capsule
import com.capybara.hypericonlab.core.designsystem.shape.RoundedCornerStyle
import top.yukonga.miuix.kmp.squircle.SquircleDefaults
import top.yukonga.miuix.kmp.squircle.addSquircleRect
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


class MiuixSquircleShape(
    private val cornerRadius: Dp,
    private val extension: Float = SquircleDefaults.Extension,
    private val squircleEnabled: Boolean
) : Shape {
    override fun createOutline(
        size: Size, layoutDirection: LayoutDirection, density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        val path = Path()
        path.addSquircleRect(
            width = size.width,
            height = size.height,
            cornerRadius = radiusPx,
            extension = extension,
            squircleEnabled = squircleEnabled
        )
        return Outline.Generic(path)
    }
}

// miuix squircle shape
@Composable
fun rememberMiuixSquircleShape(
    cornerRadius: Dp,
    extension: Float = SquircleDefaults.Extension
): Shape {
    val enabled = isSquircleEnabled()
    return remember(cornerRadius, extension, enabled) {
        MiuixSquircleShape(cornerRadius, extension, enabled)
    }
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
