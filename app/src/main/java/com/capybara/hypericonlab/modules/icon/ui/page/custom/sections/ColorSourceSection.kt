package com.capybara.hypericonlab.modules.icon.ui.page.custom.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SelectionSheet
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.core.designsystem.component.SwitchWidget
import com.capybara.hypericonlab.core.designsystem.symbol.design_services
import com.capybara.hypericonlab.core.designsystem.symbol.style
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.CTCConfigSection
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ColorPickerSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ColorSwatchPreviewIcon
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ColorSourceSection(
    viewModel: com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel,
    isForeground: Boolean,
    layerIndex: Int = 0,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    // 下层背景读写 bgLayer2 字段；上层/前景保持原逻辑
    val bgLayer2 = config.bgLayer2
    val style = if (isForeground) config.fgStyle else config.bgStyle
    val colorSource = when {
        isForeground -> config.fgColorSource
        layerIndex == 1 -> bgLayer2.colorSource
        else -> config.bgColorSource
    }
    val configColor = when {
        isForeground -> config.fgColor
        layerIndex == 1 -> bgLayer2.color
        else -> config.bgColor
    }
    // 下层使用独立的 previewThemeMode；上层/前景保持原字段
    val previewThemeMode =
        if (layerIndex == 1) bgLayer2.previewThemeMode else config.previewThemeMode
    // preset/wallpaper/ctc 是颜色生成器配置，上下层共享（仅 monet 变体由 previewThemeMode 区分）
    val presetSeedColor = config.preset.seedColor
    val presetPaletteStyle = config.preset.paletteStyle
    val presetColorSpec = config.preset.colorSpec
    val wallpaperPaletteStyle = config.wallpaper.paletteStyle
    val wallpaperColorSpec = config.wallpaper.colorSpec
    val syncColorSource = config.syncColorSource
    val syncDualLayerColorSource = config.syncDualLayerColorSource
    val dualLayerEnabled = config.dualLayerEnabled
    // 下层背景透明度（0~255），仅下层使用
    val lowerAlpha = if (layerIndex == 1) bgLayer2.alpha else 255

    // 标题前缀：双层启用时上层/下层加前缀，未启用时无前缀
    val layerPrefix = when {
        layerIndex == 1 -> "下层"
        dualLayerEnabled -> "上层"
        else -> ""
    }
    val colorSectionTitle = if (layerPrefix.isEmpty()) "颜色" else "${layerPrefix}背景颜色"
    val wallpaperConfigTitle =
        if (layerPrefix.isEmpty()) "基于壁纸色彩配置" else "${layerPrefix}基于壁纸色彩配置"
    val presetConfigTitle =
        if (layerPrefix.isEmpty()) "Material 3 预设配置" else "${layerPrefix}Material 3 预设配置"
    val ctcConfigTitle =
        if (layerPrefix.isEmpty()) "中国传统色预设配置" else "${layerPrefix}中国传统色预设配置"

    // 写入颜色来源：
    // - 下层（layerIndex==1）：写入 bgLayer2.colorSource
    // - 上层背景（layerIndex==0 && !isForeground）：上层字段；若 syncColorSource 启用则同时同步前景；
    //   若 syncDualLayerColorSource 启用则同时同步下层（仅 colorSource，下层 monet 变体保持独立）
    // - 前景（isForeground）：上层字段；若 syncColorSource 启用则同时同步背景
    fun applyColorSource(source: String) {
        viewModel.updateConfig {
            if (layerIndex == 1) {
                it.copy(bgLayer2 = it.bgLayer2.copy(colorSource = source))
            } else {
                val withFgSync = if (syncColorSource) {
                    it.copy(fgColorSource = source, bgColorSource = source)
                } else {
                    if (isForeground) it.copy(fgColorSource = source)
                    else it.copy(bgColorSource = source)
                }
                // 双层启用且开启同步上下层时，上层背景变更同步到下层 colorSource（不动 monet 变体）
                if (dualLayerEnabled && syncDualLayerColorSource && !isForeground) {
                    withFgSync.copy(bgLayer2 = withFgSync.bgLayer2.copy(colorSource = source))
                } else {
                    withFgSync
                }
            }
        }
    }

    // 写入下层 monet 变体（仅下层用）
    fun updateLowerThemeMode(mode: String) {
        viewModel.updateConfig {
            it.copy(bgLayer2 = it.bgLayer2.copy(previewThemeMode = mode))
        }
    }

    // 写入下层透明度
    fun updateLowerAlpha(alpha: Int) {
        viewModel.updateConfig {
            it.copy(bgLayer2 = it.bgLayer2.copy(alpha = alpha))
        }
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var showPaletteStyleSheet by remember { mutableStateOf(false) }
    var showColorSpecSheet by remember { mutableStateOf(false) }
    var showWallpaperPaletteStyleSheet by remember { mutableStateOf(false) }
    var showWallpaperColorSpecSheet by remember { mutableStateOf(false) }

    if (showPaletteStyleSheet) {
        SelectionSheet(
            title = "调色板样式",
            items = PaletteStyle.entries,
            selectedItem = presetPaletteStyle,
            onDismiss = { showPaletteStyleSheet = false },
            onConfirm = { style: PaletteStyle ->
                viewModel.updateConfig { c -> c.copy(preset = c.preset.copy(paletteStyle = style)) }
            },
            itemLabel = { style: PaletteStyle -> style.displayName },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showColorSpecSheet) {
        SelectionSheet(
            title = "色彩规格",
            items = ThemeColorSpec.entries,
            selectedItem = presetColorSpec,
            onDismiss = { showColorSpecSheet = false },
            onConfirm = { spec: ThemeColorSpec ->
                viewModel.updateConfig { c -> c.copy(preset = c.preset.copy(colorSpec = spec)) }
            },
            itemLabel = { spec: ThemeColorSpec -> spec.displayName },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showWallpaperPaletteStyleSheet) {
        SelectionSheet(
            title = "调色板样式",
            items = PaletteStyle.entries,
            selectedItem = wallpaperPaletteStyle,
            onDismiss = { showWallpaperPaletteStyleSheet = false },
            onConfirm = { style: PaletteStyle ->
                viewModel.updateConfig { c -> c.copy(wallpaper = c.wallpaper.copy(paletteStyle = style)) }
            },
            itemLabel = { style: PaletteStyle -> style.displayName },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    if (showWallpaperColorSpecSheet) {
        SelectionSheet(
            title = "色彩规格",
            items = ThemeColorSpec.entries,
            selectedItem = wallpaperColorSpec,
            onDismiss = { showWallpaperColorSpecSheet = false },
            onConfirm = { spec: ThemeColorSpec ->
                viewModel.updateConfig { c -> c.copy(wallpaper = c.wallpaper.copy(colorSpec = spec)) }
            },
            itemLabel = { spec: ThemeColorSpec -> spec.displayName },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }

    SegmentedColumn(
        title = colorSectionTitle,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        item { shape ->
            BaseItemContainer(shape = shape) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleChip(
                            label = "基于壁纸",
                            selected = colorSource == "wallpaper",
                            onClick = { applyColorSource("wallpaper") },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "基于应用",
                            selected = colorSource == "app",
                            onClick = { applyColorSource("app") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleChip(
                            label = "Material 3 预设",
                            selected = colorSource == "preset",
                            onClick = { applyColorSource("preset") },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "中国传统色预设",
                            selected = colorSource == "ctc",
                            onClick = { applyColorSource("ctc") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleChip(
                            label = "自定义",
                            selected = colorSource == "custom",
                            onClick = {
                                viewModel.switchToCustomColor(isForeground, layerIndex)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // 黑白 chip 仅前景 sticker 显示；下层背景不显示
                        if (isForeground && style == "sticker") {
                            StyleChip(
                                label = "黑白",
                                selected = colorSource == "black_white",
                                onClick = { applyColorSource("black_white") },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item(
            animatedVisibility = colorSource == "custom" && (!isForeground || style != "sticker"),
            topPadding = ListItemDefaults.SegmentedGap,
        ) {
            BaseWidget(
                icon = null,
                iconPlaceholder = false,
                title = "自定义颜色",
                titleStyle = MaterialTheme.typography.titleSmall,
                onClick = { showColorPicker = true },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = configColor.uppercase(),
                            fontFamily = GoogleSansCodeFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(configColor.toColorInt()))
                        )
                    }
                }
            )
        }

        // 下层透明度滑块：layerIndex==1 且非自定义时显示（custom 已通过 ColorPickerSheet alpha 通道控制）
        item(
            animatedVisibility = layerIndex == 1 && colorSource != "custom",
            topPadding = ListItemDefaults.SegmentedGap,
        ) { shape ->
            SliderWidget(
                title = "透明度",
                value = lowerAlpha.toFloat(),
                onValueChange = { v ->
                    updateLowerAlpha(
                        v.toInt()
                            .coerceIn(LowerLayerAlphaConstants.MIN, LowerLayerAlphaConstants.MAX)
                    )
                },
                valueRange = LowerLayerAlphaConstants.MIN.toFloat()..LowerLayerAlphaConstants.MAX.toFloat(),
                steps = LowerLayerAlphaConstants.STEPS,
                valueDisplay = "${(lowerAlpha / LowerLayerAlphaConstants.MAX.toFloat() * 100).toInt()}%",
                shape = shape
            )
        }

        // 同步前景与背景开关：仅上层显示（下层独立配置，不参与同步联动）。精简样式：无 icon、无 description、无 iconPlaceholder
        item(
            animatedVisibility = layerIndex == 0 && colorSource != "custom" && colorSource != "black_white",
            topPadding = ListItemDefaults.SegmentedGap,
        ) {
            SwitchWidget(
                icon = null,
                iconPlaceholder = false,
                title = "同步前景与背景",
                checked = syncColorSource,
                onCheckedChange = { enabled ->
                    viewModel.updateConfig { it.copy(syncColorSource = enabled) }
                }
            )
        }

        // 同步上层与下层背景颜色开关：双层启用且上层背景（非前景）时显示。
        // 启用后上层背景 colorSource 变更同步到下层（下层 monet 变体保持独立）。
        item(
            animatedVisibility = layerIndex == 0 && !isForeground && dualLayerEnabled && colorSource != "custom" && colorSource != "black_white",
            topPadding = ListItemDefaults.SegmentedGap,
        ) {
            SwitchWidget(
                icon = null,
                iconPlaceholder = false,
                title = "同步上层与下层",
                checked = syncDualLayerColorSource,
                onCheckedChange = { enabled ->
                    viewModel.updateConfig { it.copy(syncDualLayerColorSource = enabled) }
                }
            )
        }
    }

    if (colorSource == "wallpaper") {
        SegmentedColumn(
            title = wallpaperConfigTitle,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item {
                BaseItemContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StyleChip(
                            label = "浅色",
                            selected = previewThemeMode == "light",
                            onClick = {
                                if (layerIndex == 1) updateLowerThemeMode("light")
                                else viewModel.updateConfig { it.copy(previewThemeMode = "light") }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "中性",
                            selected = previewThemeMode == "neutral",
                            onClick = {
                                if (layerIndex == 1) updateLowerThemeMode("neutral")
                                else viewModel.updateConfig { it.copy(previewThemeMode = "neutral") }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "暗色",
                            selected = previewThemeMode == "dark",
                            onClick = {
                                if (layerIndex == 1) updateLowerThemeMode("dark")
                                else viewModel.updateConfig { it.copy(previewThemeMode = "dark") }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                BaseWidget(
                    icon = AppMaterialSymbols.style,
                    title = "调色板样式",
                    description = wallpaperPaletteStyle.displayName,
                    onClick = { showWallpaperPaletteStyleSheet = true }
                )
            }
            item {
                BaseWidget(
                    icon = AppMaterialSymbols.design_services,
                    title = "色彩规格",
                    description = wallpaperColorSpec.displayName,
                    onClick = { showWallpaperColorSpecSheet = true }
                )
            }
        }
    }

    if (colorSource == "ctc") {
        SegmentedColumn(
            title = ctcConfigTitle,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item {
                CTCConfigSection(
                    viewModel
                )
            }
        }
    }

    if (colorSource == "preset") {
        SegmentedColumn(
            title = presetConfigTitle,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item {
                BaseItemContainer {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StyleChip(
                            label = "浅色",
                            selected = previewThemeMode == "light",
                            onClick = {
                                if (layerIndex == 1) updateLowerThemeMode("light")
                                else viewModel.updateConfig { it.copy(previewThemeMode = "light") }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "暗色",
                            selected = previewThemeMode == "dark",
                            onClick = {
                                if (layerIndex == 1) updateLowerThemeMode("dark")
                                else viewModel.updateConfig { it.copy(previewThemeMode = "dark") }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                BaseWidget(
                    icon = AppMaterialSymbols.style,
                    title = "调色板样式",
                    description = presetPaletteStyle.displayName,
                    onClick = { showPaletteStyleSheet = true }
                )
            }
            item {
                BaseWidget(
                    icon = AppMaterialSymbols.design_services,
                    title = "色彩规格",
                    description = presetColorSpec.displayName,
                    onClick = { showColorSpecSheet = true }
                )
            }
            item {
                BaseItemContainer {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp)
                    ) {
                        val itemMinWidth = 88.dp
                        val columns = (this.maxWidth / itemMinWidth).toInt().coerceAtLeast(1)
                        val chunkedColors = PresetColors.chunked(columns)
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
                                            ColorSwatchPreviewIcon(
                                                rawColor = rawColor,
                                                currentStyle = presetPaletteStyle,
                                                colorSpec = presetColorSpec,
                                                textStyle = MaterialTheme.typography.labelMedium.copy(
                                                    fontSize = 13.sp
                                                ),
                                                textColor = MaterialTheme.colorScheme.onSurface,
                                                isSelected = presetSeedColor == rawColor.color.toArgb(),
                                                onClick = {
                                                    viewModel.updateConfig { c ->
                                                        c.copy(
                                                            preset = c.preset.copy(
                                                                seedColor = rawColor.color.toArgb()
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    val remaining = columns - rowItems.size
                                    if (remaining > 0) {
                                        repeat(remaining) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerSheet(
            initialColor = configColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                viewModel.updateConfig { config ->
                    if (isForeground) {
                        val newConfig = config.copy(fgColor = color)
                        if (color == "#00000000") newConfig.copy(fgStyle = "hollow") else newConfig
                    } else if (layerIndex == 1) {
                        // 下层背景自定义颜色写入 bgLayer2.color
                        config.copy(bgLayer2 = config.bgLayer2.copy(color = color))
                    } else {
                        config.copy(bgColor = color)
                    }
                }
                showColorPicker = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }
}

/**
 * 下层背景透明度常量（避免硬编码）。
 * 范围 0~255，步进 5%（即 13），共 20 档（含端点 0/255 由 coerce 保证）。
 */
private object LowerLayerAlphaConstants {
    const val MIN = 0
    const val MAX = 255

    // 0~255 共 256 个值，按 5% 步进（即每 12.75 ≈ 13），实际取 13 档步进
    const val STEPS = 13
}
