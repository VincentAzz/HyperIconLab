package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.iconpack.ui.theme.ConnectionRadius
import com.capybara.hypericonlab.iconpack.ui.theme.CornerRadius
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantUnevenRoundedRectangleShape

private object SegmentedColumnConfig {
    val CONTENT_PADDING = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
}

@Immutable
internal data class SegmentedItem(
    val key: Any,
    val content: @Composable (Shape) -> Unit
)

class SegmentedColumnScope internal constructor() {
    internal val items = mutableListOf<SegmentedItem>()

    fun item(
        key: Any? = null,
        content: @Composable (Shape) -> Unit
    ) {
        items += SegmentedItem(key = key ?: items.size, content = content)
    }
}

/**
 * 主应用 SegmentedColumn 的静态精简版，保留分段间距和连续曲率圆角。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    content: SegmentedColumnScope.() -> Unit
) {
    val items = remember(content) {
        SegmentedColumnScope().apply(content).items.toList()
    }
    if (items.isEmpty()) return

    Column(modifier = modifier.padding(SegmentedColumnConfig.CONTENT_PADDING)) {
        items.forEachIndexed { index, item ->
            val isFirst = index == 0
            val isLast = index == items.lastIndex
            val shape = rememberKyantUnevenRoundedRectangleShape(
                topStart = if (isFirst) CornerRadius else ConnectionRadius,
                topEnd = if (isFirst) CornerRadius else ConnectionRadius,
                bottomEnd = if (isLast) CornerRadius else ConnectionRadius,
                bottomStart = if (isLast) CornerRadius else ConnectionRadius
            )
            key(item.key) {
                Column(
                    modifier = Modifier.padding(
                        top = if (isFirst) 0.dp else ListItemDefaults.SegmentedGap
                    )
                ) {
                    item.content(shape)
                }
            }
        }
    }
}
