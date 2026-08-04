package com.capybara.hypericonlab.modules.icon.ui.page.home.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.LocalSegmentedItemShape
import com.capybara.hypericonlab.core.designsystem.component.PrimaryActionButton
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.symbol.check_circle
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskStatus
import kotlin.math.roundToInt

private object InitializationCardDefaults {
    val CardPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    val GroupPadding = PaddingValues(bottom = 8.dp)
    val HeaderGap = 8.dp
    val ProgressHeight = 4.dp
    val ProgressDotSize = 4.dp
}

@Composable
fun InitializationCard(
    state: InitializationState,
    onStart: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasFailure = remember(state.tasks) {
        state.tasks.any { it.status == InitializationTaskStatus.FAILED }
    }
    val hasStarted = remember(state.tasks) {
        state.tasks.any { it.status != InitializationTaskStatus.PENDING }
    }
    val progressTask = state.tasks.firstOrNull { it.task == state.activeTask }
    val progress = progressTask?.progress?.coerceIn(0f, 1f) ?: 0f

    val isRunning = state.activeTask != null && hasStarted && !hasFailure && !state.isCompleted
    val isAssetUpdate = state.resourceVersion != null || state.templateVersion != null

    SegmentedColumn(
        modifier = modifier.fillMaxWidth(),
        // title = "初始化",
        contentPadding = InitializationCardDefaults.GroupPadding
    ) {
        item(key = "header") {
            InitializationHeader(
                state = state,
                hasFailure = hasFailure,
                progress = progress,
                isRunning = isRunning,
                isAssetUpdate = isAssetUpdate,
                onStart = onStart,
                onRetry = onRetry
            )
        }

        item(key = "taskList") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = LocalSegmentedItemShape.current,
                color = MaterialTheme.colorScheme.surfaceBright
            ) {
                Column {
                    state.tasks.forEach { taskState ->
                        InitializationTaskRow(
                            taskState = taskState,
                            onRetry = onRetry
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitializationHeader(
    state: InitializationState,
    hasFailure: Boolean,
    progress: Float,
    isRunning: Boolean,
    isAssetUpdate: Boolean,
    onStart: () -> Unit,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = LocalSegmentedItemShape.current,
        color = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(modifier = Modifier.padding(InitializationCardDefaults.CardPadding)) {
            when {
                state.isCompleted -> {
                    val completedCount = state.tasks.count {
                        it.status == InitializationTaskStatus.COMPLETED
                    }
                    SummaryHeaderContent(
                        title = "初始化完成 ($completedCount/${state.tasks.size})",
                        trailingIcon = AppMaterialSymbols.check_circle
                    )
                }

                hasFailure -> {
                    SummaryHeaderContent(
                        title = "初始化未完成",
                        actionText = "重试",
                        onAction = onRetry
                    )
                }

                isRunning -> {
                    RunningHeaderContent(
                        state = state,
                        progress = progress
                    )
                }

                isAssetUpdate -> {
                    SummaryHeaderContent(
                        title = "资产更新",
                        actionText = "更新",
                        onAction = onStart
                    )
                }

                else -> {
                    SummaryHeaderContent(
                        title = "重新初始化",
                        actionText = "开始",
                        onAction = onStart
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryHeaderContent(
    title: String,
    trailingIcon: ImageVector? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(InitializationCardDefaults.HeaderGap)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null) {
            PrimaryActionButton(
                text = actionText,
                onClick = onAction ?: {}
            )
        } else if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RunningHeaderContent(
    state: InitializationState,
    progress: Float
) {
    val task = state.activeTask
    val runningTaskTitle =
        state.tasks.firstOrNull { it.status == InitializationTaskStatus.RUNNING }?.task?.let {
            when (it) {
                InitializationTask.LAWNICONS -> "从仓库拉取 Lawnicons 资源"
                InitializationTask.APK_TEMPLATE -> "从仓库拉取图标包 APK 模板"
                InitializationTask.APP_M3_CACHE -> "生成颜色映射缓存"
            }
        } ?: "正在初始化..."

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(InitializationCardDefaults.HeaderGap)
        ) {
            Text(
                text = runningTaskTitle,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(InitializationCardDefaults.HeaderGap))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(InitializationCardDefaults.ProgressHeight)
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(InitializationCardDefaults.ProgressDotSize)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
