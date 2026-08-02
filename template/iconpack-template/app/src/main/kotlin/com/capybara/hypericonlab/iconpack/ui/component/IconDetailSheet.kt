package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.IconEntry
import com.capybara.hypericonlab.iconpack.ui.symbol.close
import com.capybara.hypericonlab.iconpack.ui.symbol.done
import com.capybara.hypericonlab.iconpack.ui.theme.AppMaterialSymbols
import kotlinx.coroutines.launch

private object IconDetailConfig {
    val TOP_PADDING = 16.dp
    val ICON_CONTAINER_SIZE = 112.dp
    val ICON_SIZE = 80.dp
    val ICON_BOTTOM_SPACING = 8.dp
    val ROW_HORIZONTAL_PADDING = 16.dp
    val ROW_VERTICAL_PADDING = 12.dp
    val LABEL_BOTTOM_PADDING = 4.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconDetailSheet(
    entry: IconEntry,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        fillMaxHeight = false
    ) {
        CenterAlignedTopAppBar(
            title = { SheetTitle("图标详情") },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            navigationIcon = {
                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            },
            actions = {
                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.done,
                            contentDescription = "确认",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = IconDetailConfig.TOP_PADDING)
                    .size(IconDetailConfig.ICON_CONTAINER_SIZE),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(entry.drawableId),
                    contentDescription = entry.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(IconDetailConfig.ICON_SIZE)
                )
            }

            Spacer(modifier = Modifier.size(IconDetailConfig.ICON_BOTTOM_SPACING))

            SegmentedColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "name") {
                    IconDetailRow(
                        label = "名称",
                        value = entry.displayName,
                        shape = it
                    )
                }
                item(key = "drawable") {
                    IconDetailRow(
                        label = "Drawable",
                        value = entry.drawable,
                        shape = it
                    )
                }
                item(key = "package") {
                    IconDetailRow(
                        label = "包名",
                        value = entry.packageName?.takeIf(String::isNotBlank) ?: "未关联应用",
                        shape = it
                    )
                }
            }
        }
    }
}

@Composable
private fun IconDetailRow(
    label: String,
    value: String,
    shape: Shape
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = IconDetailConfig.ROW_HORIZONTAL_PADDING,
                vertical = IconDetailConfig.ROW_VERTICAL_PADDING
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = IconDetailConfig.LABEL_BOTTOM_PADDING)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
