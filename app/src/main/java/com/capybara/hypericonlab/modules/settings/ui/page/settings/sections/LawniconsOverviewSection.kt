package com.capybara.hypericonlab.modules.settings.ui.page.settings.sections

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState

@Composable
fun LawniconsOverviewSection(
    versionText: String,
    iconCountText: String,
    templateVersionText: String,
    assetUpdateState: AssetUpdateCheckState,
    assetUpdateRunning: Boolean,
    canCheckAssetUpdates: Boolean,
    downloadModeText: String,
    onChooseDownloadMode: () -> Unit,
    onSwitchSource: () -> Unit,
    onBrowseLawnicons: () -> Unit,
    onCheckAssetUpdates: () -> Unit,
    onUpdateAssets: () -> Unit
) {
    SegmentedColumn(title = "Lawnicons") {
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "版本",
                trailingContent = {
                    OverviewValueText(text = versionText, useCodeFont = true)
                }
            )
        }

        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "图标数量",
                trailingContent = {
                    OverviewValueText(text = iconCountText)
                }
            )
        }

        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "APK 模板",
                trailingContent = {
                    OverviewValueText(text = templateVersionText, useCodeFont = true)
                }
            )
        }

        item {
            AssetUpdateCheckSection(
                state = assetUpdateState,
                isUpdating = assetUpdateRunning,
                canCheck = canCheckAssetUpdates,
                onCheck = onCheckAssetUpdates,
                onUpdate = onUpdateAssets
            )
        }

        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "下载方式",
                description = "加速代理可提升 GitHub 资源下载速度",
                trailingContent = {
                    PrimaryActionButton(
                        text = "选择",
                        onClick = onChooseDownloadMode
                    )
                }
            )
        }

        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "来源",
                description = "在内置版本与云端版本间切换",
                trailingContent = {
                    PrimaryActionButton(
                        text = "切换",
                        onClick = onSwitchSource
                    )
                }
            )
        }

        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "浏览SVG图标",
                description = "查看 Lawnicons 仓库的全部 SVG",
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

@Composable
private fun OverviewValueText(
    text: String,
    useCodeFont: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = if (useCodeFont) GoogleSansCodeFontFamily else null,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
