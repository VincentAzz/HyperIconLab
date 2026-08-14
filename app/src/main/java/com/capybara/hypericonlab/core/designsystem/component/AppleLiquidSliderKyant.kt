package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.capybara.hypericonlab.core.designsystem.animation.DampedDragAnimation
import com.capybara.hypericonlab.core.designsystem.blur.kyant.Backdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.layerBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.rememberBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.rememberCombinedBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.rememberLayerBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.drawBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.effects.blur
import com.capybara.hypericonlab.core.designsystem.blur.kyant.effects.lens
import com.capybara.hypericonlab.core.designsystem.blur.kyant.highlight.Highlight
import com.capybara.hypericonlab.core.designsystem.blur.kyant.shadow.InnerShadow
import com.capybara.hypericonlab.core.designsystem.blur.kyant.shadow.Shadow
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantCapsuleShape
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme

private object AppleLiquidSliderKyantDefaults {
    val TrackHeight = 6.dp
    val ThumbWidth = 40.dp
    val ThumbHeight = 24.dp
    val BlurRadius = 8.dp
    val TrackColorLight = Color(0xFF787878).copy(alpha = 0.2f)
    val TrackColorDark = Color(0xFF787880).copy(alpha = 0.36f)
}

@Composable
fun AppleLiquidSliderKyant(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    activeTrackColor: Color = MaterialTheme.colorScheme.primary,
    inactiveTrackColor: Color = if (AppTheme.isDark) {
        AppleLiquidSliderKyantDefaults.TrackColorDark
    } else {
        AppleLiquidSliderKyantDefaults.TrackColorLight
    },
    thumbColor: Color = if (AppTheme.isDark) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    },
    onValueChangeFinished: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope, valueRange, visibilityThreshold) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                        onValueChangeFinished?.invoke()
                        didDrag = false
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    if (trackWidth > 0) {
                        val delta = (valueRange.endInclusive - valueRange.start) *
                                (dragAmount.x / trackWidth)
                        onValueChange(
                            if (isLtr) {
                                (targetValue + delta).coerceIn(valueRange)
                            } else {
                                (targetValue - delta).coerceIn(valueRange)
                            }
                        )
                    }
                }
            )
        }

        LaunchedEffect(dampedDragAnimation, value()) {
            val currentValue = value()
            if (dampedDragAnimation.targetValue != currentValue) {
                dampedDragAnimation.updateValue(currentValue)
            }
        }

        val trackBackdrop = rememberLayerBackdrop()
        val shape = rememberKyantCapsuleShape()
        val progress by remember(dampedDragAnimation, valueRange) {
            derivedStateOf {
                val range = valueRange.endInclusive - valueRange.start
                if (range == 0f) 0f else {
                    ((dampedDragAnimation.value - valueRange.start) / range).coerceIn(0f, 1f)
                }
            }
        }
        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(shape)
                    .background(inactiveTrackColor)
                    .pointerInput(trackWidth, valueRange, isLtr) {
                        detectTapGestures { position ->
                            if (trackWidth > 0) {
                                val delta = (valueRange.endInclusive - valueRange.start) *
                                        (position.x / trackWidth)
                                val target = if (isLtr) {
                                    valueRange.start + delta
                                } else {
                                    valueRange.endInclusive - delta
                                }.coerceIn(valueRange)
                                dampedDragAnimation.animateToValue(target)
                                onValueChange(target)
                                onValueChangeFinished?.invoke()
                            }
                        }
                    }
                    .height(AppleLiquidSliderKyantDefaults.TrackHeight)
                    .fillMaxWidth()
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(activeTrackColor)
                    .height(AppleLiquidSliderKyantDefaults.TrackHeight)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * progress)
                            .fastRoundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    }
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX = (
                            -size.width / 2f + trackWidth * progress
                            ).fastCoerceIn(
                            -size.width / 4f,
                            trackWidth - size.width * 3f / 4f
                        ) * if (isLtr) 1f else -1f
                }
                .pointerInput(trackWidth, valueRange, isLtr) {
                    var dragValue = value()
                    detectDragGestures(
                        onDragStart = {
                            dragValue = value()
                            dampedDragAnimation.press()
                        },
                        onDragEnd = {
                            onValueChangeFinished?.invoke()
                            dampedDragAnimation.release()
                        },
                        onDragCancel = {
                            dampedDragAnimation.release()
                        }
                    ) { change, dragAmount ->
                        if (trackWidth > 0 && dragAmount.x != 0f) {
                            val valueDelta = (valueRange.endInclusive - valueRange.start) *
                                    (dragAmount.x / trackWidth)
                            dragValue = if (isLtr) {
                                dragValue + valueDelta
                            } else {
                                dragValue - valueDelta
                            }.coerceIn(valueRange)
                            dampedDragAnimation.updateValue(dragValue)
                            onValueChange(dragValue)
                            change.consume()
                        }
                    }
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        backdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            scale(lerp(2f / 3f, 1f, progress), lerp(0f, 1f, progress)) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { shape },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(AppleLiquidSliderKyantDefaults.BlurRadius.toPx() * (1f - progress))
                        lens(
                            10.dp.toPx() * progress,
                            14.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = { Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.05f)) },
                    innerShadow = {
                        InnerShadow(
                            radius = 4.dp * dampedDragAnimation.pressProgress,
                            alpha = dampedDragAnimation.pressProgress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                    },
                    onDrawSurface = {
                        drawRect(thumbColor.copy(alpha = 1f - dampedDragAnimation.pressProgress))
                    }
                )
                .size(
                    AppleLiquidSliderKyantDefaults.ThumbWidth,
                    AppleLiquidSliderKyantDefaults.ThumbHeight
                )
        )
    }
}
