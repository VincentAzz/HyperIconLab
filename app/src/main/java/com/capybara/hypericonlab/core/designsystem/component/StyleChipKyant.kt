package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.capybara.hypericonlab.core.designsystem.animation.inspectDragGestures
import com.capybara.hypericonlab.core.designsystem.config.ChipCornerInset
import com.capybara.hypericonlab.core.designsystem.config.insetCornerRadius
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.designsystem.symbol.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.symbol.check
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

private object StyleChipKyantDefaults {
    val Height = 36.dp
    val PressedExpansion = 4.dp
    val HorizontalPadding = 12.dp
    val IconSize = 16.dp
    val ContentSpacing = 8.dp
    val MaxDragExpansion = 4.dp
    const val InitialDragDerivative = 0.05f
    const val SpringDampingRatio = 0.5f
    const val SpringStiffness = 300f
    const val VisibilityThreshold = 0.001f
}

private class StyleChipKyantInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressAnimationSpec = spring(
        dampingRatio = StyleChipKyantDefaults.SpringDampingRatio,
        stiffness = StyleChipKyantDefaults.SpringStiffness,
        visibilityThreshold = StyleChipKyantDefaults.VisibilityThreshold,
    )
    private val positionAnimationSpec = spring(
        dampingRatio = StyleChipKyantDefaults.SpringDampingRatio,
        stiffness = StyleChipKyantDefaults.SpringStiffness,
        visibilityThreshold = Offset.VisibilityThreshold,
    )
    private val pressAnimation = Animatable(0f, StyleChipKyantDefaults.VisibilityThreshold)
    private val positionAnimation = Animatable(
        Offset.Zero,
        Offset.VectorConverter,
        Offset.VisibilityThreshold,
    )
    private var startPosition = Offset.Zero

    val pressProgress: Float get() = pressAnimation.value
    val offset: Offset get() = positionAnimation.value - startPosition

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressAnimation.animateTo(1f, pressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressAnimation.animateTo(0f, pressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressAnimation.animateTo(0f, pressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
        ) { change, _ ->
            animationScope.launch { positionAnimation.snapTo(change.position) }
        }
    }
}


@Composable
internal fun StyleChipKyant(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    selectedContainerColor: Color,
    unselectedContainerColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color?,
) {
    val outerCornerRadius = currentSegmentedColumnOuterCornerRadius()
    val shape = rememberKyantRoundedRectangleShape(
        insetCornerRadius(outerCornerRadius, ChipCornerInset),
    )
    val animationScope = rememberCoroutineScope()
    val interaction = remember(animationScope) {
        StyleChipKyantInteraction(animationScope)
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor else unselectedContainerColor,
        label = "kyant_chip_background_color",
    )
    val resolvedUnselectedContentColor = unselectedContentColor
        ?: if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else resolvedUnselectedContentColor,
        label = "kyant_chip_text_color",
    )

    Row(
        modifier = modifier
            .zIndex(interaction.pressProgress)
            .graphicsLayer {
                val safeHeight = size.height.coerceAtLeast(1f)
                val safeWidth = size.width.coerceAtLeast(1f)
                val progress = interaction.pressProgress
                val pressedScale = lerp(
                    1f,
                    1f + StyleChipKyantDefaults.PressedExpansion.toPx() / safeHeight,
                    progress,
                )
                val offset = interaction.offset
                val maxOffset = size.minDimension.coerceAtLeast(1f)
                translationX = maxOffset * tanh(
                    StyleChipKyantDefaults.InitialDragDerivative * offset.x / maxOffset,
                )
                translationY = maxOffset * tanh(
                    StyleChipKyantDefaults.InitialDragDerivative * offset.y / maxOffset,
                )

                val maxDragScale = StyleChipKyantDefaults.MaxDragExpansion.toPx() / safeHeight
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX = pressedScale +
                        maxDragScale * abs(
                    cos(offsetAngle) * offset.x / size.maxDimension.coerceAtLeast(
                        1f
                    )
                ) *
                        (safeWidth / safeHeight).fastCoerceAtMost(1f)
                scaleY = pressedScale +
                        maxDragScale * abs(
                    sin(offsetAngle) * offset.y / size.maxDimension.coerceAtLeast(
                        1f
                    )
                ) *
                        (safeHeight / safeWidth).fastCoerceAtMost(1f)
            }
            .background(color = backgroundColor, shape = shape)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (enabled) interaction.gestureModifier else Modifier)
            .height(StyleChipKyantDefaults.Height)
            .fillMaxWidth()
            .padding(horizontal = StyleChipKyantDefaults.HorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(
            StyleChipKyantDefaults.ContentSpacing,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = AppMaterialSymbols.check,
                contentDescription = null,
                modifier = Modifier.size(StyleChipKyantDefaults.IconSize),
                tint = selectedContentColor,
            )
        }
    }
}
