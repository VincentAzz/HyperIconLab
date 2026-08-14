package com.capybara.hypericonlab.modules.icon.ui.page.custom.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.config.SheetConfigCardOuterVerticalPadding
import com.capybara.hypericonlab.core.designsystem.config.currentPreferredCardCornerRadius
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.render.image.MaskAssetLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ConfigCard(
    title: String,
    valueDisplay: String? = null,
    // 可选卡片容器色，默认 null 时使用 surfaceBright；调用方可覆盖（如 sheet 场景传 surfaceBright.copy(alpha = 0.8f) 跟随 LogSheet 风格）
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val cardCornerRadius = currentPreferredCardCornerRadius()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SheetConfigCardOuterVerticalPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (valueDisplay != null) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = rememberKyantRoundedRectangleShape(cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceBright
            )
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        for (placeable in placeables) {
            if (currentRowWidth + placeable.width + mainAxisSpacing.roundToPx() > constraints.maxWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + mainAxisSpacing.roundToPx()
        }
        rows.add(currentRow)

        val height = rows.sumOf { it.maxOfOrNull { p -> p.height } ?: 0 } +
                (rows.size - 1) * crossAxisSpacing.roundToPx()
        layout(constraints.maxWidth, height) {
            var y = 0
            for (row in rows) {
                var x = 0
                val rowHeight = row.maxOf { it.height }
                for (placeable in row) {
                    placeable.place(x, y + (rowHeight - placeable.height) / 2)
                    x += placeable.width + mainAxisSpacing.roundToPx()
                }
                y += rowHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}

@Composable
fun MaskThumbnail(mask: String) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(mask) {
        withContext(Dispatchers.IO) {
            bitmap = MaskAssetLoader.loadBitmap(context, mask)
        }
    }

    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = mask,
            modifier = Modifier.size(32.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
fun ColorPreviewItem(label: String, hex: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Color(
                        android.graphics.Color.parseColor(
                            if (hex.length == 7) hex.replace(
                                "#",
                                "#FF"
                            ) else hex
                        )
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable { onClick() }
        )
        val displayHex = if (hex.startsWith("#")) {
            if (hex.length == 7) hex.replace("#", "#FF") else hex
        } else {
            if (hex.length == 6) "#FF$hex" else "#$hex"
        }
        Text(text = displayHex, style = MaterialTheme.typography.labelSmall)
    }
}
