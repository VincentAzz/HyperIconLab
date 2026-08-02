package com.capybara.hypericonlab.iconpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.theme.IconPackTheme
import com.capybara.hypericonlab.iconpack.ui.theme.CornerRadius as AppCornerRadius
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantRoundedRectangleShape
import org.xmlpull.v1.XmlPullParser

private object PreviewRootConfig {
    const val COLUMNS = 4
    val GRID_HORIZONTAL_PADDING = 16.dp
    val GRID_VERTICAL_PADDING = 16.dp
    val GRID_BOTTOM_PADDING = 32.dp
    val GRID_SPACING = 8.dp
    val ICON_CELL_SIZE = 56.dp
    val ICON_DISPLAY_SIZE = 40.dp
    val SCROLLBAR_WIDTH = 4.dp
    val SCROLLBAR_END_PADDING = 4.dp
    val SCROLLBAR_VERTICAL_PADDING = 8.dp
    val SCROLLBAR_MIN_THUMB_HEIGHT = 40.dp
    val SCROLLBAR_CORNER_RADIUS = 2.dp
    const val SCROLLBAR_MIN_VISIBLE_FRACTION = 0.08f
    const val SCROLLBAR_ALPHA = 0.55f
}

/**
 * 图标包预览入口：读取 CI 生成的预览索引，并交由 Compose 页面展示。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IconPackTheme {
                val entries = remember { loadPreviewEntries() }
                PreviewRoot(entries)
            }
        }
    }

    private fun loadPreviewEntries(): List<IconEntry> = buildList {
        try {
            resources.getXml(R.xml.preview_icons).use { parser ->
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType != XmlPullParser.START_TAG || parser.name != "item") {
                        continue
                    }
                    val drawable = parser.getAttributeValue(null, "drawable") ?: continue
                    val drawableId = resources.getIdentifier(drawable, "drawable", packageName)
                    if (drawableId == 0) continue

                    add(
                        IconEntry(
                            name = parser.getAttributeValue(null, "name"),
                            packageName = parser.getAttributeValue(null, "package"),
                            drawable = drawable,
                            drawableId = drawableId
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // 预览读取失败时保持空列表，模板静态校验负责报告资源问题。
        }
    }
}

@Composable
private fun PreviewRoot(entries: List<IconEntry>) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "没有可预览的图标",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            IconPreviewGrid(entries)
        }
    }
}

@Composable
private fun IconPreviewGrid(entries: List<IconEntry>) {
    val gridState = rememberLazyGridState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(PreviewRootConfig.COLUMNS),
            state = gridState,
            contentPadding = PaddingValues(
                start = PreviewRootConfig.GRID_HORIZONTAL_PADDING,
                top = PreviewRootConfig.GRID_VERTICAL_PADDING,
                end = PreviewRootConfig.GRID_HORIZONTAL_PADDING,
                bottom = PreviewRootConfig.GRID_BOTTOM_PADDING
            ),
            horizontalArrangement = Arrangement.spacedBy(PreviewRootConfig.GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(PreviewRootConfig.GRID_SPACING),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = entries.size,
                key = { index -> entries[index].stableKey }
            ) { index ->
                IconPreviewItem(entries[index])
            }
        }

        LazyGridScrollbar(
            state = gridState,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun IconPreviewItem(entry: IconEntry) {
    val itemShape = rememberKyantRoundedRectangleShape(AppCornerRadius)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(PreviewRootConfig.ICON_CELL_SIZE),
            shape = itemShape,
            color = MaterialTheme.colorScheme.surfaceContainer
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

    if (scrollbarState is ScrollbarState.Visible) {
        val visibleState = scrollbarState as ScrollbarState.Visible
        Canvas(
            modifier = modifier
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

data class IconEntry(
    val name: String?,
    val packageName: String?,
    val drawable: String,
    val drawableId: Int
) {
    val displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: drawable

    val stableKey: String
        get() = "${packageName.orEmpty()}:$drawable"
}
