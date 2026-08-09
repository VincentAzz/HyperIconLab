package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ChipCornerInset
import com.capybara.hypericonlab.core.designsystem.theme.insetCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape

@Composable
fun StyleChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color? = null
) {
    // 基础参数定义
    val outerCornerRadius = currentSegmentedColumnOuterCornerRadius()
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) selectedContainerColor
        else unselectedContainerColor,
        label = "chip_background_color"
    )

    val resolvedUnselectedContentColor = unselectedContentColor
        ?: if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant

    val textColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor
        else resolvedUnselectedContentColor,
        label = "chip_text_color"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = rememberKyantRoundedRectangleShape(
            insetCornerRadius(outerCornerRadius, ChipCornerInset)
        ),
        color = backgroundColor,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = textColor
            )
            if (selected) {
                Icon(
                    imageVector = AppMaterialSymbols.check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = selectedContentColor
                )
            }
        }
    }
}
