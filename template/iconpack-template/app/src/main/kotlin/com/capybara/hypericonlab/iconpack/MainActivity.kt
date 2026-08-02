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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.capybara.hypericonlab.iconpack.ui.component.IconDetailSheet
import com.capybara.hypericonlab.iconpack.ui.component.IconPreviewGrid
import com.capybara.hypericonlab.iconpack.ui.component.PreviewSearchField
import com.capybara.hypericonlab.iconpack.ui.component.PreviewTopAppBar
import com.capybara.hypericonlab.iconpack.ui.component.SearchUiConfig
import com.capybara.hypericonlab.iconpack.ui.symbol.search
import com.capybara.hypericonlab.iconpack.ui.symbol.search_off
import com.capybara.hypericonlab.iconpack.ui.theme.AppMaterialSymbols
import com.capybara.hypericonlab.iconpack.ui.theme.IconPackTheme
import org.xmlpull.v1.XmlPullParser

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

    var isFabVisible by remember { mutableStateOf(true) }
    var lastIndex by remember { mutableIntStateOf(0) }
    var lastOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        val currentIndex = gridState.firstVisibleItemIndex
        val currentOffset = gridState.firstVisibleItemScrollOffset

        if (currentIndex == 0 && currentOffset == 0) {
            isFabVisible = true
        } else if (currentIndex > lastIndex || (currentIndex == lastIndex && currentOffset > lastOffset)) {
            isFabVisible = false
        } else if (currentIndex < lastIndex || (currentIndex == lastIndex && currentOffset < lastOffset)) {
            isFabVisible = true
        }
        lastIndex = currentIndex
        lastOffset = currentOffset
    }

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
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "没有可预览的图标",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    PreviewTopAppBar(
                        visibleEntryCount = filteredEntries.size,
                        totalEntryCount = entries.size,
                        isSearchActive = isSearchActive
                    )

                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        PreviewSearchField(
                            value = searchQuery,
                            focusRequester = focusRequester,
                            onValueChange = {
                                searchQuery = it
                                selectedEntry = null
                            }
                        )
                    }

                    if (filteredEntries.isEmpty()) {
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
                            entries = filteredEntries,
                            state = gridState,
                            onEntryClick = { selectedEntry = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isFabVisible,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (isSearchActive) {
                                isSearchActive = false
                                searchQuery = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            } else {
                                isSearchActive = true
                            }
                        },
                        modifier = Modifier
                            .imePadding()
                            .padding(
                                end = SearchUiConfig.FAB_END_PADDING,
                                bottom = SearchUiConfig.FAB_BOTTOM_PADDING
                            ),
                        shape = CircleShape,
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
        }
    }

    selectedEntry?.let { entry ->
        IconDetailSheet(
            entry = entry,
            onDismiss = { selectedEntry = null }
        )
    }
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
