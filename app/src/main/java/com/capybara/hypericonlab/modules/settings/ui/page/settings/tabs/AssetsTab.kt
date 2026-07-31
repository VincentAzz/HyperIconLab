package com.capybara.hypericonlab.modules.settings.ui.page.settings.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import org.koin.compose.koinInject
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
    val resourceManager = koinInject<LawniconsResourceManager>()

    // 当前版本信息（从 manager 观察，来源切换后自动更新）
    val version by resourceManager.currentVersion.collectAsStateWithLifecycle()

    // 版本号展示文本：云端版本显示版本号 + commit，assets 显示出厂版本
    val versionText = when (version.source) {
        ResourceSource.REMOTE -> "${version.version} (${version.lawniconsCommit.take(7)})"
        ResourceSource.ASSETS -> version.version
    }

    // 来源标签
    val sourceText = when (version.source) {
        ResourceSource.REMOTE -> "云端"
        ResourceSource.ASSETS -> "本地"
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
                                text = versionText,
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
                        title = "来源",
                        trailingContent = {
                            Text(
                                text = sourceText,
                                style = MaterialTheme.typography.bodyMedium,
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
                                // 同时显示 svg 图标数和 mapper 映射数
                                text = "${version.svgCount} 图标, ${version.mapperCount} 映射",
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
                            PrimaryActionButton(
                                text = "浏览",
                                onClick = onBrowseLawnicons
                            )
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
