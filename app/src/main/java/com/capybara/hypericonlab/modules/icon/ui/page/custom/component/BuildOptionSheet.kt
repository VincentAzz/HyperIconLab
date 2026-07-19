package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.modules.icon.domain.model.ProductType
import com.capybara.hypericonlab.modules.icon.ui.page.custom.IconSetInfo
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

/**
 * 构建选项 Sheet：选择产物类型与图标集，确认后回调 [onConfirm]。
 *
 * 风格：复用 [FloatingBottomSheet] + [SheetTitle] + [SegmentedColumn] + [BaseWidget]，
 * 与项目其他 sheet（如 MaskPickerSheet、ColorPickerSheet）保持一致。
 *
 * - 产物类型：仅展示 [ProductType.enabled] = true 的项；单选
 * - 图标集：展示 [iconSets] 列表，单选；图标数量以 GoogleSansCode 字体展示
 * - 确认按钮：未选中任一项时禁用（alpha=0.38）
 *
 * @param iconSets 可用图标集列表（由 IconViewModel.availableIconSets 提供）
 * @param onConfirm 回调参数为 (产物类型, 图标集)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildOptionSheet(
    onDismiss: () -> Unit,
    iconSets: List<IconSetInfo>,
    onConfirm: (ProductType, IconSetInfo) -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp = ExtraLargeRadius,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    // 默认选中第一个 enabled 的产物类型与第一个图标集
    var selectedProductType by remember {
        mutableStateOf(ProductType.values().first { it.enabled })
    }
    var selectedIconSet by remember {
        mutableStateOf<IconSetInfo?>(iconSets.firstOrNull())
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding,
        cornerRadius = cornerRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius,
    ) {
        // Header
        CenterAlignedTopAppBar(
            title = { SheetTitle("构建") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            navigationIcon = {
                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onDismiss()
                        }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
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
                val enabled = selectedIconSet != null
                Surface(
                    onClick = {
                        if (enabled) {
                            val iconSet = selectedIconSet
                            if (iconSet != null) {
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        onConfirm(selectedProductType, iconSet)
                                    }
                                }
                            }
                        }
                    },
                    enabled = enabled,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.check,
                            contentDescription = "确定",
                            modifier = Modifier
                                .size(24.dp)
                                .alpha(if (enabled) 1f else 0.38f),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // 产物类型选择（仅展示 enabled=true 的项）
            SegmentedColumn(title = "产物类型") {
                ProductType.values().filter { it.enabled }.forEach { productType ->
                    item {
                        BaseWidget(
                            title = productType.label,
                            selected = selectedProductType == productType,
                            iconPlaceholder = false,
                            onClick = { selectedProductType = productType }
                        )
                    }
                }
            }

            // 图标集列表
            Text(
                text = "图标集",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 16.dp,
                    bottom = 8.dp
                )
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(iconSets, key = { it.id }) { iconSet ->
                    BaseWidget(
                        title = iconSet.label,
                        description = "图标数量：${iconSet.iconCount}",
                        descriptionColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selected = selectedIconSet?.id == iconSet.id,
                        iconPlaceholder = false,
                        onClick = { selectedIconSet = iconSet }
                    )
                }
            }
        }
    }
}
