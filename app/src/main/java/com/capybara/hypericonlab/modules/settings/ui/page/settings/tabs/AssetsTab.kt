package com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.AppVersion
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantCapsuleShape
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
fun AssetsTab(
    paddingValues: PaddingValues,
    outerPadding: PaddingValues,
    backdrop: LayerBackdrop?,
    onBrowseLawnicons: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val context = LocalContext.current

    var iconCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            iconCount = try {
                context.assets
                    .open("${AssetsConstants.MAPPER_ASSET_DIR}/${AssetsConstants.FULL_MAPPER_FILE}")
                    .use { IconMapperProcessor.parseIconMapper(it).size }
            } catch (_: Exception) {
                0
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) +
                    outerPadding.calculateStartPadding(layoutDirection),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection) +
                    outerPadding.calculateEndPadding(layoutDirection),
            bottom = outerPadding.calculateBottomPadding(),
        ),
    ) {
        item(key = "lawnicons") {
            SegmentedColumn(title = "Lawnicons") {

                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "版本",
                        trailingContent = {
                            Text(
                                text = AppVersion.LAWNICONS_VERSION,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = GoogleSansCodeFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "图标数量",
                        trailingContent = {
                            Text(
                                text = "$iconCount 个图标",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                // 浏览原始 SVG 图标
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "浏览原始图标",
                        description = "查看 lawnicons 仓库的全部 SVG",
                        trailingContent = {
                            Surface(
                                onClick = onBrowseLawnicons,
                                shape = rememberKyantCapsuleShape(),
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.height(AssetsUiConstants.BROWSE_BUTTON_HEIGHT)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "浏览",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        item(key = "navPadding") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private object AssetsConstants {
    const val MAPPER_ASSET_DIR = "icon_mapper"
    const val FULL_MAPPER_FILE = "icon_mapper.xml"
}

private object AssetsUiConstants {
    val BROWSE_BUTTON_HEIGHT = 36.dp
}
