package com.capybara.hypericonlab.modules.icon.ui.page.task

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capybara.hypericonlab.core.designsystem.theme.CardCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.PreviewCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 任务卡片：展示单个 [BuildTask] 的关键信息，点击进入详情 sheet。
 *
 * 布局（仅点击交互，不做 SwipeToDismiss，避免与底栏 tab 滑动冲突）：
 * ```
 * ┌──────────────────────────────────────────────────┐
 * │ [缩略图1] [缩略图2]  taskId (GoogleSansCode)      │
 * │                     图标集 · 图标数                │
 * │                     [产物类型 chip] [状态 chip]    │
 * │ ─────────────────────────────────────────────    │
 * │ LinearProgressIndicator (仅 PENDING/RUNNING)      │
 * │ 状态文案 · 提交时间                                │
 * └──────────────────────────────────────────────────┘
 * ```
 *
 * 缩略图策略：
 * - PENDING：展示 store preview 缓存（由 BuildTaskManager.previewCache 提供，但 UI 层不直接访问，
 *   此处用占位色块）
 * - RUNNING：同 PENDING
 * - SUCCESS：从 filesDir/build_thumbnails/<taskId>.png 异步加载（单缩略图，按需复制展示两次以保持布局）
 * - FAILED：占位色块（错误图标）
 *
 * @param task 当前任务
 * @param onClick 点击卡片回调（用于打开详情 sheet）
 */
@Composable
fun TaskCard(
    task: BuildTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 缩略图位图：SUCCESS 状态异步加载，其他状态为 null
    var thumbnail by remember(task.taskId, task.status) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(task.taskId, task.status) {
        if (task.status == BuildTaskStatus.SUCCESS) {
            thumbnail = withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "build_thumbnails/${task.taskId}.png")
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = rememberKyantRoundedRectangleShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TaskCardConfig.CONTENT_PADDING)
        ) {
            // 顶部：2 个缩略图 + 任务信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2 个缩略图位（SUCCESS 时显示位图，其他状态显示占位色块）
                ThumbnailSlot(thumbnail, task.status, Modifier.size(TaskCardConfig.THUMBNAIL_SIZE))
                Spacer(Modifier.width(0.dp))
                ThumbnailSlot(thumbnail, task.status, Modifier.size(TaskCardConfig.THUMBNAIL_SIZE))

                Spacer(Modifier.weight(1f))

                // 任务 ID 与图标集信息（右对齐）
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = task.taskId,
                        fontFamily = GoogleSansCodeFontFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${task.iconSetLabel} · ${task.iconCount} 个图标",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // chips：产物类型 + 状态
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusChip(text = task.productType.label)
                StatusChip(
                    text = statusLabel(task.status),
                    containerColor = statusColor(task.status)
                )
                if (task.status == BuildTaskStatus.RUNNING && task.currentPackage != null) {
                    StatusChip(
                        text = task.currentPackage.take(TaskCardConfig.MAX_PACKAGE_NAME_LENGTH)
                    )
                }
            }

            // 进度条：仅 PENDING/RUNNING 显示
            if (task.status == BuildTaskStatus.PENDING ||
                task.status == BuildTaskStatus.RUNNING
            ) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TaskCardConfig.PROGRESS_HEIGHT),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            Spacer(Modifier.height(8.dp))

            // 底部：状态文案 + 提交时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusDescription(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatTime(task.submittedAt),
                    fontFamily = GoogleSansCodeFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 缩略图位：SUCCESS 时显示位图，其他状态显示占位色块
@Composable
private fun ThumbnailSlot(
    thumbnail: android.graphics.Bitmap?,
    status: BuildTaskStatus,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        BuildTaskStatus.PENDING, BuildTaskStatus.RUNNING ->
            MaterialTheme.colorScheme.surfaceContainerHighest

        BuildTaskStatus.SUCCESS -> MaterialTheme.colorScheme.surfaceContainerHighest
        BuildTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        BuildTaskStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = modifier
            .clip(rememberKyantRoundedRectangleShape(PreviewCornerRadius))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// 状态 chip（紧凑色块 + 文本）
@Composable
private fun StatusChip(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// 状态文案映射
private fun statusLabel(status: BuildTaskStatus): String = when (status) {
    BuildTaskStatus.PENDING -> "等待中"
    BuildTaskStatus.RUNNING -> "构建中"
    BuildTaskStatus.SUCCESS -> "已完成"
    BuildTaskStatus.FAILED -> "失败"
    BuildTaskStatus.CANCELLED -> "已取消"
}

// 状态颜色映射
private fun statusColor(status: BuildTaskStatus): Color = when (status) {
    BuildTaskStatus.PENDING -> Color(0xFF9E9E9E) // 灰色
    BuildTaskStatus.RUNNING -> Color(0xFF2196F3) // 蓝色
    BuildTaskStatus.SUCCESS -> Color(0xFF4CAF50) // 绿色
    BuildTaskStatus.FAILED -> Color(0xFFF44336)  // 红色
    BuildTaskStatus.CANCELLED -> Color(0xFF9E9E9E)
}

// 状态描述文案
private fun statusDescription(task: BuildTask): String = when (task.status) {
    BuildTaskStatus.PENDING -> "等待执行"
    BuildTaskStatus.RUNNING -> if (task.currentPackage != null) "正在处理：${task.currentPackage}" else "准备中"
    BuildTaskStatus.SUCCESS -> "耗时 ${(task.durationMs ?: 0L) / 1000.0} 秒"
    BuildTaskStatus.FAILED -> "错误：${task.errorMessage ?: "未知"}"
    BuildTaskStatus.CANCELLED -> "已取消"
}

// 时间格式化：MMdd_HHmm
private fun formatTime(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

// 任务卡片关键参数集中声明，便于调参
private object TaskCardConfig {
    // 卡片内容内边距
    val CONTENT_PADDING = PaddingValues(16.dp)

    // 缩略图尺寸（正方形）
    val THUMBNAIL_SIZE = 48.dp

    // 进度条高度
    val PROGRESS_HEIGHT = 4.dp

    // 包名最大长度（避免过长 chip）
    const val MAX_PACKAGE_NAME_LENGTH = 20
}
