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
import com.capybara.hypericonlab.core.designsystem.symbol.design_services
import com.capybara.hypericonlab.core.designsystem.symbol.style
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
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
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val style = if (isForeground) config.fgStyle else config.bgStyle
    val colorSource = if (isForeground) config.fgColorSource else config.bgColorSource
    val configColor = if (isForeground) config.fgColor else config.bgColor

    val previewThemeMode = config.previewThemeMode
    val presetSeedColor = config.preset.seedColor
    val presetPaletteStyle = config.preset.paletteStyle
    val presetColorSpec = config.preset.colorSpec

    var showColorPicker by remember { mutableStateOf(false) }
    var showPaletteStyleSheet by remember { mutableStateOf(false) }
    var showColorSpecSheet by remember { mutableStateOf(false) }

    if (showPaletteStyleSheet) {
        SelectionSheet(
            title = "调色板样式",
            items = com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle.entries,
            selectedItem = presetPaletteStyle,
            onDismiss = { showPaletteStyleSheet = false },
            onConfirm = { style: com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle ->
                viewModel.updateConfig { c -> c.copy(preset = c.preset.copy(paletteStyle = style)) }
            },
            itemLabel = { style: com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle -> style.displayName },
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

    SegmentedColumn(
        title = "颜色",
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
                            onClick = {
                                viewModel.updateConfig {
                                    if (isForeground) it.copy(
                                        fgColorSource = "wallpaper"
                                    ) else it.copy(bgColorSource = "wallpaper")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "基于应用",
                            selected = colorSource == "app",
                            onClick = {
                                viewModel.updateConfig {
                                    if (isForeground) it.copy(
                                        fgColorSource = "app"
                                    ) else it.copy(bgColorSource = "app")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleChip(
                            label = "Material 3 预设",
                            selected = colorSource == "preset",
                            onClick = {
                                viewModel.updateConfig {
                                    if (isForeground) it.copy(
                                        fgColorSource = "preset"
                                    ) else it.copy(bgColorSource = "preset")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "中国传统色预设",
                            selected = colorSource == "ctc",
                            onClick = {
                                viewModel.updateConfig {
                                    if (isForeground) it.copy(
                                        fgColorSource = "ctc"
                                    ) else it.copy(bgColorSource = "ctc")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyleChip(
                            label = "自定义",
                            selected = colorSource == "custom",
                            onClick = {
                                viewModel.switchToCustomColor(isForeground)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (isForeground && style == "sticker") {
                            StyleChip(
                                label = "黑白",
                                selected = colorSource == "black_white",
                                onClick = { viewModel.updateConfig { it.copy(fgColorSource = "black_white") } },
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
            animatedVisibility = colorSource == "wallpaper",
            topPadding = ListItemDefaults.SegmentedGap,
        ) {
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
                        onClick = { viewModel.updateConfig { it.copy(previewThemeMode = "light") } },
                        modifier = Modifier.weight(1f)
                    )
                    StyleChip(
                        label = "暗色",
                        selected = previewThemeMode == "dark",
                        onClick = { viewModel.updateConfig { it.copy(previewThemeMode = "dark") } },
                        modifier = Modifier.weight(1f)
                    )
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
                onClick = { showColorPicker = true },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(configColor.toColorInt()))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = configColor.uppercase(),
                            fontFamily = GoogleSansCodeFontFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    }

    if (colorSource == "ctc") {
        SegmentedColumn(
            title = "中国传统色预设配置",
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
            title = "Material 3 预设配置",
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
                            onClick = { viewModel.updateConfig { it.copy(previewThemeMode = "light") } },
                            modifier = Modifier.weight(1f)
                        )
                        StyleChip(
                            label = "暗色",
                            selected = previewThemeMode == "dark",
                            onClick = { viewModel.updateConfig { it.copy(previewThemeMode = "dark") } },
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
