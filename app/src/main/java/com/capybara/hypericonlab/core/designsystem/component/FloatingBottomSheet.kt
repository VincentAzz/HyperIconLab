package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.backdrops.LocalKyantBackdrop
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.drawBackdrop
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.effects.blur
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.effects.colorControls
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.effects.lens
import com.capybara.hypericonlab.core.designsystem.liquidglass.liquidGlassEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.material3BlurEffect
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantCapsuleShape
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private object KyantSheetConfig {
    const val LightSurfaceAlpha = 0.6f
    const val DarkSurfaceAlpha = LightSurfaceAlpha
    const val LightBrightness = 0.2f
    const val Saturation = 1.5f
    val LightBlurRadius = 16.dp
    val DarkBlurRadius = 8.dp
    val RefractionHeight = 24.dp
    val RefractionAmount = 48.dp
    val MiuixBlurRadius = 24.dp
}

/**
 * Floating BottomSheet component.
 *
 * @param onDismiss Callback to be invoked when the sheet is dismissed.
 * @param modifier Modifier for the sheet content.
 * @param sheetState State of the bottom sheet.
 * @param horizontalPadding Horizontal padding from the screen edges.
 * @param bottomPadding Bottom padding from the screen edge.
 * @param cornerRadius Radius for all four corners (smoother rounded corners when enabled).
 * @param containerColor Color of the sheet's container.
 * @param tonalElevation Tonal elevation of the sheet's surface.
 * @param scrimColor Color of the background scrim.
 * @param dragHandle Optional drag handle composable.
 * @param fillMaxHeight Whether to fill the screen height.
 * @param backdrop Optional backdrop for blur effect.
 * @param useLiquidGlass When true and a backdrop is present, apply a liquid glass effect
 *  (refraction + edge highlight) instead of the standard blur. Falls back to standard blur
 *  when runtime shaders are unsupported. Supports the design system's smoother rounded rectangle.
 * @param content The content of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    ),
    horizontalPadding: Dp = 12.dp,
    bottomPadding: Dp = 8.dp,
    cornerRadius: Dp = ExtraLargeRadius,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: Dp = 2.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    dragHandle: @Composable (() -> Unit)? = null,
    fillMaxHeight: Boolean = true,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(Unit) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetState.show()
        }
    }

    val shape = rememberKyantRoundedRectangleShape(cornerRadius)
    val liquidGlassShape = RoundedCornerShape(cornerRadius)
    val kyantBackdrop = LocalKyantBackdrop.current
    val hasBackdrop = backdrop != null || (useLiquidGlass && kyantBackdrop != null)
    val isDarkTheme = AppTheme.isDark
    val kyantContainerColor = containerColor.copy(
        alpha = if (isDarkTheme) {
            KyantSheetConfig.DarkSurfaceAlpha
        } else {
            KyantSheetConfig.LightSurfaceAlpha
        }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = dragHandle,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        scrimColor = scrimColor,
        modifier = if (fillMaxHeight) Modifier.fillMaxSize() else Modifier
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(bottom = bottomPadding)
                .then(if (fillMaxHeight) Modifier.fillMaxSize() else Modifier.wrapContentHeight())
                .then(
                    if (useLiquidGlass && kyantBackdrop != null) {
                        Modifier
                            .clip(shape)
                            .drawBackdrop(
                                backdrop = kyantBackdrop,
                                shape = { shape },
                                effects = {
                                    colorControls(
                                        brightness = if (isDarkTheme) 0f else KyantSheetConfig.LightBrightness,
                                        saturation = KyantSheetConfig.Saturation
                                    )
                                    blur(
                                        (if (isDarkTheme) {
                                            KyantSheetConfig.DarkBlurRadius
                                        } else {
                                            KyantSheetConfig.LightBlurRadius
                                        }).toPx()
                                    )
                                    lens(
                                        refractionHeight = KyantSheetConfig.RefractionHeight.toPx(),
                                        refractionAmount = KyantSheetConfig.RefractionAmount.toPx(),
                                        depthEffect = true
                                    )
                                },
                                highlight = null,
                                onDrawSurface = { drawRect(kyantContainerColor) }
                            )
                    } else if (useLiquidGlass && backdrop != null) {
                        Modifier
                            .clip(shape)
                            .liquidGlassEffect(
                                backdrop = backdrop,
                                shape = liquidGlassShape,
                                cornerRadius = cornerRadius,
                                blurRadius = KyantSheetConfig.MiuixBlurRadius
                            )
                    } else if (backdrop != null) {
                        Modifier.material3BlurEffect(
                            backdrop = backdrop,
                            enabled = true,
                            shape = shape
                        )
                    } else {
                        Modifier
                    }
                ),
            color = if (hasBackdrop) Color.Transparent else containerColor,
            shape = shape,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = modifier
                    .then(if (fillMaxHeight) Modifier.fillMaxSize() else Modifier.wrapContentHeight())
                    .then(
                        if (!fillMaxHeight) Modifier.animateContentSize(
                            animationSpec = tween(250),
                            alignment = Alignment.TopStart
                        ) else Modifier
                    )
            ) {
                content()
            }
        }
    }
}


@Composable
fun SheetTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
    val contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    val titleHeight = 40.dp
    // val capsuleRadius = titleHeight / 2

    Surface(
        modifier = modifier
            .height(titleHeight)
            .padding(horizontal = 8.dp),
        // shape = rememberKyantRoundedRectangleShape(capsuleRadius),
        shape = rememberKyantCapsuleShape(),
        color = backgroundColor

    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
