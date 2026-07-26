package com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ConfigCard
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.ColorSourceSection
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.GlassConfigSection
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.StickerConfigSection
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
fun ForegroundTab(
    viewModel: IconViewModel,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val style by viewModel.fgStyle.collectAsStateWithLifecycle()
    val strokeWidth by viewModel.strokeWidthRatio.collectAsStateWithLifecycle()
    val iconScale by viewModel.iconScale.collectAsStateWithLifecycle()

    Column {
        ConfigCard(
            title = "样式"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleChip(
                        label = "线条",
                        selected = style == "line",
                        onClick = { viewModel.updateConfig { it.copy(fgStyle = "line") } },
                        modifier = Modifier.weight(1f)
                    )
                    StyleChip(
                        label = "镂空线条",
                        selected = style == "hollow",
                        onClick = { viewModel.updateConfig { it.copy(fgStyle = "hollow") } },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleChip(
                        label = "贴纸",
                        selected = style == "sticker",
                        onClick = { viewModel.updateConfig { it.copy(fgStyle = "sticker") } },
                        modifier = Modifier.weight(1f)
                    )
                    StyleChip(
                        label = "玻璃",
                        selected = style == "glass",
                        onClick = { viewModel.updateConfig { it.copy(fgStyle = "glass") } },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StyleChip(
                        label = "填充",
                        selected = false,
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 玻璃样式配置 卡片组
        if (style == "glass") {
            GlassConfigSection(
                viewModel
            )
        }

        // 贴纸样式配置 卡片组
        if (style == "sticker") {
            StickerConfigSection(
                viewModel
            )
        }

        // 颜色 卡片组
        if (style != "hollow") {
            ColorSourceSection(
                viewModel = viewModel,
                isForeground = true,
                backdrop = backdrop,
                useLiquidGlass = useLiquidGlass,
                liquidGlassBlurRadius = liquidGlassBlurRadius
            )
        }

        SegmentedColumn(
            title = "粗细与缩放",
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item { shape ->
                SliderWidget(
                    title = "粗细",
                    value = strokeWidth,
                    onValueChange = { viewModel.updateConfig { c -> c.copy(strokeWidthRatio = it) } },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    valueDisplay = "${
                        String.format(
                            LocalLocale.current.platformLocale,
                            "%.2f",
                            strokeWidth
                        )
                    } x",
                    shape = shape
                )
            }
            item { shape ->
                SliderWidget(
                    title = "缩放",
                    value = iconScale,
                    onValueChange = { viewModel.updateConfig { c -> c.copy(iconScale = it) } },
                    valueRange = 0.5f..1.0f,
                    steps = 10,
                    valueDisplay = "${
                        String.format(
                            LocalLocale.current.platformLocale,
                            "%.2f",
                            iconScale
                        )
                    } x",
                    shape = shape
                )
            }
        }
    }
}
