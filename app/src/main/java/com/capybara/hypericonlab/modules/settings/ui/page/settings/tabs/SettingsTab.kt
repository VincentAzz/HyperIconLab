package com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.blur.LiquidGlassEngine
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.BaseWidgetAction
import com.capybara.hypericonlab.core.designsystem.component.BaseWidgetActionIcon
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.StyleChip
import com.capybara.hypericonlab.core.designsystem.component.SwitchWidget
import com.capybara.hypericonlab.core.designsystem.component.isAppleStyleCardEnabled
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerSize
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.core.designsystem.util.getDisplayName
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import com.capybara.hypericonlab.modules.render.image.StickerProcessor
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsAction
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsState
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.PermissionCheckCard
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import java.util.Locale

private object ExpandableStyleChipConfig {
    val ContentPadding = 12.dp
    val ItemSpacing = 8.dp
}

@Composable
fun SettingsTab(
    uiState: ThemeSettingsState,
    viewModel: SettingsViewModel,
    paddingValues: PaddingValues,
    outerPadding: PaddingValues,
    backdrop: LayerBackdrop?,
    onShowThemeModeSheet: () -> Unit,
    onShowPaletteSheet: () -> Unit,
    onShowColorSpecSheet: () -> Unit,
    onShowLiquidGlassSheet: () -> Unit,
    onShowThemeColorSheet: () -> Unit,
    // 点击"查看运行日志"按钮回调，由 SettingsPage 提供以弹出 LogSheet
    onViewLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val appleStyleCardEnabled = isAppleStyleCardEnabled()
    val expandableStyleChipPadding = PaddingValues(
        start = ExpandableStyleChipConfig.ContentPadding,
        top = if (appleStyleCardEnabled) 0.dp else ExpandableStyleChipConfig.ContentPadding,
        end = ExpandableStyleChipConfig.ContentPadding,
        bottom = ExpandableStyleChipConfig.ContentPadding
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                layoutDirection
            ),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                layoutDirection
            ),
            bottom = outerPadding.calculateBottomPadding()
        )
    ) {
        // 权限检查卡片：仅在权限未授权时显示（已授权则自动隐藏，不占空间）
        item {
            PermissionCheckCard()
        }
        item {
            ThemeAndColorSettings(
                uiState = uiState,
                viewModel = viewModel,
                onShowThemeModeSheet = onShowThemeModeSheet,
                onShowPaletteSheet = onShowPaletteSheet,
                onShowColorSpecSheet = onShowColorSpecSheet,
                onShowThemeColorSheet = onShowThemeColorSheet
            )
        }
        item {
            SegmentedColumn(
                title = "组件风格"
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_smoother_rounded_corners),
                            description = stringResource(R.string.theme_settings_use_smoother_rounded_corners_desc),
                            checked = uiState.useSmootherRoundedCorners,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseSmootherRoundedCorners(
                                        it
                                    )
                                )
                            }
                        )
                    }
                }
                expandableItem(
                    expanded = uiState.useCustomCardCornerRadius,
                    topContent = {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_custom_card_corner_radius),
                            description = stringResource(R.string.theme_settings_use_custom_card_corner_radius_desc),
                            checked = uiState.useCustomCardCornerRadius,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseCustomCardCornerRadius(it)
                                )
                            }
                        )
                    },
                    bottomContent = { shape ->
                        BaseItemContainer(shape = shape) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(expandableStyleChipPadding),
                                horizontalArrangement = Arrangement.spacedBy(
                                    ExpandableStyleChipConfig.ItemSpacing
                                )
                            ) {
                                CardCornerSize.entries.forEach { size ->
                                    StyleChip(
                                        label = stringResource(
                                            when (size) {
                                                CardCornerSize.DEFAULT -> R.string.theme_settings_card_corner_default
                                                CardCornerSize.LARGE -> R.string.theme_settings_card_corner_large
                                            }
                                        ),
                                        selected = uiState.cardCornerSize == size,
                                        onClick = {
                                            viewModel.dispatch(
                                                ThemeSettingsAction.SetCardCornerSize(size)
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                )
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = "使用 Apple 风格卡片",
                        description = "启用后卡片组将使用无间距连接样式及分割线",
                        checked = uiState.useAppleStyleCard,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseAppleStyleCard(
                                    it
                                )
                            )
                        }
                    )
                }
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_show_sheet_card_background),
                        description = stringResource(R.string.theme_settings_show_sheet_card_background_desc),
                        checked = uiState.useSheetCardBackground,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseSheetCardBackground(it)
                            )
                        }
                    )
                }
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_use_google_sans_flex),
                        description = stringResource(R.string.theme_settings_use_google_sans_flex_desc),
                        checked = uiState.useGoogleSansFlex,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseGoogleSansFlex(
                                    it
                                )
                            )
                        }
                    )
                }
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_use_tab_row_center_alignment),
                        description = stringResource(R.string.theme_settings_use_tab_row_center_alignment_desc),
                        checked = uiState.useTabRowCenterAlignment,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseTabRowCenterAlignment(
                                    it
                                )
                            )
                        }
                    )
                }
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_use_tab_row_transparent_background),
                        description = stringResource(R.string.theme_settings_use_tab_row_transparent_background_desc),
                        checked = uiState.useTabRowTransparentBackground,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseTabRowTransparentBackground(
                                    it
                                )
                            )
                        }
                    )
                }
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_use_tab_row_fill_width),
                        description = stringResource(R.string.theme_settings_use_tab_row_fill_width_desc),
                        checked = uiState.useTabRowFillWidth,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseTabRowFillWidth(
                                    it
                                )
                            )
                        }
                    )
                }
                item {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_use_floating_bottom_bar),
                        description = stringResource(R.string.theme_settings_use_floating_bottom_bar_desc),
                        checked = uiState.useFloatingBottomBar,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseFloatingBottomBar(
                                    it
                                )
                            )
                        }
                    )
                }
                expandableItem(
                    animatedVisibility = uiState.useFloatingBottomBar,
                    expanded = uiState.useFloatingBottomBarCompact,
                    topContent = {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_floating_bottom_bar_compact),
                            description = stringResource(R.string.theme_settings_use_floating_bottom_bar_compact_desc),
                            checked = uiState.useFloatingBottomBarCompact,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseFloatingBarCompact(it)
                                )
                            }
                        )
                    },
                    bottomContent = { shape ->
                        BaseItemContainer(shape = shape) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(expandableStyleChipPadding),
                                verticalArrangement = Arrangement.spacedBy(
                                    ExpandableStyleChipConfig.ItemSpacing
                                )
                            ) {
                                FloatingBottomBarCompactType.entries.chunked(2)
                                    .forEach { rowEntries ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(
                                                ExpandableStyleChipConfig.ItemSpacing
                                            )
                                        ) {
                                            rowEntries.forEach { entry ->
                                                StyleChip(
                                                    label = entry.displayName,
                                                    selected = uiState.floatingBottomBarCompactType == entry,
                                                    onClick = {
                                                        viewModel.dispatch(
                                                            ThemeSettingsAction.SetFloatingBottomBarCompactType(
                                                                entry
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }
                )
            }
        }

        item {
            SegmentedColumn(
                title = "组件材质"
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_blur),
                            description = stringResource(R.string.theme_settings_use_blur_desc),
                            checked = uiState.useBlur,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseBlur(
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item(animatedVisibility = uiState.useBlur) {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_progressive_blur_top_app_bar),
                            description = stringResource(R.string.theme_settings_use_progressive_blur_top_app_bar_desc),
                            checked = uiState.useProgressiveBlurTopAppBar,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseProgressiveBlurTopAppBar(
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item(animatedVisibility = uiState.useFloatingBottomBar) {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_floating_bottom_bar_blur),
                            description = stringResource(R.string.theme_settings_use_floating_bottom_bar_blur_desc),
                            checked = uiState.useFloatingBottomBarBlur,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseFloatingBottomBarBlur(
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item(animatedVisibility = uiState.useBlur) {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_liquid_glass_bottom_sheet),
                            description = stringResource(R.string.theme_settings_use_liquid_glass_bottom_sheet_desc),
                            checked = uiState.useLiquidGlassBottomSheet,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseLiquidGlassBottomSheet(
                                        it
                                    )
                                )
                            }
                        )
                    }
                    item {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_apple_style_toggle),
                            description = stringResource(
                                R.string.theme_settings_use_apple_style_toggle_desc
                            ),
                            checked = uiState.useAppleStyleToggle,
                            enabled = uiState.liquidGlassEngine == LiquidGlassEngine.KYANT,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseAppleStyleToggle(it)
                                )
                            }
                        )
                    }
                    item {
                        SwitchWidget(
                            iconPlaceholder = false,
                            title = stringResource(R.string.theme_settings_use_apple_style_slider),
                            description = stringResource(
                                R.string.theme_settings_use_apple_style_slider_desc
                            ),
                            checked = uiState.useAppleStyleSlider,
                            enabled = uiState.liquidGlassEngine == LiquidGlassEngine.KYANT,
                            onCheckedChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetUseAppleStyleSlider(it)
                                )
                            }
                        )
                    }
                    item(animatedVisibility = uiState.useBlur) {
                        BaseWidget(
                            iconPlaceholder = false,
                            title = "自定义液态玻璃",
                            description = "调整液态玻璃引擎和参数。",
                            onClick = onShowLiquidGlassSheet,
                            trailingContent = {
                                BaseWidgetAction(
                                    icon = BaseWidgetActionIcon.CHEVRON_RIGHT
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            PerformanceAndCacheSettings()
        }

        item {
            // 运行日志入口卡片：点击"查看"按钮弹出 LogSheet；日志较少查看，置于设置页底部
            RunLogSettings(onViewLog = onViewLog)
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun ThemeAndColorSettings(
    uiState: ThemeSettingsState,
    viewModel: SettingsViewModel,
    onShowThemeModeSheet: () -> Unit,
    onShowPaletteSheet: () -> Unit,
    onShowColorSpecSheet: () -> Unit,
    onShowThemeColorSheet: () -> Unit
) {
    SegmentedColumn(title = "主题与配色") {
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = stringResource(R.string.theme_settings_theme_mode),
                onClick = onShowThemeModeSheet,
                trailingContent = {
                    BaseWidgetAction(
                        statusText = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                            ThemeMode.MIUIX_DEFAULT_LIGHT -> stringResource(R.string.theme_settings_theme_mode_miuix_default_light)
                            ThemeMode.MIUIX_DEFAULT_DARK -> stringResource(R.string.theme_settings_theme_mode_miuix_default_dark)
                            ThemeMode.MIUIX_DEFAULT_SYSTEM -> stringResource(R.string.theme_settings_theme_mode_miuix_default_system)
                        }
                    )
                }
            )
        }
        if (!uiState.themeMode.usesMiuixDefaultPalette) {
            item {
                BaseWidget(
                    iconPlaceholder = false,
                    title = stringResource(R.string.theme_settings_palette_style),
                    onClick = onShowPaletteSheet,
                    trailingContent = {
                        BaseWidgetAction(statusText = uiState.paletteStyle.displayName)
                    }
                )
            }
            item {
                val isSpec2025Supported = uiState.paletteStyle.supportsSpec2025
                val activeSpec =
                    if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else uiState.colorSpec
                val descriptionText = if (!isSpec2025Supported) {
                    stringResource(id = R.string.theme_settings_color_spec_only_2021)
                } else {
                    activeSpec.displayName
                }
                BaseWidget(
                    iconPlaceholder = false,
                    title = stringResource(id = R.string.theme_settings_color_spec),
                    description = descriptionText.takeIf { !isSpec2025Supported },
                    enabled = isSpec2025Supported,
                    onClick = onShowColorSpecSheet,
                    trailingContent = {
                        BaseWidgetAction(
                            statusText = activeSpec.displayName.takeIf { isSpec2025Supported }
                        )
                    }
                )
            }
            item {
                SwitchWidget(
                    iconPlaceholder = false,
                    title = stringResource(R.string.theme_settings_dynamic_color),
                    description = stringResource(R.string.theme_settings_dynamic_color_desc),
                    checked = uiState.useDynamicColor,
                    onCheckedChange = {
                        viewModel.dispatch(ThemeSettingsAction.SetUseDynamicColor(it))
                    }
                )
            }
            item {
                val selectedColor =
                    uiState.availableColors.firstOrNull { it.color == uiState.seedColor }
                BaseWidget(
                    iconPlaceholder = false,
                    title = "主题颜色",
                    enabled = !uiState.useDynamicColor,
                    onClick = onShowThemeColorSheet,
                    trailingContent = {
                        BaseWidgetAction(
                            statusText = selectedColor?.getDisplayName() ?: "自定义",
                            icon = BaseWidgetActionIcon.EXPAND_ALL
                        )
                    }
                )
            }
        }
    }
}
@Composable
private fun PerformanceAndCacheSettings(
    viewModel: IconViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val useStreaming by viewModel.useStreaming.collectAsStateWithLifecycle()
    var cacheSize by remember { mutableStateOf("正在计算...") }
    val stickerProcessor = StickerProcessor

    fun updateSize() {
        val size = try {
            val dir = stickerProcessor.getCacheDir(context)
            val bytes = dir.walk().filter { it.isFile }.sumOf { it.length() }
            if (bytes < 1024 * 1024) {
                String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
            } else {
                String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
            }
        } catch (_: Exception) {
            "0 KB"
        }
        cacheSize = size
    }

    LaunchedEffect(Unit) {
        updateSize()
    }

    SegmentedColumn(title = "性能与缓存") {
        item {
            SwitchWidget(
                iconPlaceholder = false,
                title = "流式打包模式",
                description = "通过流式打包图标来节省内存",
                checked = useStreaming,
                onCheckedChange = { viewModel.useStreaming.value = it }
            )
        }
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "清除构建缓存",
                description = "当前占用: $cacheSize",
                trailingContent = {
                    PrimaryActionButton(
                        text = "清除",
                        onClick = {
                            try {
                                val dir = stickerProcessor.getCacheDir(context)
                                dir.deleteRecursively()
                                updateSize()
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
            )
        }
    }
}

/**
 * 运行日志入口卡片：与"清除贴纸缓存"风格一致，点击"查看"按钮弹出 LogSheet。
 * 日志较少查看，置于设置页底部。
 *
 * @param onViewLog 点击"查看"按钮回调
 */
@Composable
fun RunLogSettings(onViewLog: () -> Unit) {
    SegmentedColumn(title = "调试") {
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "运行日志",
                description = "查看图标生成与构建过程的运行日志",
                onClick = onViewLog,
                trailingContent = {
                    BaseWidgetAction(
                        statusText = "查看",
                        icon = BaseWidgetActionIcon.CHEVRON_RIGHT
                    )
                }
            )
        }
    }
}
