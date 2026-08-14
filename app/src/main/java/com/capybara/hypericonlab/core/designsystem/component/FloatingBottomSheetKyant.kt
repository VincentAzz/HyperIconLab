package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.blur.kyant.backdrops.LayerBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.LocalKyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.blur.kyant.drawBackdrop
import com.capybara.hypericonlab.core.designsystem.blur.kyant.effects.blur
import com.capybara.hypericonlab.core.designsystem.blur.kyant.effects.colorControls
import com.capybara.hypericonlab.core.designsystem.blur.kyant.effects.lens
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme

private object KyantSheetConfig {
    val LightSurfaceAlpha = 0.6f
    val DarkSurfaceAlpha = LightSurfaceAlpha
    val LightBrightness = 0.1f
    val Saturation = 1.5f
    val LightBlurRadius = 16.dp
    val DarkBlurRadius = 8.dp
    val RefractionHeight = 24.dp
    val RefractionAmount = 48.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBottomSheetKyant(
    onDismiss: () -> Unit,
    modifier: Modifier,
    sheetState: SheetState,
    horizontalPadding: Dp,
    bottomPadding: Dp,
    cornerRadius: Dp,
    containerColor: Color,
    tonalElevation: Dp,
    scrimColor: Color,
    dragHandle: @Composable (() -> Unit)?,
    fillMaxHeight: Boolean,
    backdrop: LayerBackdrop,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(Unit) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetState.show()
        }
    }

    val shape = rememberKyantRoundedRectangleShape(cornerRadius)
    val isDarkTheme = AppTheme.isDark
    val tuning = LocalKyantGlassTuning.current
    val surfaceColor = containerColor.copy(
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
                .clip(shape)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        colorControls(
                            brightness = if (isDarkTheme) 0f else KyantSheetConfig.LightBrightness,
                            saturation = KyantSheetConfig.Saturation
                        )
                        val blurRadius = if (isDarkTheme) {
                            KyantSheetConfig.DarkBlurRadius
                        } else {
                            KyantSheetConfig.LightBlurRadius
                        }
                        blur(blurRadius.toPx() * tuning.blurScale)
                        lens(
                            refractionHeight = (
                                    KyantSheetConfig.RefractionHeight.toPx() *
                                            tuning.refractionHeightScale
                                    ).coerceAtMost(size.minDimension / 2f),
                            refractionAmount = (
                                    KyantSheetConfig.RefractionAmount.toPx() *
                                            tuning.refractionAmountScale
                                    ).coerceAtMost(size.minDimension),
                            depthEffect = true,
                            chromaticAberrationIntensity = tuning.chromaticAberration
                        )
                    },
                    highlight = null,
                    onDrawSurface = { drawRect(surfaceColor) }
                ),
            color = Color.Transparent,
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
