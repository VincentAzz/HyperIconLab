package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.IconEntry

private object PreviewRootConfig {
    const val COLUMNS = 4
    val GRID_HORIZONTAL_PADDING = 16.dp
    val GRID_VERTICAL_PADDING = 16.dp
    val GRID_SPACING = 12.dp
    val ICON_DISPLAY_SIZE = 64.dp
    val SCROLLBAR_WIDTH = 4.dp
    val SCROLLBAR_END_PADDING = 4.dp
    val SCROLLBAR_VERTICAL_PADDING = 8.dp
    val SCROLLBAR_MIN_THUMB_HEIGHT = 40.dp
    val SCROLLBAR_CORNER_RADIUS = 2.dp
    const val SCROLLBAR_MIN_VISIBLE_FRACTION = 0.08f
    const val SCROLLBAR_ALPHA = 0.55f
}

@Composable
fun IconPreviewGrid(
    entries: List<IconEntry>,
    state: LazyGridState,
    onEntryClick: (IconEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyVerticalGrid(
            columns = GridCells.Fixed(PreviewRootConfig.COLUMNS),
            state = state,
            contentPadding = PaddingValues(
                start = PreviewRootConfig.GRID_HORIZONTAL_PADDING,
                top = PreviewRootConfig.GRID_VERTICAL_PADDING,
                end = PreviewRootConfig.GRID_HORIZONTAL_PADDING,
                bottom = SearchUiConfig.GRID_BOTTOM_PADDING_WITH_FAB + navBarPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(PreviewRootConfig.GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(PreviewRootConfig.GRID_SPACING),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = entries.size,
                key = { index -> entries[index].stableKey }
            ) { index ->
                IconPreviewItem(
                    entry = entries[index],
                    onClick = { onEntryClick(entries[index]) }
                )
            }
        }

        LazyGridScrollbar(
            state = state,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun IconPreviewItem(
    entry: IconEntry,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(entry.drawableId),
                contentDescription = entry.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(PreviewRootConfig.ICON_DISPLAY_SIZE)
            )
        }
    }
}

@Composable
private fun LazyGridScrollbar(
    state: LazyGridState,
    modifier: Modifier = Modifier
) {
    val scrollbarState by remember(state) {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo.size
            val totalRows =
                (totalItems + PreviewRootConfig.COLUMNS - 1) / PreviewRootConfig.COLUMNS
            val visibleRows =
                (visibleItems + PreviewRootConfig.COLUMNS - 1) / PreviewRootConfig.COLUMNS
            if (totalItems == 0 || visibleRows >= totalRows) {
                ScrollbarState.Hidden
            } else {
                val firstVisibleRow = state.firstVisibleItemIndex / PreviewRootConfig.COLUMNS
                val scrollableRows = (totalRows - visibleRows).coerceAtLeast(1)
                val firstItemHeight = layoutInfo.visibleItemsInfo.firstOrNull()?.size?.height ?: 0
                val rowExtent = (firstItemHeight + layoutInfo.mainAxisItemSpacing).coerceAtLeast(1)
                val rowOffsetFraction =
                    state.firstVisibleItemScrollOffset.toFloat() / rowExtent.toFloat()
                ScrollbarState.Visible(
                    positionFraction =
                        (firstVisibleRow + rowOffsetFraction) / scrollableRows.toFloat(),
                    visibleFraction =
                        (visibleRows.toFloat() / totalRows.toFloat()).coerceAtLeast(
                            PreviewRootConfig.SCROLLBAR_MIN_VISIBLE_FRACTION
                        )
                )
            }
        }
    }
    val thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = PreviewRootConfig.SCROLLBAR_ALPHA
    )

    AnimatedVisibility(
        visible = scrollbarState is ScrollbarState.Visible && state.isScrollInProgress,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val visibleState = scrollbarState as ScrollbarState.Visible
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = PreviewRootConfig.SCROLLBAR_END_PADDING)
                .width(PreviewRootConfig.SCROLLBAR_WIDTH)
        ) {
            val verticalPadding = PreviewRootConfig.SCROLLBAR_VERTICAL_PADDING.toPx()
            val availableHeight = (size.height - verticalPadding * 2).coerceAtLeast(0f)
            val minThumbHeight = PreviewRootConfig.SCROLLBAR_MIN_THUMB_HEIGHT.toPx()
            val thumbHeight = (availableHeight * visibleState.visibleFraction)
                .coerceIn(minThumbHeight.coerceAtMost(availableHeight), availableHeight)
            val thumbTravel = (availableHeight - thumbHeight).coerceAtLeast(0f)
            val thumbTop = verticalPadding +
                    thumbTravel * visibleState.positionFraction.coerceIn(0f, 1f)

            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(
                    x = 0f,
                    y = thumbTop
                ),
                size = Size(width = size.width, height = thumbHeight),
                cornerRadius = CornerRadius(
                    x = PreviewRootConfig.SCROLLBAR_CORNER_RADIUS.toPx(),
                    y = PreviewRootConfig.SCROLLBAR_CORNER_RADIUS.toPx()
                )
            )
        }
    }
}

private sealed interface ScrollbarState {
    data object Hidden : ScrollbarState

    data class Visible(
        val positionFraction: Float,
        val visibleFraction: Float
    ) : ScrollbarState
}
