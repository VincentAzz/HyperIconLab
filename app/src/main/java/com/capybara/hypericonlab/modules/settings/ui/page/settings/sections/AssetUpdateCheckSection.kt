package com.capybara.hypericonlab.modules.settings.ui.page.settings.sections

import androidx.compose.runtime.Composable
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.AssetUpdateCheckState

@Composable
fun AssetUpdateCheckSection(
    state: AssetUpdateCheckState,
    isUpdating: Boolean,
    canCheck: Boolean,
    onCheck: () -> Unit,
    onUpdate: () -> Unit
) {
    val isChecking = state is AssetUpdateCheckState.Checking
    val hasUpdate = state is AssetUpdateCheckState.Available
    val buttonText = when {
        isUpdating -> "正在更新"
        isChecking -> "正在检查"
        hasUpdate -> "更新"
        state is AssetUpdateCheckState.UpToDate -> "已是最新"
        else -> "检查"
    }
    val buttonEnabled = when {
        isUpdating || isChecking -> false
        hasUpdate -> true
        else -> canCheck
    }
    val description = when {
        isUpdating -> "正在更新资产"
        isChecking -> "正在检查资产更新"
        state is AssetUpdateCheckState.Available ->
            "资产更新可用 ${state.availableRelease.version}"

        state is AssetUpdateCheckState.UpToDate -> "当前资产已是最新"
        state is AssetUpdateCheckState.Failed -> "检查失败，请稍后重试"
        else -> "检查资产是否有更新"
    }

    BaseWidget(
        iconPlaceholder = false,
        title = "检查资产更新",
        description = description,
        enabled = buttonEnabled,
        trailingContent = {
            PrimaryActionButton(
                text = buttonText,
                enabled = buttonEnabled,
                onClick = if (hasUpdate) onUpdate else onCheck
            )
        }
    )
}
