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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.FloatingBottomSheet
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.SheetTitle
import com.capybara.hypericonlab.core.designsystem.symbol.category
import com.capybara.hypericonlab.core.designsystem.symbol.check
import com.capybara.hypericonlab.core.designsystem.symbol.close
import com.capybara.hypericonlab.core.designsystem.symbol.delete
import com.capybara.hypericonlab.core.designsystem.symbol.refresh
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.ExtraLargeRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.PreviewCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ConfigCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import java.io.File

/**
 * 任务详情 Sheet：根据任务状态自适应布局。
 *
 * - **进行中**（PENDING / RUNNING）：[FloatingBottomSheet.fillMaxHeight] = true，
 *   全屏布局，Header + LazyColumn(weight 1f, 可滚动) + 底部按钮区（独占高度，不被挤占）
 * - **已完成**（SUCCESS / FAILED / CANCELLED）：自适应高度，Header + LazyColumn（自适应）+ 底部按钮区
 *
 * 布局关键点：
 * - 整体用 Column 包裹 Header / 内容 / 按钮，按钮固定在 Column 末尾独占高度
 * - LazyColumn 使用 weight(1f) 让按钮高度始终不被压缩
 * - 卡片容器色使用 `surfaceBright.copy(alpha = 0.8f)`，与 LogSheet 风格一致，能透出 sheet 模糊
 *
 * 风格对齐 MaskPickerSheet / LogSheet：默认 dragHandle（可下滑关闭）、对称 40dp 圆形按钮保持 title 居中、
 * backdrop/useLiquidGlass 由调用方透传跟随应用设置。
 *
 * 危险操作（停止/删除/重试）均配 [AlertDialog] 二次确认。
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
        // 使用默认 dragHandle（与 LogSheet 一致），允许用户从顶部 dragHandle 拖动关闭
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


@Composable
private fun DetailContent(
    task: BuildTask,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // 预览图位图：提交时已持久化，所有状态均可加载 filesDir/build_previews/<taskId>.png
    var previewBitmap by remember(task.taskId, task.status) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(task.taskId, task.status) {
        // 任务提交即持久化预览图，统一异步加载
        previewBitmap = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "build_previews/${task.taskId}.png")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    val cardContainerColor =
        MaterialTheme.colorScheme.surfaceBright.copy(alpha = TaskDetailSheetConfig.CARD_ALPHA)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            horizontal = TaskDetailSheetConfig.CONTENT_HORIZONTAL_PADDING,
            vertical = TaskDetailSheetConfig.CONTENT_VERTICAL_PADDING
        ),
        verticalArrangement = Arrangement.spacedBy(TaskDetailSheetConfig.SECTION_SPACING)
    ) {
        item { PreviewSection(task = task, bitmap = previewBitmap) }

        // 任务信息（含导出位置）：使用 SegmentedColumn 分段展示，与设置页风格一致
        item {
            TaskInfoSection(
                task = task,
                containerColorAlpha = TaskDetailSheetConfig.CARD_ALPHA
            )
        }

        if (isActive) {
            item {
                ProgressCard(task = task, containerColor = cardContainerColor)
            }
        }

        if (task.status == BuildTaskStatus.FAILED) {
            item {
                ErrorCard(
                    errorMessage = task.errorMessage,
                    containerColor = cardContainerColor
                )
            }
        }

        item {
            Spacer(Modifier.height(TaskDetailSheetConfig.CONTENT_BOTTOM_SPACING))
        }
    }
}

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

/**
 * 任务信息分段卡片：合并原任务信息与导出位置，使用 SegmentedColumn 分段展示。
 * 导出位置仅在 SUCCESS 状态显示，路径过长时换行而非截断。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TaskInfoSection(
    task: BuildTask,
    containerColorAlpha: Float
) {
    SegmentedColumn(
        title = "详情",
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
        containerColorAlpha = containerColorAlpha
    ) {
        item {
            BaseWidget(
                iconPlaceholder = false,
                title = "ID",
                trailingContent = {
                    Text(
                        text = task.taskId,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = GoogleSansCodeFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        item(topPadding = ListItemDefaults.SegmentedGap) {
            BaseWidget(
                iconPlaceholder = false,
                title = "产物类型",
                trailingContent = {
                    Text(
                        text = task.productType.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        item(topPadding = ListItemDefaults.SegmentedGap) {
            BaseWidget(
                iconPlaceholder = false,
                title = "图标集",
                trailingContent = {
                    Text(
                        text = "${task.iconSetLabel} · ${task.iconCount} 个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        item(topPadding = ListItemDefaults.SegmentedGap) {
            BaseWidget(
                iconPlaceholder = false,
                title = "前景",
                trailingContent = {
                    Text(
                        text = "${fgStyleLabel(task.configSnapshot.fgStyle)} · ${
                            colorSourceLabel(
                                task.configSnapshot.fgColorSource
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        item(topPadding = ListItemDefaults.SegmentedGap) {
            BaseWidget(
                iconPlaceholder = false,
                title = "背景",
                trailingContent = {
                    Text(
                        text = "${bgStyleLabel(task.configSnapshot.bgStyle)} · ${
                            colorSourceLabel(
                                task.configSnapshot.bgColorSource
                            )
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
        if (task.configSnapshot.dualLayerEnabled) {
            item(topPadding = ListItemDefaults.SegmentedGap) {
                BaseWidget(
                    iconPlaceholder = false,
                    title = "下层背景",
                    trailingContent = {
                        Text(
                            text = "${bgStyleLabel(task.configSnapshot.bgLayer2.style)} · ${
                                colorSourceLabel(
                                    task.configSnapshot.bgLayer2.colorSource
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        // 导出位置：仅 SUCCESS 显示，路径换行展示不截断
        if (task.status == BuildTaskStatus.SUCCESS) {
            item(topPadding = ListItemDefaults.SegmentedGap) {
                BaseWidget(
                    iconPlaceholder = false,
                    title = "导出位置",
                    description = task.artifactPath
                        ?: "Documents/HyperIconLabArtifacts/${task.taskId}",
                    descriptionStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = GoogleSansCodeFontFamily
                    )
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(
    task: BuildTask,
    containerColor: Color
) {
    ConfigCard(
        title = "进度",
        containerColor = containerColor
    ) {
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

@Composable
private fun ErrorCard(
    errorMessage: String?,
    containerColor: Color
) {
    ConfigCard(
        title = "错误信息",
        containerColor = containerColor
    ) {
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

// 底部操作按钮区：独占高度，wrapContentHeight 不被 LazyColumn 挤压
@Composable
private fun DetailBottomActions(
    task: BuildTask,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    val showRetry = task.status == BuildTaskStatus.FAILED
    val isActive = task.status == BuildTaskStatus.PENDING ||
            task.status == BuildTaskStatus.RUNNING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(TaskDetailSheetConfig.BOTTOM_ACTION_PADDING)
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
                // 保存到预设：暂未实现，置 disabled
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


// 预览图占位文案、前景/背景/颜色来源标签、布局常量已外迁至：
// - TaskDetailLabels.kt（标签函数，public 供复用）
// - TaskDetailSheetConfig.kt（布局与尺寸常量）
