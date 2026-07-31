package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.SelectionSheet
import com.capybara.hypericonlab.core.designsystem.symbol.archive
import com.capybara.hypericonlab.core.designsystem.symbol.cloud_download
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsVersion
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.ResourceSource
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.LayerBackdrop

// 来源选择 sheet 的选项：本地出厂版本或云端版本
sealed class SourceOption {
    // assets 出厂版本
    object Assets : SourceOption()

    // 已下载的云端版本
    data class Remote(val version: String) : SourceOption()

    // 云端未下载时的占位选项，提示用户先检查更新
    object RemotePending : SourceOption()
}

// 资源来源选择 sheet：始终显示本地 + 云端两项
// 云端有已下载版本时显示版本号，未下载时显示"待检查更新"
// 内部注入 LawniconsResourceManager 读取版本信息，SettingsPage 仅需传递状态控制参数
@Composable
fun SourceOptionSheet(
    currentVersion: LawniconsVersion,
    onDismiss: () -> Unit,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    val resourceManager = koinInject<LawniconsResourceManager>()

    // 始终显示本地 + 云端两项：云端有已下载版本时取最新版本，否则显示占位
    val downloadedVersions = resourceManager.getDownloadedVersions()
    val latestRemoteVersion = downloadedVersions.maxByOrNull { it }
    val sourceOptions = buildList {
        add(SourceOption.Assets)
        if (latestRemoteVersion != null) {
            add(SourceOption.Remote(latestRemoteVersion))
        } else {
            add(SourceOption.RemotePending)
        }
    }
    // 当前激活来源作为选中项
    val currentSelected: SourceOption = when (currentVersion.source) {
        ResourceSource.ASSETS -> SourceOption.Assets
        ResourceSource.REMOTE -> latestRemoteVersion?.let { SourceOption.Remote(it) }
            ?: SourceOption.Assets
    }

    SelectionSheet(
        title = "切换资源来源",
        items = sourceOptions,
        selectedItem = currentSelected,
        onDismiss = onDismiss,
        onConfirm = { option ->
            when (option) {
                SourceOption.Assets -> resourceManager.switchToAssets()
                is SourceOption.Remote -> resourceManager.switchToRemote(option.version)
                // 云端未下载时不切换，用户需先检查更新下载
                SourceOption.RemotePending -> {}
            }
        },
        itemLabel = { option -> formatSourceLabel(option, resourceManager) },
        itemIcon = { option -> sourceOptionIcon(option) },
        // 云端未下载时置灰，仅允许选中本地
        itemEnabled = { option -> option !is SourceOption.RemotePending },
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius
    )
}

// 统一格式：(来源标签) 日期 (commit)，云端未下载显示"待检查更新"
private fun formatSourceLabel(
    option: SourceOption,
    resourceManager: LawniconsResourceManager
): String {
    val label = sourceLabel(option)
    val info = versionInfoOf(option, resourceManager)
    return if (info != null) {
        val date = info.version.substringBefore("-")
        val commit = info.lawniconsCommit.take(7)
        "$label $date ($commit)"
    } else {
        "$label (待检查更新)"
    }
}

// 获取各选项的版本信息用于统一显示
private fun versionInfoOf(
    option: SourceOption,
    resourceManager: LawniconsResourceManager
): LawniconsVersion? = when (option) {
    SourceOption.Assets -> resourceManager.getAssetsVersionInfo()
    is SourceOption.Remote -> resourceManager.getRemoteVersionInfo(option.version)
    SourceOption.RemotePending -> null
}

// 来源标签
private fun sourceLabel(option: SourceOption): String = when (option) {
    SourceOption.Assets -> "内置"
    is SourceOption.Remote, SourceOption.RemotePending -> "云端"
}

// 来源对应图标
@Composable
private fun sourceOptionIcon(option: SourceOption): ImageVector = when (option) {
    SourceOption.Assets -> AppMaterialSymbols.archive
    is SourceOption.Remote, SourceOption.RemotePending -> AppMaterialSymbols.cloud_download
}
