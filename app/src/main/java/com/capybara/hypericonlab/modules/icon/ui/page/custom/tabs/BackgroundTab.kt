package com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.MaskPickerSheet
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.MaskThumbnail
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import com.capybara.hypericonlab.modules.icon.ui.page.custom.sections.ColorSourceSection
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackgroundTab(
    viewModel: com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp
) {
    val style by viewModel.bgStyle.collectAsStateWithLifecycle()
    val selectedMasks by viewModel.selectedMasks.collectAsStateWithLifecycle()

    var showMaskPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SegmentedColumn(
            title = "样式",
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
        ) {
            item { shape ->
                BaseItemContainer(shape = shape) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleChip(
                                label = "无背景",
                                selected = style == "none",
                                onClick = { viewModel.updateConfig { it.copy(bgStyle = "none") } },
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "纯色",
                                selected = style == "solid",
                                onClick = { viewModel.updateConfig { it.copy(bgStyle = "solid") } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StyleChip(
                                label = "图片",
                                selected = false,
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                            StyleChip(
                                label = "图片填充",
                                selected = false,
                                enabled = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item(
                animatedVisibility = style == "solid",
                topPadding = ListItemDefaults.SegmentedGap,
            ) { shape ->
                BaseItemContainer(shape = shape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "形状",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedMasks.forEach { mask ->
                                MaskThumbnail(
                                    mask = mask
                                )
                            }
                        }
                        TextButton(
                            onClick = { showMaskPicker = true }
                        ) {
                            Text("更改")
                        }
                    }
                }
            }
        }

        if (style != "none") {
            ColorSourceSection(
                viewModel = viewModel,
                isForeground = false,
                backdrop = backdrop,
                useLiquidGlass = useLiquidGlass,
                liquidGlassBlurRadius = liquidGlassBlurRadius
            )
        }
    }

    if (showMaskPicker) {
        MaskPickerSheet(
            onDismiss = { showMaskPicker = false },
            selectedMasks = selectedMasks,
            onMasksConfirmed = {
                viewModel.updateConfig { c -> c.copy(selectedMasks = it) }; showMaskPicker = false
            },
            backdrop = backdrop,
            useLiquidGlass = useLiquidGlass,
            liquidGlassBlurRadius = liquidGlassBlurRadius
        )
    }
}
