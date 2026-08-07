package com.capybara.hypericonlab.modules.settings.ui.page.settings.sections

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.capybara.hypericonlab.BuildConfig
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.symbol.cloud_download
import com.capybara.hypericonlab.core.designsystem.symbol.delete
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols

@Composable
fun AssetDebugSection(
    hasDownloadedAssets: Boolean,
    cacheAvailable: Boolean,
    isAssetUpdateRunning: Boolean,
    isSimulatedAssetUpdateTriggered: Boolean,
    onClearAssets: () -> Unit,
    onClearColorCache: () -> Unit,
    onSimulateAssetUpdate: () -> Unit
) {
    SegmentedColumn(title = "调试") {
        item {
            BaseWidget(
                icon = AppMaterialSymbols.delete,
                iconColor = MaterialTheme.colorScheme.error,
                title = "清除已下载资产",
                description = "删除所有云端下载的资产\n用于测试资产版本回退\n重新初始化 1 & 2",
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
                icon = AppMaterialSymbols.delete,
                iconColor = MaterialTheme.colorScheme.error,
                title = "清除颜色映射缓存",
                description = "删除 App-M3 颜色映射缓存\n用于测试刷新颜色缓存\n重新初始化 3",
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
                    icon = AppMaterialSymbols.cloud_download,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "模拟资产更新",
                    description = "注入虚拟更新\n测试资产更新流程，不变更真实数据",
                    trailingContent = {
                        PrimaryActionButton(
                            text = if (isSimulatedAssetUpdateTriggered) "已触发" else "触发",
                            enabled = !isAssetUpdateRunning && !isSimulatedAssetUpdateTriggered,
                            onClick = onSimulateAssetUpdate
                        )
                    }
                )
            }
        }
    }
}
