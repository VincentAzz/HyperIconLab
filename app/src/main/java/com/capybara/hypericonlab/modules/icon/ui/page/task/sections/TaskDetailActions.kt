package com.capybara.hypericonlab.modules.icon.ui.page.task.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.capybara.hypericonlab.core.designsystem.symbol.category
import com.capybara.hypericonlab.core.designsystem.symbol.delete
import com.capybara.hypericonlab.core.designsystem.symbol.refresh
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.currentSheetRoundedLayout
import com.capybara.hypericonlab.modules.build.domain.model.BuildTask
import com.capybara.hypericonlab.modules.build.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.icon.ui.page.task.component.TaskDetailSheetConfig

// 任务详情底部操作按钮区与二次确认对话框
@Composable
fun DetailBottomActions(
    task: BuildTask,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    val roundedLayout = currentSheetRoundedLayout()
    val showRetry = task.status == BuildTaskStatus.FAILED
    val isActive = task.status == BuildTaskStatus.PENDING ||
            task.status == BuildTaskStatus.RUNNING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(
                start = roundedLayout.cardInset,
                top = TaskDetailSheetConfig.BOTTOM_ACTION_TOP_PADDING,
                end = roundedLayout.cardInset,
                bottom = roundedLayout.cardInset
            )
    ) {
        if (showRetry) {
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TaskDetailSheetConfig.BUTTON_HEIGHT)
            ) {
                Icon(
                    AppMaterialSymbols.refresh,
                    contentDescription = null,
                    modifier = Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_SIZE)
                )
                Spacer(Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_TEXT_SPACING))
                Text("重试")
            }
            Spacer(Modifier.height(TaskDetailSheetConfig.BOTTOM_BUTTON_SPACING))
        }

        if (isActive) {
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TaskDetailSheetConfig.BUTTON_HEIGHT)
            ) {
                Text("停止构建")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TaskDetailSheetConfig.BUTTON_HEIGHT),
                horizontalArrangement = Arrangement.spacedBy(TaskDetailSheetConfig.BOTTOM_BUTTON_SPACING)
            ) {
                FilledTonalButton(
                    onClick = { /* TODO: 预设页面实现后接入 */ },
                    enabled = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        AppMaterialSymbols.category,
                        contentDescription = null,
                        modifier = Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_SIZE)
                    )
                    Spacer(Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_TEXT_SPACING))
                    Text("保存到预设")
                }
                FilledTonalButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        AppMaterialSymbols.delete,
                        contentDescription = null,
                        modifier = Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_SIZE)
                    )
                    Spacer(Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_TEXT_SPACING))
                    Text("删除记录")
                }
            }
        }
    }
}

// 二次确认对话框
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmIsDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (confirmIsDestructive) {
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
