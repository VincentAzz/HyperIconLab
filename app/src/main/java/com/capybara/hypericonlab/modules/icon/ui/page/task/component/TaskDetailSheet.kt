package com.capybara.hypericonlab.modules.icon.ui.page.task.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.modules.icon.ui.page.task.sections.ConfirmDialog
import com.capybara.hypericonlab.modules.icon.ui.page.task.sections.DetailBottomActions
import com.capybara.hypericonlab.modules.icon.ui.page.task.sections.DetailContent
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTask
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTaskStatus
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.LayerBackdrop

// 任务详情 Sheet 主入口
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: BuildTask,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    horizontalPadding: Dp = 8.dp,
    bottomPadding: Dp = 4.dp,
    cornerRadius: Dp = ExtraLargeRadius,
    backdrop: LayerBackdrop? = null,
    useLiquidGlass: Boolean = false,
    liquidGlassBlurRadius: Dp = 24.dp,
) {
    val isActive = task.status == BuildTaskStatus.PENDING ||
            task.status == BuildTaskStatus.RUNNING
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var showStopDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRetryDialog by remember { mutableStateOf(false) }

    fun closeSheet() {
        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    FloatingBottomSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding,
        cornerRadius = cornerRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius,
        fillMaxHeight = true
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            CenterAlignedTopAppBar(
                title = { SheetTitle(if (isActive) "构建中" else "任务详情") },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    Surface(
                        onClick = { closeSheet() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .padding(start = TaskDetailSheetConfig.HEADER_ICON_LEADING_PADDING)
                            .size(TaskDetailSheetConfig.HEADER_ICON_SIZE)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                AppMaterialSymbols.close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(TaskDetailSheetConfig.HEADER_ICON_INNER_SIZE),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = { closeSheet() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .padding(end = TaskDetailSheetConfig.HEADER_ICON_TRAILING_PADDING)
                            .size(TaskDetailSheetConfig.HEADER_ICON_SIZE)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                AppMaterialSymbols.check,
                                contentDescription = "确认",
                                modifier = Modifier.size(TaskDetailSheetConfig.HEADER_ICON_INNER_SIZE),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                DetailContent(
                    task = task,
                    isActive = isActive
                )
            }
            DetailBottomActions(
                task = task,
                onStop = { showStopDialog = true },
                onDelete = { showDeleteDialog = true },
                onRetry = { showRetryDialog = true }
            )
        }
    }

    if (showStopDialog) {
        ConfirmDialog(
            title = "停止构建",
            message = "确定停止任务 ${task.taskId}？已生成的部分将被丢弃。",
            confirmLabel = "停止",
            confirmIsDestructive = true,
            onConfirm = {
                showStopDialog = false
                onStop()
                closeSheet()
            },
            onDismiss = { showStopDialog = false }
        )
    }
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "删除记录",
            message = "确定删除任务 ${task.taskId} 的记录？删除后无法恢复。",
            confirmLabel = "删除",
            confirmIsDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
                closeSheet()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
    if (showRetryDialog) {
        ConfirmDialog(
            title = "重试任务",
            message = "将基于原配置重新提交一个新任务，原失败记录会被移除。",
            confirmLabel = "重试",
            confirmIsDestructive = false,
            onConfirm = {
                showRetryDialog = false
                onRetry()
                closeSheet()
            },
            onDismiss = { showRetryDialog = false }
        )
    }
}

object TaskDetailSheetConfig {
    // 主体内容水平内边距
    val CONTENT_HORIZONTAL_PADDING = 16.dp

    // 主体内容垂直内边距
    val CONTENT_VERTICAL_PADDING = 8.dp

    // section 间距
    val SECTION_SPACING = 12.dp

    // 内容区底部留白：避免最后一个 item 紧贴底部按钮组
    val CONTENT_BOTTOM_SPACING = 8.dp

    // 预览图高度（保持 store preview 1080×640 的 5:3 长宽比近似）
    val PREVIEW_HEIGHT = 200.dp

    // 卡片容器色透明度（与 LogSheet 一致，让 sheet 模糊透出）
    const val CARD_ALPHA = 0.8f

    // 进度卡片内容内边距
    val PROGRESS_CONTENT_PADDING = PaddingValues(16.dp)

    // 进度条上方间距
    val PROGRESS_BAR_TOP_SPACING = 8.dp

    // 进度条百分比文本上方间距
    val PROGRESS_TEXT_TOP_SPACING = 4.dp

    // 进度条高度
    val PROGRESS_HEIGHT = 4.dp

    // 错误信息卡片内容内边距
    val ERROR_CONTENT_PADDING = PaddingValues(16.dp)

    // 底部操作区按钮组内边距
    val BOTTOM_ACTION_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    // 底部按钮间距
    val BOTTOM_BUTTON_SPACING = 12.dp

    // 底部按钮高度（Material3 Button 默认 36dp，此处显式指定避免被压缩）
    val BUTTON_HEIGHT = 48.dp

    // 按钮内图标尺寸
    val BUTTON_ICON_SIZE = 18.dp

    // 按钮内图标与文本间距
    val BUTTON_ICON_TEXT_SPACING = 8.dp

    // Header 圆形按钮容器尺寸
    val HEADER_ICON_SIZE = 40.dp

    // Header 圆形按钮内部图标尺寸
    val HEADER_ICON_INNER_SIZE = 24.dp

    // Header 关闭按钮左侧 padding
    val HEADER_ICON_LEADING_PADDING = 12.dp

    // Header 确认按钮右侧 padding
    val HEADER_ICON_TRAILING_PADDING = 12.dp
}
