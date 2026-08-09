package com.capybara.hypericonlab.modules.icon.ui.page.custom.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseItemContainer
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.StyleChip
import com.capybara.hypericonlab.modules.icon.viewmodel.IconViewModel

// assets/shadow_baked/<shapeName>_<styleName>_shadow_512.png

private fun friendlyStyleName(styleName: String): String = when (styleName) {
    "3d" -> "OneUI 8.5 3D"
    "neumorphism" -> "拟物"
    else -> styleName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}


private enum class IntensityOption(val label: String, val layers: Int) {
    WEAK("弱", 1),
    MEDIUM("中", 2),
    STRONG("强", 3)
}

private object BorderTabUiConstants {
    val CARD_INNER_PADDING = 12.dp
    const val STYLE_CHIPS_PER_ROW = 2
}


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

    Column {
        when {
            dualLayerEnabled -> InnerShadowUnavailableHint(
                title = "启用双层背景时无法设定内阴影",
                description = "请先禁用双层背景",
                actionLabel = "去关闭",
                onAction = onGoToBackgroundTab
            )

            availableStyles.isEmpty() -> InnerShadowUnavailableHint(
                title = "当前形状未提供内阴影",
                description = "请选用常用形状",
                actionLabel = "去修改",
                onAction = onGoToBackgroundTab
            )

            else -> InnerShadowCardGroup(
                enabled = innerShadow.enabled,
                styleName = innerShadow.styleName,
                intensityLayers = innerShadow.intensityLayers,
                availableStyles = availableStyles,
                onToggleEnabled = { enabled ->
                    viewModel.updateInnerShadow { shadow ->
                        if (enabled && shadow.styleName == null && availableStyles.isNotEmpty()) {
                            shadow.copy(enabled = true, styleName = availableStyles.first())
                        } else {
                            shadow.copy(enabled = enabled)
                        }
                    }
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
    SegmentedColumn(
        title = "内阴影",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        item { shape ->
            BaseItemContainer(shape = shape) {
                BaseWidget(
                    icon = null,
                    iconPlaceholder = false,
                    title = title,
                    description = description,
                    trailingContent = {
                        PrimaryActionButton(
                            text = actionLabel,
                            onClick = onAction
                        )
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    availableStyles.chunked(BorderTabUiConstants.STYLE_CHIPS_PER_ROW)
                        .forEach { rowStyles ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowStyles.forEach { style ->
                                    StyleChip(
                                        label = friendlyStyleName(style),
                                        selected = styleName == style,
                                        onClick = { onStyleSelected(style) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowStyles.size < BorderTabUiConstants.STYLE_CHIPS_PER_ROW) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
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
                        IntensityOption.entries.forEach { option ->
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


