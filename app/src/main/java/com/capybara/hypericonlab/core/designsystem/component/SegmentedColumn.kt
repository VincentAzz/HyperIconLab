package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius

private object SegmentedColumnConfig {
    val HorizontalPadding = 16.dp
    val VerticalPadding = 8.dp
}

val LocalUseAppleStyleCard = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isAppleStyleCardEnabled(): Boolean = LocalUseAppleStyleCard.current

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    outerCornerRadius: Dp = CornerRadius,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SegmentedColumnConfig.HorizontalPadding,
        vertical = SegmentedColumnConfig.VerticalPadding
    ),
    containerColorAlpha: Float = 1f,
    content: SegmentedColumnScope.() -> Unit
) {
    if (isAppleStyleCardEnabled()) {
        SegmentedColumnApple(
            modifier = modifier,
            title = title,
            outerCornerRadius = outerCornerRadius,
            contentPadding = contentPadding,
            containerColorAlpha = containerColorAlpha,
            content = content
        )
    } else {
        SegmentedColumnMaterial(
            modifier = modifier,
            title = title,
            outerCornerRadius = outerCornerRadius,
            contentPadding = contentPadding,
            containerColorAlpha = containerColorAlpha,
            content = content
        )
    }
}
