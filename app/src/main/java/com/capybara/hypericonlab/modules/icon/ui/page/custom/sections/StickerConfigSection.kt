package com.capybara.hypericonlab.modules.icon.ui.page.custom.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.BaseWidgetAction
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.core.designsystem.component.StyleChip
import com.capybara.hypericonlab.core.designsystem.symbol.style
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.SwatchPreviewCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ColorPickerSheet
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel

@Composable
fun StickerConfigSection(
    viewModel: IconViewModel,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val fillStyle = config.sticker.fillStyle
    val stickerStrokeWidth = config.sticker.strokeWidth
    val glowIntensity = config.sticker.glowIntensity
    val stickerLineColor = config.sticker.lineColor
    val stickerFillColor = config.sticker.fillColor
    val colorSource = config.fgColorSource

    var showLineColorPicker by remember { mutableStateOf(false) }
    var showFillColorPicker by remember { mutableStateOf(false) }

    SegmentedColumn(
        title = "贴纸样式配置",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        if (colorSource != "black_white") {
            item {
                BaseItemContainer {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleChip(
                                label = "无填充",
                                selected = fillStyle == "none",
                                onClick = {
                                    viewModel.updateConfig { c ->
                                        c.copy(
                                            sticker = c.sticker.copy(
                                                fillStyle = "none"
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "填充",
                                selected = fillStyle == "fill",
                                onClick = {
                                    viewModel.updateConfig { c ->
                                        c.copy(
                                            sticker = c.sticker.copy(
                                                fillStyle = "fill"
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "光晕",
                                selected = fillStyle == "glow",
                                onClick = {
                                    viewModel.updateConfig { c ->
                                        c.copy(
                                            sticker = c.sticker.copy(
                                                fillStyle = "glow"
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item { shape ->
            SliderWidget(
                title = "描边粗细",
                value = stickerStrokeWidth,
                onValueChange = {
                    viewModel.updateConfig { c ->
                        c.copy(
                            sticker = c.sticker.copy(
                                strokeWidth = it
                            )
                        )
                    }
                },
                valueRange = 0.05f..0.3f,
                steps = 10,
                valueDisplay = String.format(
                    LocalLocale.current.platformLocale,
                    "%.2f",
                    stickerStrokeWidth
                ),
                shape = shape
            )
        }

        if (fillStyle == "glow" && colorSource != "black_white") {
            item { shape ->
                SliderWidget(
                    title = "光晕强度",
                    value = glowIntensity,
                    onValueChange = {
                        viewModel.updateConfig { c ->
                            c.copy(
                                sticker = c.sticker.copy(
                                    glowIntensity = it
                                )
                            )
                        }
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 10,
                    valueDisplay = String.format(
                        LocalLocale.current.platformLocale,
                        "%.2f",
                        glowIntensity
                    ),
                    shape = shape
                )
            }
        }

        if (colorSource == "custom") {
            item {
                BaseWidget(
                    icon = AppMaterialSymbols.style,
                    title = "线条颜色",
                    onClick = { showLineColorPicker = true },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(
                                        rememberKyantRoundedRectangleShape(
                                            SwatchPreviewCornerRadius
                                        )
                                    )
                                    .background(Color(stickerLineColor.toColorInt()))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stickerLineColor.uppercase(), fontSize = 12.sp)
                            BaseWidgetAction()
                        }
                    }
                )
            }
            item {
                BaseWidget(
                    icon = AppMaterialSymbols.style,
                    title = "填充颜色",
                    onClick = { showFillColorPicker = true },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(
                                        rememberKyantRoundedRectangleShape(
                                            SwatchPreviewCornerRadius
                                        )
                                    )
                                    .background(Color(stickerFillColor.toColorInt()))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stickerFillColor.uppercase(), fontSize = 12.sp)
                            BaseWidgetAction()
                        }
                    }
                )
            }
        }
    }

    if (showLineColorPicker) {
        ColorPickerSheet(
            initialColor = stickerLineColor,
            onDismiss = { showLineColorPicker = false },
            onColorSelected = {
                viewModel.updateConfig { c ->
                    c.copy(
                        sticker = c.sticker.copy(
                            lineColor = it
                        )
                    )
                }; showLineColorPicker = false
            }
        )
    }
    if (showFillColorPicker) {
        ColorPickerSheet(
            initialColor = stickerFillColor,
            onDismiss = { showFillColorPicker = false },
            onColorSelected = {
                viewModel.updateConfig { c ->
                    c.copy(
                        sticker = c.sticker.copy(
                            fillColor = it
                        )
                    )
                }; showFillColorPicker = false
            }
        )
    }
}
