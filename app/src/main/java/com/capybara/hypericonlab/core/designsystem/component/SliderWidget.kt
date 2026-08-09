package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.backdrops.LocalKyantControlsBackdrop
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import kotlin.math.roundToInt

@Composable
fun SliderWidget(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueDisplay: String = String.format("%.2f", value),
    shape: Shape = MaterialTheme.shapes.medium,
    enabled: Boolean = true,
    useMiuixSlider: Boolean = false,
    trackThickness: ExpressiveSliderTrackThickness = ExpressiveSliderTrackThickness.THIN
) {
    BaseItemContainer(
        shape = shape,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f
                    )
                )
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = GoogleSansCodeFontFamily,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.38f
                    )
                )
            }
            // Spacer(Modifier.height(4.dp))
            val kyantBackdrop = LocalKyantControlsBackdrop.current
            if (enabled && LocalAppleStyleControls.current.useSlider && kyantBackdrop != null) {
                AppleLiquidSliderKyant(
                    value = { value },
                    onValueChange = { rawValue ->
                        val resolvedValue = if (steps > 0) {
                            val fraction = (rawValue - valueRange.start) /
                                    (valueRange.endInclusive - valueRange.start)
                            val stepCount = steps + 1
                            val steppedFraction =
                                (fraction * stepCount).roundToInt() / stepCount.toFloat()
                            valueRange.start + steppedFraction *
                                    (valueRange.endInclusive - valueRange.start)
                        } else {
                            rawValue
                        }
                        onValueChange(resolvedValue.coerceIn(valueRange))
                    },
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    visibilityThreshold = 0.001f,
                    backdrop = kyantBackdrop,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ExpressiveSlider(
                    value = value,
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    valueRange = valueRange,
                    steps = steps,
                    enabled = enabled,
                    trackThickness = trackThickness,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
