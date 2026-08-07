package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.theme.currentPreferredCardCornerRadius

private object SegmentedColumnConfig {
    val HorizontalPadding = 16.dp
    val VerticalPadding = 8.dp
}

val LocalUseAppleStyleCard = staticCompositionLocalOf { false }

val LocalSegmentedColumnOuterCornerRadius = staticCompositionLocalOf<Dp?> { null }

@Composable
@ReadOnlyComposable
fun isAppleStyleCardEnabled(): Boolean = LocalUseAppleStyleCard.current

@Composable
@ReadOnlyComposable
fun currentSegmentedColumnOuterCornerRadius(): Dp =
    LocalSegmentedColumnOuterCornerRadius.current ?: currentPreferredCardCornerRadius()

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String = "",
    outerCornerRadius: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = SegmentedColumnConfig.HorizontalPadding,
        vertical = SegmentedColumnConfig.VerticalPadding
    ),
    containerColorAlpha: Float = 1f,
    content: SegmentedColumnScope.() -> Unit
) {
    val resolvedOuterCornerRadius = outerCornerRadius
        ?: currentPreferredCardCornerRadius()

    CompositionLocalProvider(
        LocalSegmentedColumnOuterCornerRadius provides resolvedOuterCornerRadius
    ) {
        if (isAppleStyleCardEnabled()) {
            SegmentedColumnApple(
                modifier = modifier,
                title = title,
                outerCornerRadius = resolvedOuterCornerRadius,
                contentPadding = contentPadding,
                containerColorAlpha = containerColorAlpha,
                content = content
            )
        } else {
            SegmentedColumnMaterial(
                modifier = modifier,
                title = title,
                outerCornerRadius = resolvedOuterCornerRadius,
                contentPadding = contentPadding,
                containerColorAlpha = containerColorAlpha,
                content = content
            )
        }
    }
}
