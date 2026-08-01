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
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.FailureReason
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.IconPackTemplateState
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsUpdateManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.UpdateState
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
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
    val templateManager = koinInject<IconPackTemplateManager>()
    val appSettingsRepository = koinInject<AppSettingsRepository>()
    val scope = rememberCoroutineScope()

    // 当前版本信息（从 manager 观察，来源切换后自动更新）
    val version by resourceManager.currentVersion.collectAsStateWithLifecycle()
    // 更新流程状态（检查/下载/解压/成功/失败）
    val updateState by updateManager.state.collectAsStateWithLifecycle()
    val templateState by templateManager.state.collectAsStateWithLifecycle()
    val templateAvailable =
        (templateState as? IconPackTemplateState.Available)?.version == version.version
    // 下载代理开关
    val useDownloadProxy by appSettingsRepository.useDownloadProxy.collectAsStateWithLifecycle()

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
    val (baseCheckButtonText, baseCheckEnabled) = updateStateToButton(updateState)
    val templateNeedsRetry = version.source == ResourceSource.REMOTE &&
            (updateState == UpdateState.UpToDate || updateState is UpdateState.Success) &&
            !templateAvailable &&
            (templateState == IconPackTemplateState.Failed ||
                    templateState == IconPackTemplateState.Unavailable)
    val templateInProgress = templateState == IconPackTemplateState.Checking ||
            templateState is IconPackTemplateState.Downloading
    val checkButtonText = when {
        templateState is IconPackTemplateState.Downloading -> "模板下载中"
        templateState == IconPackTemplateState.Checking -> "模板检查中"
        templateNeedsRetry -> "重试"
        else -> baseCheckButtonText
    }
    val checkEnabled = if (templateInProgress) false else baseCheckEnabled || templateNeedsRetry

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(backdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier),
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(layoutDirection) + outerPadding.calculateStartPadding(
                layoutDirection
            ),
            top = paddingValues.calculateTopPadding(),
            end = paddingValues.calculateEndPadding(layoutDirection) + outerPadding.calculateEndPadding(
                layoutDirection
            ),
            bottom = outerPadding.calculateBottomPadding(),
        ),
    ) {
        item(key = "lawnicons") {
            SegmentedColumn(title = "Lawnicons") {
                // 版本 + 来源合并显示：20260731 (ba36a38) - 云端
                item {
                    BaseWidget(
                        iconPlaceholder = false, title = "版本", trailingContent = {
                            Text(
                                text = versionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = GoogleSansCodeFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        })
                }

                item {
                    BaseWidget(
                        iconPlaceholder = false, title = "图标数量", trailingContent = {
                            Text(
                                text = "${version.svgCount} 图标, ${version.mapperCount} 映射",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        })
                }

                // 检查更新：云端来源显示，或仅有本地资产时显示（需保留更新入口）
                // 仅当本地来源且已有云端版本共存时隐藏（用户手动切回本地无需更新）
                val hasCloudVersions = resourceManager.getDownloadedVersions().isNotEmpty()
                val showUpdateEntry = version.source == ResourceSource.REMOTE || !hasCloudVersions

                item(animatedVisibility = showUpdateEntry) {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "检查资源更新",
                        description = updateStateDescription(updateState),
                        trailingContent = {
                            PrimaryActionButton(
                                text = checkButtonText, enabled = checkEnabled, onClick = {
                                    scope.launch { updateManager.checkAndInstall() }
                                })
                        })
                }

                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "APK 模板",
                        description = "模板与云端 Lawnicons 版本严格绑定",
                        trailingContent = {
                            Text(
                                text = templateStateText(
                                    version.source,
                                    templateState,
                                    templateAvailable
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }

                // 下载方式：与检查更新同条件显示
                item(animatedVisibility = showUpdateEntry) {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "下载方式",
                        description = "加速代理可提升 GitHub 资源下载速度",
                        trailingContent = {
                            PrimaryActionButton(
                                text = downloadModeText, onClick = onChooseDownloadMode
                            )
                        })
                }

                // 来源：弹出选择 sheet，选项含本地与已下载云端版本
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "来源",
                        description = "在内置版本与云端版本间切换",
                        trailingContent = {
                            PrimaryActionButton(
                                text = "选择", onClick = onSwitchSource
                            )
                        })
                }

                // 浏览原始 SVG 图标
                item {
                    BaseWidget(
                        iconPlaceholder = false,
                        title = "浏览SVG图标",
                        description = "查看 Lawnicons 仓库的全部 SVG",
                        trailingContent = {
                            PrimaryActionButton(
                                text = "浏览", onClick = onBrowseLawnicons
                            )
                        })
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
                                text = "清除", onClick = {
                                    scope.launch {
                                        resourceManager.clearCloudAssets()
                                        updateManager.resetState()
                                    }
                                })
                        })
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


// 更新状态描述文本
private fun updateStateDescription(state: UpdateState): String? = when (state) {
    UpdateState.Idle -> "同时检查 Lawnicons 与配套 APK 模板"
    is UpdateState.Checking -> "正在检查云端版本..."
    is UpdateState.Downloading -> "正在下载资源包..."
    is UpdateState.Extracting -> "正在解压资源..."
    is UpdateState.Success -> "已切换到版本 ${state.newVersion}"
    is UpdateState.Failed -> FailureMessage.forReason(state.reason)
    UpdateState.UpToDate -> "当前已是最新版本"
}

private fun templateStateText(
    source: ResourceSource,
    state: IconPackTemplateState,
    templateAvailable: Boolean
): String = if (source == ResourceSource.ASSETS) {
    "内置版本不支持"
} else if (templateAvailable) {
    "已就绪"
} else {
    when (state) {
        IconPackTemplateState.Idle -> "待检查"
        IconPackTemplateState.Checking -> "检查中"
        is IconPackTemplateState.Downloading ->
            "下载中 ${ButtonConstants.PERCENT_FMT.format((state.progress * ButtonConstants.PERCENT_SCALE).toInt())}"

        is IconPackTemplateState.Available -> "已就绪 (${state.version})"
        IconPackTemplateState.Unavailable -> "当前版本未提供"
        IconPackTemplateState.Failed -> "更新失败"
    }
}

private object FailureMessage {
    const val RATE_LIMITED = "GitHub API 限速，请稍后重试或切换网络"

    const val NETWORK_ERROR = "连接失败，请检查网络连接"

    const val TIMEOUT = "连接超时，请检查网络连接"

    const val HTTP_ERROR = "连接异常，请稍后重试"

    const val CORRUPTED = "校验失败，请重新下载"

    const val PARSE_ERROR = "解析失败"

    const val EXTRACT_FAILED = "解压失败"

    const val ACTIVATE_FAILED = "版本切换失败"

    const val UNKNOWN = "更新失败，请重试"

    fun forReason(reason: FailureReason): String = when (reason) {
        FailureReason.RATE_LIMITED -> RATE_LIMITED
        FailureReason.NETWORK_ERROR -> NETWORK_ERROR
        FailureReason.TIMEOUT -> TIMEOUT
        FailureReason.HTTP_ERROR -> HTTP_ERROR
        FailureReason.CORRUPTED -> CORRUPTED
        FailureReason.PARSE_ERROR -> PARSE_ERROR
        FailureReason.EXTRACT_FAILED -> EXTRACT_FAILED
        FailureReason.ACTIVATE_FAILED -> ACTIVATE_FAILED
        FailureReason.UNKNOWN -> UNKNOWN
    }
}

private object ButtonConstants {
    const val PERCENT_SCALE = 100
    const val PERCENT_FMT = "%d%%"
}
