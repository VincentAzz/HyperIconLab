package com.capybara.hypericonlab.modules.icon.ui.page.task

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.refresh
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.PreviewCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import java.io.File

/**
 * 任务详情 Sheet：根据任务状态自适应布局。
 *
 * - **进行中**（PENDING / RUNNING）：[FloatingBottomSheet.fillMaxHeight] = true，
 *   全屏布局，包含预览图、信息 chips、进度条、底部"停止构建"按钮
 * - **已完成**（SUCCESS / FAILED / CANCELLED）：自适应高度，包含预览图、信息、导出位置/错误信息、
 *   底部"删除记录"按钮；FAILED 任务额外显示"重试"按钮
 *
 * 危险操作（停止/删除/重试）均配 [AlertDialog] 二次确认。
 *
 * @param task 当前任务
 * @param onStop 进行中任务"停止构建"回调
 * @param onDelete 已完成任务"删除记录"回调
 * @param onRetry 失败任务"重试"回调
 * @param onDismiss 关闭 sheet 回调
 */
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
    // 进行中任务全屏，已完成任务自适应高度
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // 二次确认对话框状态
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
        dragHandle = null,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding,
        cornerRadius = cornerRadius,
        backdrop = backdrop,
        useLiquidGlass = useLiquidGlass,
        liquidGlassBlurRadius = liquidGlassBlurRadius,
        fillMaxHeight = isActive
    ) {
        // Header：与 BuildOptionSheet 风格一致
        CenterAlignedTopAppBar(
            title = { SheetTitle(if (isActive) "构建中" else "任务详情") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            ),
            navigationIcon = {
                Surface(
                    onClick = { closeSheet() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(start = 12.dp)
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
            }
        )

        // 内容主体：进行中 LazyColumn 占满剩余空间（weight(1f)），已完成 wrapContentHeight
        // 注意：weight 是 ColumnScope 扩展，需在 if 分支内显式调用以正确解析 receiver
        val contentModifier = if (isActive) {
            Modifier
                .fillMaxWidth()
                .weight(1f)
        } else {
            Modifier.fillMaxWidth()
        }
        DetailContent(
            task = task,
            isActive = isActive,
            modifier = contentModifier
        )

        // 底部操作区：进行中固定在底部，已完成跟在内容之后
        DetailBottomActions(
            task = task,
            onStop = { showStopDialog = true },
            onDelete = { showDeleteDialog = true },
            onRetry = { showRetryDialog = true }
        )
    }

    // 二次确认对话框
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

// 详情主体内容：预览图 + 任务信息 chips + 状态相关区
@Composable
private fun DetailContent(
    task: BuildTask,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 预览图位图：SUCCESS 时异步加载 filesDir/build_previews/<taskId>.png
    var previewBitmap by remember(task.taskId, task.status) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(task.taskId, task.status) {
        if (task.status == BuildTaskStatus.SUCCESS) {
            previewBitmap = withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "build_previews/${task.taskId}.png")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .then(if (isActive) Modifier.fillMaxSize() else Modifier.wrapContentHeight()),
        contentPadding = PaddingValues(
            horizontal = TaskDetailSheetConfig.CONTENT_HORIZONTAL_PADDING,
            vertical = TaskDetailSheetConfig.CONTENT_VERTICAL_PADDING
        ),
        verticalArrangement = Arrangement.spacedBy(TaskDetailSheetConfig.SECTION_SPACING)
    ) {
        // 预览图区
        item { PreviewSection(task = task, bitmap = previewBitmap) }

        // 任务信息 chips
        item { TaskInfoChips(task = task) }

        // 进度区（仅 PENDING/RUNNING）
        if (isActive) {
            item { ProgressSection(task = task) }
        }

        // 状态相关区
        when (task.status) {
            BuildTaskStatus.SUCCESS -> {
                item {
                    ExportPathSection(
                        taskId = task.taskId,
                        artifactPath = task.artifactPath
                    )
                }
            }

            BuildTaskStatus.FAILED -> {
                item { ErrorSection(errorMessage = task.errorMessage) }
            }

            else -> {}
        }
    }
}

// 预览图区：成功时展示位图，其他状态展示占位
@Composable
private fun PreviewSection(
    task: BuildTask,
    bitmap: android.graphics.Bitmap?
) {
    val bgColor = when (task.status) {
        BuildTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskDetailSheetConfig.PREVIEW_HEIGHT)
            .clip(rememberKyantRoundedRectangleShape(PreviewCornerRadius))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "任务预览图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = previewPlaceholder(task.status),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 任务信息 chips：产物类型、图标集、前景/背景样式、双层状态
@Composable
private fun TaskInfoChips(task: BuildTask) {
    SegmentedColumn(title = "任务信息") {
        item {
            InfoChipRow(
                label = "任务 ID",
                value = task.taskId,
                useCodeFont = true
            )
        }
        item {
            InfoChipRow(
                label = "产物类型",
                value = task.productType.label
            )
        }
        item {
            InfoChipRow(
                label = "图标集",
                value = "${task.iconSetLabel} · ${task.iconCount} 个"
            )
        }
        item {
            InfoChipRow(
                label = "前景",
                value = "${fgStyleLabel(task.configSnapshot.fgStyle)} · ${colorSourceLabel(task.configSnapshot.fgColorSource)}"
            )
        }
        item {
            InfoChipRow(
                label = "背景",
                value = "${bgStyleLabel(task.configSnapshot.bgStyle)} · ${colorSourceLabel(task.configSnapshot.bgColorSource)}"
            )
        }
        if (task.configSnapshot.dualLayerEnabled) {
            item {
                InfoChipRow(
                    label = "下层背景",
                    value = "${bgStyleLabel(task.configSnapshot.bgLayer2.style)} · ${
                        colorSourceLabel(
                            task.configSnapshot.bgLayer2.colorSource
                        )
                    }"
                )
            }
        }
    }
}

// 进度区：进度条 + 当前处理包名
@Composable
private fun ProgressSection(task: BuildTask) {
    SegmentedColumn(title = "进度") {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TaskDetailSheetConfig.PROGRESS_CONTENT_PADDING)
            ) {
                Text(
                    text = if (task.currentPackage != null) {
                        "正在处理：${task.currentPackage}"
                    } else if (task.status == BuildTaskStatus.RUNNING) {
                        "准备中..."
                    } else {
                        "排队等待中..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(TaskDetailSheetConfig.PROGRESS_BAR_TOP_SPACING))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TaskDetailSheetConfig.PROGRESS_HEIGHT),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
                Spacer(Modifier.height(TaskDetailSheetConfig.PROGRESS_TEXT_TOP_SPACING))
                Text(
                    text = "${(task.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = GoogleSansCodeFontFamily
                )
            }
        }
    }
}

// 导出位置区
@Composable
private fun ExportPathSection(taskId: String, artifactPath: String?) {
    SegmentedColumn(title = "导出位置") {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TaskDetailSheetConfig.EXPORT_CONTENT_PADDING)
            ) {
                Text(
                    text = "文件已保存到：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(TaskDetailSheetConfig.EXPORT_PATH_TOP_SPACING))
                Text(
                    text = artifactPath ?: "Documents/HyperIconLabArtifacts/$taskId",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = GoogleSansCodeFontFamily
                )
            }
        }
    }
}

// 错误信息区
@Composable
private fun ErrorSection(errorMessage: String?) {
    SegmentedColumn(title = "错误信息") {
        item {
            Text(
                text = errorMessage ?: "未知错误",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TaskDetailSheetConfig.ERROR_CONTENT_PADDING)
            )
        }
    }
}

// 底部操作按钮区
@Composable
private fun DetailBottomActions(
    task: BuildTask,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    val showRetry = task.status == BuildTaskStatus.FAILED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TaskDetailSheetConfig.BOTTOM_ACTION_PADDING),
        horizontalArrangement = Arrangement.spacedBy(TaskDetailSheetConfig.BOTTOM_BUTTON_SPACING)
    ) {
        // 失败任务：重试按钮（普通色，左侧 weight=1）
        if (showRetry) {
            FilledTonalButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    AppMaterialSymbols.refresh,
                    contentDescription = null,
                    modifier = Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_SIZE)
                )
                Spacer(Modifier.size(TaskDetailSheetConfig.BUTTON_ICON_TEXT_SPACING))
                Text("重试")
            }
        }

        // 进行中：停止按钮（红色强调）；已完成：删除按钮（普通色）
        if (task.status == BuildTaskStatus.PENDING ||
            task.status == BuildTaskStatus.RUNNING
        ) {
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("停止构建")
            }
        } else {
            FilledTonalButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            ) {
                Text("删除记录")
            }
        }
    }
}

// 简单信息行：label + value
@Composable
private fun InfoChipRow(
    label: String,
    value: String,
    useCodeFont: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TaskDetailSheetConfig.INFO_ROW_PADDING),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontFamily = if (useCodeFont) GoogleSansCodeFontFamily else null,
            maxLines = 1
        )
    }
}

// 二次确认对话框
@Composable
private fun ConfirmDialog(
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

// === 文案映射工具函数 ===

// 预览图占位文案
private fun previewPlaceholder(status: BuildTaskStatus): String = when (status) {
    BuildTaskStatus.PENDING -> "等待开始..."
    BuildTaskStatus.RUNNING -> "构建中，预览图将在完成后显示"
    BuildTaskStatus.FAILED -> "构建失败"
    BuildTaskStatus.CANCELLED -> "已取消"
    BuildTaskStatus.SUCCESS -> "预览图加载中..."
}

// 前景样式标签
private fun fgStyleLabel(style: String): String = when (style) {
    "line" -> "线条"
    "sticker" -> "贴纸"
    "glass" -> "玻璃"
    "hollow" -> "镂空"
    else -> style
}

// 背景样式标签
private fun bgStyleLabel(style: String): String = when (style) {
    "none" -> "无背景"
    "solid" -> "纯色"
    "img_static" -> "静态图片"
    "img_filling" -> "图片填充"
    else -> style
}

// 颜色来源标签
private fun colorSourceLabel(source: String): String = when (source) {
    "wallpaper" -> "壁纸"
    "app" -> "应用"
    "preset" -> "预设"
    "ctc" -> "同色系"
    "custom" -> "自定义"
    "black_white" -> "黑白"
    else -> source
}

// 任务详情 sheet 关键参数集中声明，便于调参
private object TaskDetailSheetConfig {
    // 主体内容水平内边距
    val CONTENT_HORIZONTAL_PADDING = 16.dp

    // 主体内容垂直内边距
    val CONTENT_VERTICAL_PADDING = 8.dp

    // section 间距
    val SECTION_SPACING = 12.dp

    // 预览图高度（保持 store preview 1080×640 的 5:3 长宽比近似）
    val PREVIEW_HEIGHT = 200.dp

    // 进度区内容内边距
    val PROGRESS_CONTENT_PADDING = PaddingValues(16.dp)

    // 进度条上方间距
    val PROGRESS_BAR_TOP_SPACING = 8.dp

    // 进度条百分比文本上方间距
    val PROGRESS_TEXT_TOP_SPACING = 4.dp

    // 进度条高度
    val PROGRESS_HEIGHT = 4.dp

    // 导出位置区内容内边距
    val EXPORT_CONTENT_PADDING = PaddingValues(16.dp)

    // 导出路径文本上方间距
    val EXPORT_PATH_TOP_SPACING = 4.dp

    // 错误信息区内容内边距
    val ERROR_CONTENT_PADDING = PaddingValues(16.dp)

    // 信息行内边距
    val INFO_ROW_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    // 底部操作区按钮组内边距
    val BOTTOM_ACTION_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

    // 底部按钮间距
    val BOTTOM_BUTTON_SPACING = 12.dp

    // 按钮内图标尺寸
    val BUTTON_ICON_SIZE = 18.dp

    // 按钮内图标与文本间距
    val BUTTON_ICON_TEXT_SPACING = 8.dp

    // Header 圆形按钮容器尺寸
    val HEADER_ICON_SIZE = 40.dp

    // Header 圆形按钮内部图标尺寸
    val HEADER_ICON_INNER_SIZE = 24.dp
}
