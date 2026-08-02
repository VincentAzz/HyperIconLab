package com.capybara.hypericonlab.iconpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.component.FloatingBottomSheet
import com.capybara.hypericonlab.iconpack.ui.component.SegmentedColumn
import com.capybara.hypericonlab.iconpack.ui.symbol.search
import com.capybara.hypericonlab.iconpack.ui.symbol.search_off
import com.capybara.hypericonlab.iconpack.ui.theme.AppMaterialSymbols
import com.capybara.hypericonlab.iconpack.ui.theme.ExtraLargeRadius
import com.capybara.hypericonlab.iconpack.ui.theme.IconPackTheme
import com.capybara.hypericonlab.iconpack.ui.theme.LargeCardRadius
import com.capybara.hypericonlab.iconpack.ui.theme.CornerRadius as AppCornerRadius
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantRoundedRectangleShape
import org.xmlpull.v1.XmlPullParser

private object PreviewRootConfig {
    const val COLUMNS = 4
    val GRID_HORIZONTAL_PADDING = 16.dp
    val GRID_VERTICAL_PADDING = 16.dp
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

private object IconDetailConfig {
    val TOP_PADDING = 24.dp
    val ICON_CONTAINER_SIZE = 112.dp
    val ICON_SIZE = 80.dp
    val ICON_BOTTOM_SPACING = 8.dp
    val ROW_HORIZONTAL_PADDING = 16.dp
    val ROW_VERTICAL_PADDING = 12.dp
    val LABEL_BOTTOM_PADDING = 4.dp
}

private object SearchUiConfig {
    val FIELD_HORIZONTAL_PADDING = 16.dp
    val FIELD_TOP_PADDING = 16.dp
    val FIELD_BOTTOM_PADDING = 8.dp
    val FAB_END_PADDING = 16.dp
    val FAB_BOTTOM_PADDING = 16.dp
    val FAB_ICON_SIZE = 24.dp
    val GRID_BOTTOM_PADDING_WITH_FAB = 88.dp
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
    var selectedEntry by remember { mutableStateOf<IconEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val gridState = rememberLazyGridState()
    val filteredEntries by remember(entries) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isEmpty()) {
                entries
            } else {
                entries.filter { it.matches(query) }
            }
        }
    }

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
            PreviewContent(
                entries = filteredEntries,
                gridState = gridState,
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                focusRequester = focusRequester,
                onSearchQueryChange = {
                    searchQuery = it
                    selectedEntry = null
                },
                onSearchToggle = {
                    if (isSearchActive) {
                        isSearchActive = false
                        searchQuery = ""
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    } else {
                        isSearchActive = true
                    }
                },
                onEntryClick = { selectedEntry = it }
            )
        }
    }

    selectedEntry?.let { entry ->
        IconDetailSheet(
            entry = entry,
            onDismiss = { selectedEntry = null }
        )
    }
}

@Composable
private fun PreviewContent(
    entries: List<IconEntry>,
    gridState: LazyGridState,
    searchQuery: String,
    isSearchActive: Boolean,
    focusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchToggle: () -> Unit,
    onEntryClick: (IconEntry) -> Unit
) {
    val fabShape = rememberKyantRoundedRectangleShape(LargeCardRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSearchActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                PreviewSearchField(
                    value = searchQuery,
                    focusRequester = focusRequester,
                    onValueChange = onSearchQueryChange
                )
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到匹配的图标",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                IconPreviewGrid(
                    entries = entries,
                    state = gridState,
                    onEntryClick = onEntryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        FloatingActionButton(
            onClick = onSearchToggle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = SearchUiConfig.FAB_END_PADDING,
                    bottom = SearchUiConfig.FAB_BOTTOM_PADDING
                ),
            shape = fabShape,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = if (isSearchActive) {
                    AppMaterialSymbols.search_off
                } else {
                    AppMaterialSymbols.search
                },
                contentDescription = if (isSearchActive) "关闭搜索" else "搜索",
                modifier = Modifier.size(SearchUiConfig.FAB_ICON_SIZE)
            )
        }
    }
}

@Composable
private fun PreviewSearchField(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val fieldShape = rememberKyantRoundedRectangleShape(AppCornerRadius)

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SearchUiConfig.FIELD_HORIZONTAL_PADDING,
                top = SearchUiConfig.FIELD_TOP_PADDING,
                end = SearchUiConfig.FIELD_HORIZONTAL_PADDING,
                bottom = SearchUiConfig.FIELD_BOTTOM_PADDING
            )
            .focusRequester(focusRequester),
        label = { Text("搜索名称、包名或 Drawable") },
        leadingIcon = {
            Icon(
                imageVector = AppMaterialSymbols.search,
                contentDescription = null
            )
        },
        singleLine = true,
        shape = fieldShape
    )
}

@Composable
private fun IconPreviewGrid(
    entries: List<IconEntry>,
    state: LazyGridState,
    onEntryClick: (IconEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(PreviewRootConfig.COLUMNS),
            state = state,
            contentPadding = PaddingValues(
                start = PreviewRootConfig.GRID_HORIZONTAL_PADDING,
                top = PreviewRootConfig.GRID_VERTICAL_PADDING,
                end = PreviewRootConfig.GRID_HORIZONTAL_PADDING,
                bottom = SearchUiConfig.GRID_BOTTOM_PADDING_WITH_FAB
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
    val itemShape = rememberKyantRoundedRectangleShape(AppCornerRadius)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
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
private fun IconDetailSheet(
    entry: IconEntry,
    onDismiss: () -> Unit
) {
    val iconShape = rememberKyantRoundedRectangleShape(ExtraLargeRadius)

    FloatingBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = IconDetailConfig.TOP_PADDING)
                    .size(IconDetailConfig.ICON_CONTAINER_SIZE),
                shape = iconShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(entry.drawableId),
                        contentDescription = entry.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(IconDetailConfig.ICON_SIZE)
                    )
                }
            }

            Spacer(modifier = Modifier.size(IconDetailConfig.ICON_BOTTOM_SPACING))

            SegmentedColumn(modifier = Modifier.fillMaxWidth()) {
                item(key = "name") {
                    IconDetailRow(
                        label = "名称",
                        value = entry.displayName,
                        shape = it
                    )
                }
                item(key = "drawable") {
                    IconDetailRow(
                        label = "Drawable",
                        value = entry.drawable,
                        shape = it
                    )
                }
                item(key = "package") {
                    IconDetailRow(
                        label = "包名",
                        value = entry.packageName?.takeIf(String::isNotBlank) ?: "未关联应用",
                        shape = it
                    )
                }
            }
        }
    }
}

@Composable
private fun IconDetailRow(
    label: String,
    value: String,
    shape: Shape
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = IconDetailConfig.ROW_HORIZONTAL_PADDING,
                vertical = IconDetailConfig.ROW_VERTICAL_PADDING
            )
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = IconDetailConfig.LABEL_BOTTOM_PADDING)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
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

    fun matches(query: String): Boolean =
        displayName.contains(query, ignoreCase = true) ||
                packageName.orEmpty().contains(query, ignoreCase = true) ||
                drawable.contains(query, ignoreCase = true)
}
