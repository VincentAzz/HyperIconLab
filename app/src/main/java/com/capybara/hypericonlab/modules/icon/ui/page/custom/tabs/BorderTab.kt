package com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip


@Composable
fun BorderTab(
    viewModel: IconViewModel,
    onGoToBackgroundTab: () -> Unit
) {
    val innerShadow by viewModel.innerShadow.collectAsStateWithLifecycle()
    val shadowAssetsMap by viewModel.shadowAssetsMap.collectAsStateWithLifecycle()
    val selectedMasks by viewModel.selectedMasks.collectAsStateWithLifecycle()
    val dualLayerEnabled by viewModel.dualLayerEnabled.collectAsStateWithLifecycle()

    val upperShape = selectedMasks.firstOrNull()
    val availableStyles = shadowAssetsMap[upperShape] ?: emptyList()

    Column(modifier = Modifier.padding(horizontal = BorderTabUiConstants.HORIZONTAL_PADDING)) {
        when {
            dualLayerEnabled -> InnerShadowUnavailableHint(
                title = "启用双层背景时无法设定内阴影",
                description = "如需使用内阴影，请先关闭双层背景",
                actionLabel = "去关闭",
                onAction = onGoToBackgroundTab
            )

            availableStyles.isEmpty() -> InnerShadowUnavailableHint(
                title = "当前形状未提供内阴影",
                description = "请选择提供了内阴影样式的常用形状",
                actionLabel = "去修改",
                onAction = onGoToBackgroundTab
            )
            // 状态 C：符合启用条件，显示正常卡片组
            else -> InnerShadowCardGroup(
                enabled = innerShadow.enabled,
                styleName = innerShadow.styleName,
                intensityLayers = innerShadow.intensityLayers,
                availableStyles = availableStyles,
                onToggleEnabled = { enabled ->
                    viewModel.updateInnerShadow { it.copy(enabled = enabled) }
                },
                onStyleSelected = { style ->
                    viewModel.updateInnerShadow { it.copy(styleName = style) }
                },
                onIntensitySelected = { layers ->
                    viewModel.updateInnerShadow { it.copy(intensityLayers = layers) }
                }
            )
        }
    }
}


@Composable
private fun InnerShadowUnavailableHint(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    SegmentedColumn(title = "内阴影") {
        item { shape ->
            BaseItemContainer(shape = shape) {
                BaseWidget(
                    icon = null,
                    iconPlaceholder = false,
                    title = title,
                    description = description,
                    trailingContent = {
                        TextButton(onClick = onAction) { Text(actionLabel) }
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
private fun InnerShadowCardGroup(
    enabled: Boolean,
    styleName: String?,
    intensityLayers: Int,
    availableStyles: List<String>,
    onToggleEnabled: (Boolean) -> Unit,
    onStyleSelected: (String) -> Unit,
    onIntensitySelected: (Int) -> Unit
) {
    SegmentedColumn(
        title = "内阴影",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        // 内阴影开关
        item { shape ->
            BaseItemContainer(shape = shape) {
                BaseWidget(
                    icon = null,
                    iconPlaceholder = false,
                    title = "内阴影",
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = onToggleEnabled
                        )
                    }
                )
            }
        }

        // 样式选择
        item(
            animatedVisibility = enabled,
            topPadding = ListItemDefaults.SegmentedGap
        ) { shape ->
            BaseItemContainer(shape = shape) {
                Column(
                    modifier = Modifier.padding(BorderTabUiConstants.CARD_INNER_PADDING),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("样式", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableStyles.forEach { style ->
                            StyleChip(
                                label = friendlyStyleName(style),
                                selected = styleName == style,
                                onClick = { onStyleSelected(style) }
                            )
                        }
                    }
                }
            }
        }

        // 强度
        item(
            animatedVisibility = enabled,
            topPadding = ListItemDefaults.SegmentedGap
        ) { shape ->
            BaseItemContainer(shape = shape) {
                Column(
                    modifier = Modifier.padding(BorderTabUiConstants.CARD_INNER_PADDING),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("强度", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IntensityOption.values().forEach { option ->
                            StyleChip(
                                label = option.label,
                                selected = intensityLayers == option.layers,
                                onClick = { onIntensitySelected(option.layers) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun friendlyStyleName(styleName: String): String = when (styleName) {
    "3d" -> "3D"
    "neumorphism" -> "拟物"
    else -> styleName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}


private enum class IntensityOption(val label: String, val layers: Int) {
    WEAK("弱", 1),
    MEDIUM("中", 2),
    STRONG("强", 3)
}

private object BorderTabUiConstants {
    val HORIZONTAL_PADDING = 16.dp
    val CARD_INNER_PADDING = 12.dp
}
