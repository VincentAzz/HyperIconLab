package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

enum class ExpressiveSliderTrackThickness(val height: Dp) {
    THIN(4.dp),
    STANDARD(6.dp),
    THICK(8.dp)
}

private object ExpressiveSliderDefaults {
    val ThumbRadius = 8.dp
    val ThumbLineHeight = 24.dp
    val ThumbGap = 4.dp
    val ContainerVerticalPadding = 8.dp
    const val HAPTIC_GRANULARITY = 50
    const val THUMB_ANIM_DURATION_MS = 250
}


@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    trackThickness: ExpressiveSliderTrackThickness = ExpressiveSliderTrackThickness.THIN,
    thumbRadius: Dp = ExpressiveSliderDefaults.ThumbRadius,
    thumbLineHeight: Dp = ExpressiveSliderDefaults.ThumbLineHeight,
    thumbGap: Dp = ExpressiveSliderDefaults.ThumbGap,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
) {
    val trackHeight = trackThickness.height
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isInteracting = isDragged || isPressed

    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(
            durationMillis = ExpressiveSliderDefaults.THUMB_ANIM_DURATION_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "ThumbInteraction",
    )

    val density = LocalDensity.current
    val trackHeightPx = with(density) { trackHeight.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val thumbLineHeightPx = with(density) { thumbLineHeight.toPx() }
    val thumbGapPx = with(density) { thumbGap.toPx() }

    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val normalizedValue = if (valueRange.endInclusive == valueRange.start) {
        0f
    } else {
        ((clampedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(
            0f,
            1f
        )
    }

    val disabledAlpha = 0.38f
    val actualActiveColor =
        if (enabled) activeTrackColor else activeTrackColor.copy(alpha = disabledAlpha)
    val actualInactiveColor =
        if (enabled) inactiveTrackColor else inactiveTrackColor.copy(alpha = disabledAlpha)
    val actualThumbColor = if (enabled) thumbColor else thumbColor.copy(alpha = disabledAlpha)

    val hapticFeedback = LocalHapticFeedback.current
    val sliderVisualHeight = max(
        trackHeight * 2,
        max(thumbRadius * 2, thumbLineHeight) + ExpressiveSliderDefaults.ContainerVerticalPadding,
    )

    Box(modifier = modifier) {
        val lastHapticStep = remember { mutableIntStateOf(-1) }

        Slider(
            value = clampedValue,
            onValueChange = { newValue ->
                val normalizedNew = if (valueRange.endInclusive == valueRange.start) {
                    0f
                } else {
                    ((newValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(
                        0f,
                        1f
                    )
                }
                val currentStep =
                    (normalizedNew * ExpressiveSliderDefaults.HAPTIC_GRANULARITY).roundToInt()
                if (currentStep != lastHapticStep.intValue) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticStep.intValue = currentStep
                }
                onValueChange(newValue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderVisualHeight),
            enabled = enabled,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent,
            ),
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderVisualHeight)
                .drawWithCache {
                    val canvasWidth = size.width
                    val centerY = size.height / 2f
                    val halfTrackHeight = trackHeightPx / 2f
                    val trackStart = halfTrackHeight
                    val trackEnd = canvasWidth - halfTrackHeight
                    val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)
                    val progressEnd = trackStart + (trackWidth * normalizedValue)

                    val thumbWidth = lerp(
                        thumbRadiusPx * 2f,
                        trackHeightPx * 1.2f,
                        thumbInteractionFraction,
                    )
                    val thumbHeight = lerp(
                        thumbRadiusPx * 2f,
                        thumbLineHeightPx,
                        thumbInteractionFraction,
                    )

                    onDrawWithContent {
                        val currentThumbRadius = thumbWidth / 2f
                        val halfTrackHeight = trackHeightPx / 2f

                        val inactiveStart =
                            progressEnd + currentThumbRadius + thumbGapPx + halfTrackHeight
                        if (inactiveStart < trackEnd) {
                            drawLine(
                                color = actualInactiveColor,
                                start = Offset(inactiveStart, centerY),
                                end = Offset(trackEnd, centerY),
                                strokeWidth = trackHeightPx,
                                cap = StrokeCap.Round,
                            )
                        }

                        val activeEnd =
                            progressEnd - currentThumbRadius - thumbGapPx - halfTrackHeight
                        if (activeEnd > trackStart) {
                            drawLine(
                                color = actualActiveColor,
                                start = Offset(trackStart, centerY),
                                end = Offset(activeEnd, centerY),
                                strokeWidth = trackHeightPx,
                                cap = StrokeCap.Round,
                            )
                        }

                        val thumbCenterX = trackStart + trackWidth * normalizedValue
                        drawRoundRect(
                            color = actualThumbColor,
                            topLeft = Offset(
                                thumbCenterX - thumbWidth / 2f,
                                centerY - thumbHeight / 2f,
                            ),
                            size = Size(thumbWidth, thumbHeight),
                            cornerRadius = CornerRadius(thumbWidth / 2f),
                        )
                    }
                },
        )
    }
}
