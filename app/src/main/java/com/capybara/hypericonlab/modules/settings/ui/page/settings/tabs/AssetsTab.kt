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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsUpdateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.UpdateState
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import com.capybara.hypericonlab.modules.settings.domain.repository.BooleanSetting
import com.capybara.hypericonlab.modules.settings.ui.page.settings.component.DownloadMode
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
fun AssetsTab(
    paddingValues: PaddingValues,
    outerPadding: PaddingValues,
    backdrop: LayerBackdrop?,
    onBrowseLawnicons: () -> Unit,
    onSwitchSource: () -> Unit,
    onChooseDownloadMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    val resourceManager = koinInject<LawniconsResourceManager>()
    val updateManager = koinInject<LawniconsUpdateManager>()
    val appSettingsRepository = koinInject<AppSettingsRepository>()
    val scope = rememberCoroutineScope()

    // 当前版本信息（从 manager 观察，来源切换后自动更新）
    val version by resourceManager.currentVersion.collectAsStateWithLifecycle()
    // 更新流程状态（检查/下载/解压/成功/失败）
    val updateState by updateManager.state.collectAsStateWithLifecycle()
    // 下载代理开关（默认关闭）
    val useDownloadProxy by appSettingsRepository
        .getBoolean(BooleanSetting.UiUseDownloadProxy, default = false)
        .collectAsStateWithLifecycle(initialValue = false)

    // 版本号 + 来源统一显示：日期 (commit) - 内置/云端
    val versionDate = version.version.substringBefore("-")
    val commitShort = version.lawniconsCommit.take(7)
    val sourceLabel = when (version.source) {
        ResourceSource.REMOTE -> "云端"
        ResourceSource.ASSETS -> "内置"
    }
    val versionText = "$versionDate ($commitShort) - $sourceLabel"

    // 下载方式显示文本
    val downloadModeText =
        if (useDownloadProxy) DownloadMode.PROXY.label else DownloadMode.DIRECT.label

    // 检查更新按钮文本与启用状态
    val (checkButtonText, checkEnabled) = updateStateToButton(updateState)

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
                // 版本 + 来源合并显示：20260731 (ba36a38) - 云端
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
                        title = "图标数量",
                        trailingContent = {
                            Text(
                                text = "${version.svgCount} 图标, ${version.mapperCount} 映射",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // 检查更新：云端来源显示，或仅有本地资产时显示（需保留更新入口）
                // 仅当本地来源且已有云端版本共存时隐藏（用户手动切回本地无需更新）
                val hasCloudVersions = resourceManager.getDownloadedVersions().isNotEmpty()
                val showUpdateEntry = version.source == ResourceSource.REMOTE || !hasCloudVersions
                if (showUpdateEntry) {
                    item {
                        BaseWidget(
                            iconPlaceholder = false,
                            title = "检查更新",
                            description = updateStateDescription(updateState),
                            trailingContent = {
                                PrimaryActionButton(
                                    text = checkButtonText,
                                    enabled = checkEnabled,
                                    onClick = {
                                        scope.launch { updateManager.checkAndInstall() }
                                    }
                                )
                            }
                        )
                    }
                }

                // 下载方式：与检查更新同条件显示
                if (showUpdateEntry) {
                    item {
                        BaseWidget(
                            iconPlaceholder = false,
                            title = "下载方式",
                            description = "加速代理可提升 GitHub 资源下载速度",
                            trailingContent = {
                                PrimaryActionButton(
                                    text = downloadModeText,
                                    onClick = onChooseDownloadMode
                                )
                            }
                        )
                    }
                }

                // 来源：弹出选择 sheet，选项含本地与已下载云端版本
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "来源",
                        description = "在内置版本与云端版本间切换",
                        trailingContent = {
                            PrimaryActionButton(
                                text = "选择",
                                onClick = onSwitchSource
                            )
                        }
                    )
                }

                // 浏览原始 SVG 图标
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

        // 清除资产缓存：删除所有云端下载资源并切回本地（调试用，上线前移除）
        item(key = "clearCache") {
            SegmentedColumn(title = "调试") {
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "清除已下载资产",
                        description = "删除所有云端下载的 Lawnicons 资产\n仅保留应用内置版本",
                        trailingContent = {
                            PrimaryActionButton(
                                text = "清除",
                                onClick = {
                                    scope.launch {
                                        resourceManager.clearCloudAssets()
                                        updateManager.resetState()
                                    }
                                }
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

// 更新状态映射为按钮文本，进行中状态禁用点击
private fun updateStateToButton(state: UpdateState): Pair<String, Boolean> = when (state) {
    UpdateState.Idle -> "检查" to true
    is UpdateState.Checking -> "检查中" to false
    is UpdateState.Downloading -> "下载中 ${ButtonConstants.PERCENT_FMT.format((state.progress * ButtonConstants.PERCENT_SCALE).toInt())}" to false
    is UpdateState.Extracting -> "解压中 ${ButtonConstants.PERCENT_FMT.format((state.progress * ButtonConstants.PERCENT_SCALE).toInt())}" to false
    is UpdateState.Success -> "已更新" to true
    is UpdateState.Failed -> "重试" to true
    UpdateState.UpToDate -> "已是最新" to false
}


// 更新状态描述文本，显示在检查更新行下方
private fun updateStateDescription(state: UpdateState): String? = when (state) {
    UpdateState.Idle -> null
    is UpdateState.Checking -> "正在检查云端版本..."
    is UpdateState.Downloading -> "正在下载资源包..."
    is UpdateState.Extracting -> "正在解压资源..."
    is UpdateState.Success -> "已切换到版本 ${state.newVersion}"
    is UpdateState.Failed -> state.reason
    UpdateState.UpToDate -> "当前已是最新版本"
}

private object ButtonConstants {
    const val PERCENT_SCALE = 100
    val PERCENT_FMT = "%d%%"
}
