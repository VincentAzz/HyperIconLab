package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.isSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.core.designsystem.theme.kyantUnevenRoundedShape
import kotlin.math.roundToInt

private object AppleSegmentedColumnConfig {
    val HorizontalPadding = 16.dp
    val VerticalPadding = 8.dp
    val TitleBottomPadding = 16.dp
    val LeadingIconSize = 24.dp
    val LeadingContentSpacing = 16.dp
    val DividerStartWithLeading = HorizontalPadding + LeadingIconSize + LeadingContentSpacing
    val DividerHeight = 0.8.dp
    const val DividerAlpha = 0.6f
    const val SpringStiffness = 800f
    const val SpringDamping = 0.5f
    const val ContentFadeMultiplier = 1.5f
}

internal val LocalSegmentedLeadingContentReporter =
    staticCompositionLocalOf<((Boolean) -> Unit)?> { null }

@Composable
fun SegmentedColumnApple(
    modifier: Modifier = Modifier,
    title: String = "",
    outerCornerRadius: Dp = CornerRadius,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = AppleSegmentedColumnConfig.HorizontalPadding,
        vertical = AppleSegmentedColumnConfig.VerticalPadding
    ),
    containerColorAlpha: Float = 1f,
    content: SegmentedColumnScope.() -> Unit
) {
    val scope = SegmentedColumnScope().apply(content)
    val allItems = scope.items

    if (allItems.isEmpty()) return

    val smootherRoundedCornersEnabled = isSmootherRoundedCornersEnabled()
    val groupShape = kyantUnevenRoundedShape(
        topStart = outerCornerRadius,
        topEnd = outerCornerRadius,
        bottomEnd = outerCornerRadius,
        bottomStart = outerCornerRadius,
        enabled = smootherRoundedCornersEnabled
    )

    Column(modifier = modifier.padding(contentPadding)) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start = AppleSegmentedColumnConfig.HorizontalPadding,
                    top = AppleSegmentedColumnConfig.VerticalPadding,
                    bottom = AppleSegmentedColumnConfig.TitleBottomPadding
                )
            )
        }

        val floatSpring = spring<Float>(
            dampingRatio = AppleSegmentedColumnConfig.SpringDamping,
            stiffness = AppleSegmentedColumnConfig.SpringStiffness
        )
        val progresses = allItems.mapIndexed { index, item ->
            key(item.key ?: index) {
                animateFloatAsState(
                    targetValue = if (item.visible) 1f else 0f,
                    animationSpec = floatSpring,
                    label = "AppleSegmentedItemProgress"
                )
            }
        }
        val firstVisibleIndex = allItems.indexOfFirst { it.visible }

        Layout(
            modifier = Modifier.clip(groupShape),
            content = {
                allItems.forEachIndexed { index, itemData ->
                    key(itemData.key ?: index) {
                        val isFirst = index == firstVisibleIndex
                        var hasLeadingContent by remember { mutableStateOf(false) }
                        val leadingContentReporter = remember {
                            { hasLeading: Boolean -> hasLeadingContent = hasLeading }
                        }

                        Box(
                            modifier = Modifier
                                .zIndex(
                                    if (itemData.visible) {
                                        (allItems.size - index).toFloat()
                                    } else {
                                        -index.toFloat()
                                    }
                                )
                                .semantics {
                                    if (!itemData.visible) hideFromAccessibility()
                                }
                                .graphicsLayer {
                                    val currentProgress = progresses[index].value
                                    val safeProgress = currentProgress.coerceAtLeast(0f)

                                    clip = safeProgress < 1f
                                    if (clip) {
                                        shape = ProgressClipShape(safeProgress)
                                    }
                                    alpha = (
                                            currentProgress * AppleSegmentedColumnConfig.ContentFadeMultiplier
                                            ).coerceIn(0f, 1f)
                                }
                        ) {
                            CompositionLocalProvider(
                                LocalSegmentedItemShape provides RectangleShape,
                                LocalSegmentedContainerColorAlpha provides containerColorAlpha,
                                LocalSegmentedLeadingContentReporter provides leadingContentReporter
                            ) {
                                Box {
                                    itemData.content(RectangleShape)
                                    if (!isFirst && itemData.visible) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .fillMaxWidth()
                                                .padding(
                                                    start = if (hasLeadingContent) {
                                                        AppleSegmentedColumnConfig.DividerStartWithLeading
                                                    } else {
                                                        AppleSegmentedColumnConfig.HorizontalPadding
                                                    },
                                                    end = AppleSegmentedColumnConfig.HorizontalPadding
                                                )
                                                .height(AppleSegmentedColumnConfig.DividerHeight)
                                                .background(
                                                    MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = AppleSegmentedColumnConfig.DividerAlpha
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            var currentY = 0f
            val positions = mutableListOf<Float>()

            placeables.forEachIndexed { index, placeable ->
                positions.add(currentY)
                currentY += placeable.height * progresses[index].value
            }

            layout(constraints.maxWidth, currentY.roundToInt().coerceAtLeast(0)) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(
                        x = 0,
                        y = positions[index].roundToInt()
                    )
                }
            }
        }
    }
}

private class ProgressClipShape(
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Rectangle(
        Rect(0f, 0f, size.width, size.height * progress)
    )
}
