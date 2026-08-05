package com.capybara.hypericonlab.modules.settings.ui.page.settings.sections

import androidx.compose.runtime.Composable
import com.capybara.hypericonlab.BuildConfig
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn

@Composable
fun AssetCleanupSection(
    hasDownloadedAssets: Boolean,
    cacheAvailable: Boolean,
    isAssetUpdateRunning: Boolean,
    onClearAssets: () -> Unit,
    onClearColorCache: () -> Unit,
    onSimulateAssetUpdate: () -> Unit
) {
    SegmentedColumn(title = "调试") {
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "清除已下载资产",
                description = "删除所有云端下载的资产\n测试资产版本回退，重新初始化 1 & 2",
                trailingContent = {
                    PrimaryActionButton(
                        text = if (hasDownloadedAssets) "清除" else "已清除",
                        enabled = hasDownloadedAssets,
                        onClick = onClearAssets
                    )
                }
            )
        }

        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "清除颜色映射缓存",
                description = "删除 App-M3 颜色映射缓存\n测试重新初始化 3",
                trailingContent = {
                    PrimaryActionButton(
                        text = if (cacheAvailable) "清除" else "已清除",
                        enabled = cacheAvailable,
                        onClick = onClearColorCache
                    )
                }
            )
        }

        if (BuildConfig.DEBUG) {
            item {
                BaseWidget(
                    iconPlaceholder = false,
                    title = "模拟资产更新",
                    description = "注入虚拟更新，不变更数据\n20260806 → 20770101\n测试资产更新流程",
                    trailingContent = {
                        PrimaryActionButton(
                            text = "模拟更新",
                            enabled = !isAssetUpdateRunning,
                            onClick = onSimulateAssetUpdate
                        )
                    }
                )
            }
        }
    }
}
