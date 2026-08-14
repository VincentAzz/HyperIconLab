package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.blur.miuix.liquidGlassEffect
import com.capybara.hypericonlab.core.designsystem.blur.miuix.material3BlurEffect
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private object MiuixSheetConfig {
    val BlurRadius = 24.dp
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
fun FloatingBottomSheetMiuix(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    ),
    horizontalPadding: Dp = 12.dp,
    bottomPadding: Dp = 8.dp,
    cornerRadius: Dp,
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
    val hasBackdrop = backdrop != null

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
                    if (useLiquidGlass && backdrop != null) {
                        Modifier
                            .clip(shape)
                            .liquidGlassEffect(
                                backdrop = backdrop,
                                shape = liquidGlassShape,
                                cornerRadius = cornerRadius,
                                blurRadius = MiuixSheetConfig.BlurRadius
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


