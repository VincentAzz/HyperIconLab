package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ChipCornerInset
import com.capybara.hypericonlab.core.designsystem.theme.insetCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.designsystem.util.inspectDragGestures
import kotlinx.coroutines.launch

private object StyleChipLegacyDefaults {
    val Height = 36.dp
    val PressedExpansion = 4.dp
    const val SpringDampingRatio = 0.5f
    const val SpringStiffness = 300f
    const val VisibilityThreshold = 0.001f
}

@Composable
internal fun StyleChipLegacy(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    selectedContainerColor: Color,
    unselectedContainerColor: Color,
    selectedContentColor: Color,
    unselectedContentColor: Color?,
) {
    val outerCornerRadius = currentSegmentedColumnOuterCornerRadius()
    val scope = rememberCoroutineScope()
    val pressProgress = remember { Animatable(0f) }
    val pressAnimationSpec = remember {
        spring<Float>(
            dampingRatio = StyleChipLegacyDefaults.SpringDampingRatio,
            stiffness = StyleChipLegacyDefaults.SpringStiffness,
            visibilityThreshold = StyleChipLegacyDefaults.VisibilityThreshold,
        )
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor else unselectedContainerColor,
        label = "legacy_chip_background_color",
    )
    val resolvedUnselectedContentColor = unselectedContentColor
        ?: if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else resolvedUnselectedContentColor,
        label = "legacy_chip_text_color",
    )

    Box(
        modifier = modifier
            .height(StyleChipLegacyDefaults.Height)
            .zIndex(pressProgress.value)
            .graphicsLayer {
                val pressedScale = 1f +
                        StyleChipLegacyDefaults.PressedExpansion.toPx() / size.height.coerceAtLeast(
                    1f
                )
                val scale = lerp(1f, pressedScale, pressProgress.value)
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                inspectDragGestures(
                    onDragStart = {
                        scope.launch { pressProgress.animateTo(1f, pressAnimationSpec) }
                    },
                    onDragEnd = {
                        scope.launch { pressProgress.animateTo(0f, pressAnimationSpec) }
                    },
                    onDragCancel = {
                        scope.launch { pressProgress.animateTo(0f, pressAnimationSpec) }
                    },
                ) { _, _ -> }
            },
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = rememberKyantRoundedRectangleShape(
                insetCornerRadius(outerCornerRadius, ChipCornerInset),
            ),
            color = backgroundColor,
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label, fontSize = 13.sp, color = textColor)
                if (selected) {
                    Icon(
                        imageVector = AppMaterialSymbols.check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = selectedContentColor,
                    )
                }
            }
        }
    }
}
