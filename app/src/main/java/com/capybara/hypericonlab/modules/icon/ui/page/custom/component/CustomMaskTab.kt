package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.core.designsystem.component.SwitchWidget
import com.capybara.hypericonlab.core.designsystem.shape.RoundedCornerStyle
import com.capybara.hypericonlab.core.designsystem.theme.SheetSegmentedColumnContentPadding
import com.capybara.hypericonlab.core.designsystem.theme.currentSheetRoundedLayout
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.render.image.CustomMaskGenerator

// 自定义形状默认参数（默认 HyperOS 3）
private const val DEFAULT_CORNER_RADIUS = 0.281f
private const val DEFAULT_SMOOTH_CORNER = true

// 自定义形状 tab
@Composable
fun CustomMaskTab(
    cornerRadius: Float,
    smoothCorner: Boolean,
    onCornerRadiusChange: (Float) -> Unit,
    onSmoothCornerChange: (Boolean) -> Unit
) {
    val roundedLayout = currentSheetRoundedLayout()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(roundedLayout.cardInset),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "自定义形状仅支持单个",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val previewSize = 120.dp
            val cornerRadiusDp = previewSize * cornerRadius
            val shape = rememberKyantRoundedRectangleShape(
                cornerRadius = cornerRadiusDp,
                style = if (smoothCorner) RoundedCornerStyle.Continuous
                else RoundedCornerStyle.Circular
            )
            Surface(
                modifier = Modifier.size(previewSize),
                shape = shape,
                color = MaterialTheme.colorScheme.primary
            ) {}
        }

        SegmentedColumn(
            contentPadding = PaddingValues(SheetSegmentedColumnContentPadding)
        ) {
            item { shape ->
                SliderWidget(
                    title = "圆角比例 (半径 / 高度)",
                    value = cornerRadius,
                    onValueChange = onCornerRadiusChange,
                    valueRange = 0f..0.5f,
                    valueDisplay = String.format("%.3f", cornerRadius),
                    shape = shape
                )
            }
            item { shape ->
                SwitchWidget(
                    title = "平滑圆角",
                    description = "使用连续曲率圆角",
                    checked = smoothCorner,
                    onCheckedChange = onSmoothCornerChange,
                    iconPlaceholder = false
                )
            }
        }
    }
}

fun parseCustomMaskParams(selectedMasks: List<String>): Pair<Float, Boolean> {
    val customMask = selectedMasks.firstOrNull { CustomMaskGenerator.isCustomMask(it) }
    return customMask?.let { CustomMaskGenerator.parseCustomMask(it) }?.let {
        it.first to it.second
    } ?: (DEFAULT_CORNER_RADIUS to DEFAULT_SMOOTH_CORNER)
}
