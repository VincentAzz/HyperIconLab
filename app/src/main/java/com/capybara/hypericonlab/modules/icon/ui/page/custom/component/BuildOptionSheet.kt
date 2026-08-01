package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import com.capybara.hypericonlab.modules.icon.domain.model.ProductType
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

private object BuildOptionSheetConfig {
    // Header 圆形按钮尺寸
    val HEADER_ICON_SIZE = 40.dp

    // Header 圆形按钮内部图标尺寸
    val HEADER_ICON_INNER_SIZE = 24.dp

    // Header 关闭按钮左侧 padding
    val HEADER_ICON_LEADING_PADDING = 12.dp

    // Header 确认按钮右侧 padding
    val HEADER_ICON_TRAILING_PADDING = 12.dp

    // 内容区水平内边距
    val CONTENT_HORIZONTAL_PADDING = 16.dp

    // 内容区底部内边距（避免最后一张卡片紧贴 sheet 底部）
    val CONTENT_BOTTOM_PADDING = 16.dp

    // chip 之间垂直间距（与 ForegroundTab 一致）
    val CHIP_SPACING = 8.dp

    // 禁用态透明度
    const val DISABLED_ALPHA = 0.38f
}


/**
 * 构建选项 Sheet：选择产物类型与图标集，确认后回调 [onConfirm]。
 *
 * 风格：复用 [FloatingBottomSheet] + [SheetTitle] + [ConfigCard] + [StyleChip]，
 * 与自定义页面（前景/背景 tab）的卡片+chip 风格保持一致。
 *
 * - 产物类型：仅展示 [ProductType.enabled] = true 的项；单选；一行一个
 * - 图标集：展示 [iconSets] 列表（full/filtered/test），单选；一行一个
 * - 确认按钮：未选中任一项时禁用（alpha=0.38）
 *
 * 一行一个 chip 的原因：产物类型与图标集文本较长（如 "zip (仅图标)"、"filtered · N 个图标"），
 * 一行两个会换行或截断，改为 fillMaxWidth 单列展示，间距与圆角参考 ForegroundTab。
 *
 * @param iconSets 可用图标集列表（由 IconViewModel.availableIconSets 提供）
 * @param onConfirm 回调参数为 (产物类型, 图标集)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildOptionSheet(
    onDismiss: () -> Unit,
    iconSets: List<IconSetInfo>,
    apkEnabled: Boolean,
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
        mutableStateOf(ProductType.entries.first { it.enabled })
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
        fillMaxHeight = false
    ) {
        // Header：与项目其他 sheet 一致的 CenterAlignedTopAppBar + 关闭/确认按钮
        CenterAlignedTopAppBar(
            title = { SheetTitle("从自定义构建") },
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
                        .padding(start = BuildOptionSheetConfig.HEADER_ICON_LEADING_PADDING)
                        .size(BuildOptionSheetConfig.HEADER_ICON_SIZE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(BuildOptionSheetConfig.HEADER_ICON_INNER_SIZE),
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
                        .padding(end = BuildOptionSheetConfig.HEADER_ICON_TRAILING_PADDING)
                        .size(BuildOptionSheetConfig.HEADER_ICON_SIZE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            AppMaterialSymbols.check,
                            contentDescription = "确定",
                            modifier = Modifier
                                .size(BuildOptionSheetConfig.HEADER_ICON_INNER_SIZE)
                                .alpha(if (enabled) 1f else BuildOptionSheetConfig.DISABLED_ALPHA),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        )

        // 内容：两个 ConfigCard，chip 一行一个，间距 8dp，与 ForegroundTab 风格一致
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BuildOptionSheetConfig.CONTENT_HORIZONTAL_PADDING)
                .padding(bottom = BuildOptionSheetConfig.CONTENT_BOTTOM_PADDING)
        ) {
            // val cardContainerColor = MaterialTheme.colorScheme.surfaceBright
            val cardContainerColor = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.8f)

            // 产物类型卡片
            ConfigCard(
                title = "产物类型",
                containerColor = cardContainerColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(BuildOptionSheetConfig.CHIP_SPACING)) {
                    ProductType.entries.filter { it.enabled }.forEach { productType ->
                        val optionEnabled = productType != ProductType.APK || apkEnabled
                        StyleChip(
                            label = productType.label,
                            selected = selectedProductType == productType,
                            onClick = { selectedProductType = productType },
                            enabled = optionEnabled,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!apkEnabled) {
                        Text(
                            text = "APK 仅支持已下载配套模板的云端 Lawnicons。请前往资产页检查更新。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 图标集卡片
            ConfigCard(
                title = "图标集",
                containerColor = cardContainerColor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(BuildOptionSheetConfig.CHIP_SPACING)) {
                    iconSets.forEach { iconSet ->
                        StyleChip(
                            label = "${iconSet.label} · ${iconSet.iconCount} 映射",
                            selected = selectedIconSet?.id == iconSet.id,
                            onClick = { selectedIconSet = iconSet },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
