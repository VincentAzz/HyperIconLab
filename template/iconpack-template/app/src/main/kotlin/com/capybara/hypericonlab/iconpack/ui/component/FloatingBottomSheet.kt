package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.theme.ExtraLargeRadius
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantRoundedRectangleShape

private object FloatingBottomSheetConfig {
    val HORIZONTAL_PADDING = 12.dp
    val BOTTOM_PADDING = 8.dp
    val TONAL_ELEVATION = 2.dp
    const val SCRIM_ALPHA = 0.32f
}

/**
 * 主应用 FloatingBottomSheet 的模板精简版，仅保留浮动容器与关闭交互。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden
    )
    val shape = rememberKyantRoundedRectangleShape(ExtraLargeRadius)

    LaunchedEffect(sheetState) {
        if (sheetState.currentValue == SheetValue.Hidden) {
            sheetState.show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = FloatingBottomSheetConfig.SCRIM_ALPHA)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = FloatingBottomSheetConfig.HORIZONTAL_PADDING)
                .padding(bottom = FloatingBottomSheetConfig.BOTTOM_PADDING)
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = shape,
            tonalElevation = FloatingBottomSheetConfig.TONAL_ELEVATION
        ) {
            Column(
                modifier = modifier.wrapContentHeight(),
                content = content
            )
        }
    }
}
