package com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.core.designsystem.component.SwitchWidget
import com.capybara.hypericonlab.core.designsystem.theme.FloatingBottomBarCompactType
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.core.image.StickerProcessor
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ColorSwatchPreview
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import com.capybara.hypericonlab.modules.icon.viewmodel.AppM3PreprocessManager
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsAction
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeSettingsState
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsViewModel
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.PermissionCheckCard
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import java.util.Locale

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
    // 点击"查看运行日志"按钮回调，由 SettingsPage 提供以弹出 LogSheet
    onViewLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current

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
            SegmentedColumn(
                title = "应用视觉效果"
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
                item(animatedVisibility = uiState.useFloatingBottomBar) {
                    SwitchWidget(
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_use_floating_bottom_bar_compact),
                        description = stringResource(R.string.theme_settings_use_floating_bottom_bar_compact_desc),
                        checked = uiState.useFloatingBottomBarCompact,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseFloatingBarCompact(
                                    it
                                )
                            )
                        }
                    )
                }
                item(
                    animatedVisibility = uiState.useFloatingBottomBar && uiState.useFloatingBottomBarCompact
                ) { shape ->
                    BaseItemContainer(shape = shape) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FloatingBottomBarCompactType.entries.chunked(2)
                                .forEach { rowEntries ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            8.dp
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
            }
        }

        item {
            SegmentedColumn(
                title = "应用材质"
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
                    item(animatedVisibility = uiState.useBlur && uiState.useLiquidGlassBottomSheet) { shape ->
                        SliderWidget(
                            title = stringResource(R.string.theme_settings_liquid_glass_blur_radius),
                            value = uiState.liquidGlassBlurRadius.toFloat(),
                            onValueChange = {
                                viewModel.dispatch(
                                    ThemeSettingsAction.SetLiquidGlassBlurRadius(
                                        it.toInt()
                                    )
                                )
                            },
                            valueRange = 0f..48f,
                            valueDisplay = "${uiState.liquidGlassBlurRadius} dp",
                            shape = shape
                        )
                    }
                }
            }
        }

        item {
            SegmentedColumn(
                title = "应用配色方案"
            ) {
                item {
                    BaseWidget(
                        // icon = AppMaterialSymbols.dark_mode,
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_theme_mode),
                        description = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_settings_theme_mode_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_settings_theme_mode_dark)
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_settings_theme_mode_system)
                        },
                        onClick = onShowThemeModeSheet
                    )
                }
                item {
                    BaseWidget(
                        // icon = AppMaterialSymbols.style,
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_palette_style),
                        description = uiState.paletteStyle.displayName,
                        onClick = onShowPaletteSheet
                    )
                }
                item {
                    val isSpec2025Supported = uiState.paletteStyle.supportsSpec2025
                    val activeSpec =
                        if (!isSpec2025Supported) ThemeColorSpec.SPEC_2021 else uiState.colorSpec
                    val descriptionText =
                        if (!isSpec2025Supported) stringResource(id = R.string.theme_settings_color_spec_only_2021) else activeSpec.displayName
                    BaseWidget(
                        // icon = AppMaterialSymbols.design_services,
                        iconPlaceholder = false,
                        title = stringResource(id = R.string.theme_settings_color_spec),
                        description = descriptionText,
                        enabled = isSpec2025Supported,
                        onClick = onShowColorSpecSheet
                    )
                }
                item {
                    SwitchWidget(
                        // icon = AppMaterialSymbols.invert_colors,
                        iconPlaceholder = false,
                        title = stringResource(R.string.theme_settings_dynamic_color),
                        description = stringResource(R.string.theme_settings_dynamic_color_desc),
                        checked = uiState.useDynamicColor,
                        onCheckedChange = {
                            viewModel.dispatch(
                                ThemeSettingsAction.SetUseDynamicColor(
                                    it
                                )
                            )
                        }
                    )
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = !uiState.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                ) +
                        expandVertically(
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing
                            )
                        ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 250,
                        easing = FastOutSlowInEasing
                    )
                ) +
                        shrinkVertically(
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        )
            ) {
                SegmentedColumn(
                    title = stringResource(R.string.theme_settings_theme_color)
                ) {
                    item {
                        BaseItemContainer {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                            ) {
                                val itemMinWidth = 88.dp
                                val columns =
                                    (this.maxWidth / itemMinWidth).toInt()
                                        .coerceAtLeast(1)
                                val chunkedColors =
                                    uiState.availableColors.chunked(columns)

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    chunkedColors.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            rowItems.forEach { rawColor ->
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    ColorSwatchPreview(
                                                        rawColor = rawColor,
                                                        currentStyle = uiState.paletteStyle,
                                                        colorSpec = uiState.colorSpec,
                                                        textStyle = MaterialTheme.typography.labelMedium.copy(
                                                            fontSize = 13.sp
                                                        ),
                                                        textColor = MaterialTheme.colorScheme.onSurface,
                                                        isSelected = uiState.seedColor == rawColor.color &&
                                                                !(uiState.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
                                                    ) {
                                                        viewModel.dispatch(
                                                            ThemeSettingsAction.SetSeedColor(
                                                                rawColor.color
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            val remaining = columns - rowItems.size
                                            if (remaining > 0) {
                                                repeat(remaining) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            StreamingModeSettings()
        }

        item {
            StickerCacheSettings()
        }

        item {
            // 运行日志入口卡片：点击"查看"按钮弹出 LogSheet；日志较少查看，置于设置页底部
            RunLogSettings(onViewLog = onViewLog)
        }

        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
fun StickerCacheSettings() {
    val context = LocalContext.current
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

    SegmentedColumn(title = "缓存") {
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
    SegmentedColumn(title = "日志") {
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "运行日志",
                description = "查看图标生成与构建过程的运行日志",
                trailingContent = {
                    PrimaryActionButton(
                        text = "查看",
                        onClick = onViewLog
                    )
                }
            )
        }
    }
}

@Composable
fun StreamingModeSettings(
    viewModel: IconViewModel = koinViewModel()
) {
    val useStreaming by viewModel.useStreaming.collectAsStateWithLifecycle()
    // App-M3 预处理状态
    val preprocessState by viewModel.appM3PreprocessState.collectAsStateWithLifecycle()
    SegmentedColumn(
        title = "性能"
    ) {
        item {
            SwitchWidget(
                iconPlaceholder = false,
                title = "流式打包模式",
                description = "通过流式打包图标来节省内存",
                checked = useStreaming,
                onCheckedChange = { viewModel.useStreaming.value = it }
            )
        }
        // App-M3 预处理：主动预热并持久化 AppColorSchemes 映射
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "预处理'基于应用-M3'颜色映射集",
                description = when (val state = preprocessState) {
                    is AppM3PreprocessManager.PreprocessState.Idle ->
                        "可加快'基于应用-M3'样式的构建速度"

                    is AppM3PreprocessManager.PreprocessState.Running -> {
                        val percent = if (state.total > 0) {
                            (state.computed * 100 / state.total)
                        } else 0
                        "可加快'基于应用-M3'样式的构建速度\n已完成 $percent%"
                    }

                    AppM3PreprocessManager.PreprocessState.Done ->
                        "可加快'基于应用-M3'样式的构建速度\n已完成 100%"
                },
                trailingContent = {
                    PrimaryActionButton(
                        text = "开始",
                        onClick = { viewModel.startAppM3Preprocess() },
                        enabled = preprocessState !is AppM3PreprocessManager.PreprocessState.Done &&
                                preprocessState !is AppM3PreprocessManager.PreprocessState.Running
                    )
                }
            )
        }
    }
}
