package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.done
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionSheet(
    title: String,
    items: List<T>,
    selectedItem: T,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
    itemLabel: @Composable (T) -> String,
    // 可选：每项前缀图标，返回 null 时不显示
    itemIcon: @Composable ((T) -> ImageVector?)? = null,
    // 可选：单项是否可选中，返回 false 时该项置灰，默认全部可选中
    itemEnabled: ((T) -> Boolean)? = null,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var currentSelection by remember { mutableStateOf(selectedItem) }

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        fillMaxHeight = false,
        horizontalPadding = 8.dp,
        bottomPadding = 8.dp,
        cornerRadius = ExtraLargeRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass
    ) {
        CenterAlignedTopAppBar(
            title = { SheetTitle(title) },
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
                            contentDescription = stringResource(R.string.close),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            },
            actions = {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onConfirm(currentSelection)
                                onDismiss()
                            }
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
                            contentDescription = stringResource(R.string.confirm),
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        SegmentedColumn(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp
            ),
            outerCornerRadius = CornerRadius,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            containerColorAlpha = 0.8f
        ) {
            items.forEach { item ->
                val enabled = itemEnabled?.invoke(item) ?: true
                item(key = item) {
                    RadioButtonWidget(
                        title = itemLabel(item),
                        selected = item == currentSelection,
                        enabled = enabled,
                        onClick = { if (enabled) currentSelection = item },
                        icon = itemIcon?.invoke(item),
                        iconPlaceholder = false
                    )
                }
            }
        }
    }
}
