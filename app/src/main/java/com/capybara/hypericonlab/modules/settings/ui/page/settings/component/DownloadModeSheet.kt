package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.SelectionSheet
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import com.capybara.hypericonlab.modules.settings.domain.repository.BooleanSetting
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.blur.LayerBackdrop

// 下载方式选项：直连或加速代理
enum class DownloadMode(val label: String) {
    DIRECT("直连"),
    PROXY("代理")
}

// 下载方式选择 sheet：直连 / 加速代理
// 内部注入 AppSettingsRepository 持久化选择，调用方仅需传递当前状态与 dismiss 回调
@Composable
fun DownloadModeSheet(
    currentUseProxy: Boolean,
    onDismiss: () -> Unit,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    val appSettingsRepository = koinInject<AppSettingsRepository>()
    val scope = rememberCoroutineScope()

    val currentMode = if (currentUseProxy) DownloadMode.PROXY else DownloadMode.DIRECT
    SelectionSheet(
        title = "下载方式",
        items = DownloadMode.entries,
        selectedItem = currentMode,
        onDismiss = onDismiss,
        onConfirm = { mode ->
            scope.launch {
                appSettingsRepository.putBoolean(
                    BooleanSetting.UiUseDownloadProxy,
                    mode == DownloadMode.PROXY
                )
            }
        },
        itemLabel = { mode -> mode.label },
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius
    )
}
