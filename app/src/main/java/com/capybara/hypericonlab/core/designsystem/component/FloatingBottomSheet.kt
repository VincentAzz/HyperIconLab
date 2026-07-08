package com.capybara.hypericonlab.core.designsystem.component

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.liquidglass.liquidGlassEffect
import com.capybara.hypericonlab.core.designsystem.liquidglass.material3BlurEffect
import com.capybara.hypericonlab.core.designsystem.theme.BottomSheetCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberMiuixSquircleShape
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * Floating BottomSheet component.
 *
 * @param onDismiss Callback to be invoked when the sheet is dismissed.
 * @param modifier Modifier for the sheet content.
 * @param sheetState State of the bottom sheet.
 * @param horizontalPadding Horizontal padding from the screen edges.
 * @param bottomPadding Bottom padding from the screen edge.
 * @param cornerRadius Radius for all four corners (squircle when enabled).
 * @param containerColor Color of the sheet's container.
 * @param tonalElevation Tonal elevation of the sheet's surface.
 * @param scrimColor Color of the background scrim.
 * @param dragHandle Optional drag handle composable.
 * @param fillMaxHeight Whether to fill the screen height.
 * @param backdrop Optional backdrop for blur effect.
 * @param useLiquidGlass When true and a backdrop is present, apply a liquid glass effect
 *  (refraction + edge highlight) instead of the standard blur. Falls back to standard blur
 *  when runtime shaders are unsupported. Requires a CornerBasedShape, so squircle is disabled
 *  in this mode.
 * @param liquidGlassBlurRadius Gaussian blur radius used by the liquid glass effect; ignored when
 *  [useLiquidGlass] is false. Defaults to 24.dp.
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
    cornerRadius: Dp = BottomSheetCornerRadius,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: Dp = 2.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.32f),
    dragHandle: @Composable (() -> Unit)? = null,
    fillMaxHeight: Boolean = true,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(Unit) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetState.show()
        }
    }

    // Liquid glass requires a CornerBasedShape (lens SDF); squircle paths are incompatible.
    val shape = if (useLiquidGlass && backdrop != null) {
        RoundedCornerShape(cornerRadius)
    } else {
        rememberMiuixSquircleShape(cornerRadius)
    }

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
                        Modifier.liquidGlassEffect(
                            backdrop = backdrop,
                            shape = shape,
                            cornerRadius = cornerRadius,
                            blurRadius = liquidGlassBlurRadius
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
            color = if (backdrop != null) Color.Transparent else containerColor,
            shape = shape,
            tonalElevation = tonalElevation
        ) {
            Column(
                modifier = modifier.then(if (fillMaxHeight) Modifier.fillMaxSize() else Modifier.wrapContentHeight())
            ) {
                content()
            }
        }
    }
}
