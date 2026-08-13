package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

@Composable
fun BaseItemContainer(
    modifier: Modifier = Modifier,
    shape: Shape = LocalSegmentedItemShape.current,
    content: @Composable () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceBright.copy(
        alpha = LocalSegmentedContainerColorAlpha.current
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
    ) {
        content()
    }
}
