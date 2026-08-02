package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.theme.ExtraLargeRadius
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantCapsuleShape
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantRoundedRectangleShape

private object FloatingBottomSheetConfig {
    val HORIZONTAL_PADDING = 12.dp
    val BOTTOM_PADDING = 8.dp
    val TONAL_ELEVATION = 2.dp
    const val SCRIM_ALPHA = 0.32f
}

/**
 * Floating BottomSheet component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    ),
    horizontalPadding: Dp = FloatingBottomSheetConfig.HORIZONTAL_PADDING,
    bottomPadding: Dp = FloatingBottomSheetConfig.BOTTOM_PADDING,
    cornerRadius: Dp = ExtraLargeRadius,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation: Dp = FloatingBottomSheetConfig.TONAL_ELEVATION,
    scrimColor: Color = Color.Black.copy(alpha = FloatingBottomSheetConfig.SCRIM_ALPHA),
    dragHandle: @Composable (() -> Unit)? = null,
    fillMaxHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(Unit) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetState.show()
        }
    }

    val shape = rememberKyantRoundedRectangleShape(cornerRadius)

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
                .then(if (fillMaxHeight) Modifier.fillMaxSize() else Modifier.wrapContentHeight()),
            color = containerColor,
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
