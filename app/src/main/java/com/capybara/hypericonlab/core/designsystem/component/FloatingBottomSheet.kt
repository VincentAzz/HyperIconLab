package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.backdrops.LocalKyantBackdrop
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantCapsuleShape
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 根据液态玻璃引擎分发底部弹层实现。
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
    val kyantBackdrop = LocalKyantBackdrop.current
    if (useLiquidGlass && kyantBackdrop != null) {
        FloatingBottomSheetKyant(
            onDismiss = onDismiss,
            modifier = modifier,
            sheetState = sheetState,
            horizontalPadding = horizontalPadding,
            bottomPadding = bottomPadding,
            cornerRadius = cornerRadius,
            containerColor = containerColor,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            fillMaxHeight = fillMaxHeight,
            backdrop = kyantBackdrop,
            content = content
        )
    } else {
        FloatingBottomSheetMiuix(
            onDismiss = onDismiss,
            modifier = modifier,
            sheetState = sheetState,
            horizontalPadding = horizontalPadding,
            bottomPadding = bottomPadding,
            cornerRadius = cornerRadius,
            containerColor = containerColor,
            tonalElevation = tonalElevation,
            scrimColor = scrimColor,
            dragHandle = dragHandle,
            fillMaxHeight = fillMaxHeight,
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            content = content
        )
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

    Surface(
        modifier = modifier
            .height(titleHeight)
            .padding(horizontal = 8.dp),
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
