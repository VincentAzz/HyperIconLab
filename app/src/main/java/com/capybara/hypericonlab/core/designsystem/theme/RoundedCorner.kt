package com.capybara.hypericonlab.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.shape.Capsule
import com.capybara.hypericonlab.core.designsystem.shape.RoundedCornerStyle
import com.capybara.hypericonlab.core.designsystem.shape.RoundedRectangle
import com.capybara.hypericonlab.core.designsystem.shape.UnevenRoundedRectangle

val CornerRadius = 16.dp

val ConnectionRadius = 6.dp

val ChipCornerInset = 8.dp

val PreviewCornerInset = 6.dp

val SwatchPreviewCornerRadius = 4.dp

val LargeCardRadius = 20.dp

val ExtraLargeRadius = 32.dp

enum class CardCornerSize(val cornerRadius: Dp) {
    DEFAULT(16.dp),
    LARGE(26.dp);

    companion object {
        fun fromValueOrDefault(value: String): CardCornerSize =
            entries.find { it.name == value } ?: DEFAULT
    }
}

val LocalPreferredCardCornerRadius = staticCompositionLocalOf { CornerRadius }

@Composable
@ReadOnlyComposable
fun currentPreferredCardCornerRadius(): Dp = LocalPreferredCardCornerRadius.current

fun insetCornerRadius(
    outerRadius: Dp,
    inset: Dp,
    minimumRadius: Dp = 0.dp
): Dp {
    val safeOuterRadius = outerRadius.coerceAtLeast(0.dp)
    val safeInset = inset.coerceAtLeast(0.dp)
    val safeMinimumRadius = minimumRadius.coerceAtLeast(0.dp)
    return (safeOuterRadius - safeInset).coerceAtLeast(safeMinimumRadius)
}

object TabRowRoundedCorner {
    val TabRowIndicatorCornerRadius = 24.dp

    val TabRowBarCornerRadius = TabRowIndicatorCornerRadius + 4.dp
}

val LocalSmootherRoundedCornersEnabled = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
fun isSmootherRoundedCornersEnabled(): Boolean = LocalSmootherRoundedCornersEnabled.current

// kyant capsule shape
@Composable
fun rememberKyantCapsuleShape(
    style: RoundedCornerStyle = RoundedCornerStyle.Continuous
): Shape {
    val enabled = isSmootherRoundedCornersEnabled()
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
    val enabled = isSmootherRoundedCornersEnabled()
    return remember(cornerRadius, style, enabled) {
        if (enabled) RoundedRectangle(cornerRadius = cornerRadius, style = style)
        else RoundedCornerShape(cornerRadius)
    }
}

// kyant uneven rounded shape
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
