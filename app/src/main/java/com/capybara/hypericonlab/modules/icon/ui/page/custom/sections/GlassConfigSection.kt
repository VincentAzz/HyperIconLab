package com.capybara.hypericonlab.modules.icon.ui.page.custom.sections

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget

@Composable
fun GlassConfigSection(viewModel: com.capybara.hypericonlab.modules.icon.ui.page.custom.IconViewModel) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val glassAngle = config.glass.angle
    val glassStrokeDiff = config.glass.strokeDiff
    val glassShadowEnabled = config.glass.shadowEnabled

    SegmentedColumn(
        title = "玻璃样式配置",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
    ) {
        item { shape ->
            SliderWidget(
                title = "角度",
                value = glassAngle,
                onValueChange = {
                    viewModel.updateConfig { c ->
                        c.copy(
                            glass = c.glass.copy(
                                angle = it
                            )
                        )
                    }
                },
                valueRange = -45f..45f,
                steps = 1,
                valueDisplay = "${glassAngle.toInt()}°",
                shape = shape
            )
        }
        item { shape ->
            SliderWidget(
                title = "粗细差",
                value = glassStrokeDiff,
                onValueChange = {
                    viewModel.updateConfig { c ->
                        c.copy(
                            glass = c.glass.copy(
                                strokeDiff = it
                            )
                        )
                    }
                },
                valueRange = -4.0f..0.0f,
                steps = 79,
                valueDisplay = String.format(
                    LocalLocale.current.platformLocale,
                    "%.2f",
                    glassStrokeDiff
                ),
                shape = shape
            )
        }
        item {
            BaseWidget(
                icon = null,
                iconPlaceholder = false,
                title = "阴影开关",
                trailingContent = {
                    Switch(
                        checked = glassShadowEnabled,
                        onCheckedChange = {
                            viewModel.updateConfig { c ->
                                c.copy(
                                    glass = c.glass.copy(
                                        shadowEnabled = it
                                    )
                                )
                            }
                        }
                    )
                }
            )
        }
    }
}
