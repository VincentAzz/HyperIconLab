package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.runtime.Composable
import com.capybara.hypericonlab.core.designsystem.component.SelectionSheet
import top.yukonga.miuix.kmp.blur.LayerBackdrop

// 下载方式选项：直连或加速代理
enum class DownloadMode(val label: String) {
    DIRECT("直连"),
    PROXY("代理")
}

// 下载方式选择 sheet：直连 / 加速代理
// 持久化由调用方通过 onConfirm 回调处理（需用不随 sheet 关闭取消的 scope 执行 putBoolean）
@Composable
fun DownloadModeSheet(
    currentUseProxy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (DownloadMode) -> Unit,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
) {
    val currentMode = if (currentUseProxy) DownloadMode.PROXY else DownloadMode.DIRECT
    SelectionSheet(
        title = "下载方式",
        items = DownloadMode.entries,
        selectedItem = currentMode,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        itemLabel = { mode -> mode.label },
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass
    )
}
