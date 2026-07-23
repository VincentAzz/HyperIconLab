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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.capybara.hypericonlab.core.designsystem.theme.ConnectionRadius
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.core.designsystem.theme.PreviewCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.isSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.core.designsystem.theme.kyantUnevenRoundedShape
import com.capybara.hypericonlab.core.designsystem.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun TaskCard(
    task: BuildTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val context = LocalContext.current

    // 缩略图位图：提交时已由 BuildTaskManager 持久化，所有状态均可加载
    var thumbnail by remember(task.taskId, task.status) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(task.taskId, task.status) {
        // 任务提交即持久化缩略图，此处统一异步加载
        thumbnail = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "build_thumbnails/${task.taskId}.png")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

    // 根据位置计算圆角：首项顶部大圆角、末项底部大圆角、中间项连接小圆角
    val smootherEnabled = isSmootherRoundedCornersEnabled()
    val cardShape = remember(isFirst, isLast, smootherEnabled) {
        val topRadius = if (isFirst) CornerRadius else ConnectionRadius
        val bottomRadius = if (isLast) CornerRadius else ConnectionRadius
        kyantUnevenRoundedShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomEnd = bottomRadius,
            bottomStart = bottomRadius,
            enabled = smootherEnabled
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceBright)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TaskCardConfig.CONTENT_PADDING)
        ) {
            // 顶部：单张缩略图（长方形，540:320 比例）+ 任务信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 单缩略图位（长方形，提交时已持久化，所有状态均显示位图或占位色块）
                ThumbnailSlot(
                    thumbnail = thumbnail,
                    status = task.status,
                    modifier = Modifier
                        .height(TaskCardConfig.THUMBNAIL_HEIGHT)
                        .width(TaskCardConfig.THUMBNAIL_WIDTH)
                )

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

            // chips：产物类型 + 状态（不再展示当前处理包名，意义不大）
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatusChip(text = task.productType.label)
                StatusChip(
                    text = statusLabel(task.status),
                    containerColor = statusColor(task.status)
                )
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

// 缩略图位：长方形（540:320 比例），Fit 显示避免裁切
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
                modifier = Modifier.fillMaxSize(),
                // Fit：按比例缩放完整显示，不裁切；图比例 540:320 = 1.6875:1，与 slot 1.5:1 不完全匹配但保证不裁切
                contentScale = ContentScale.Fit
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

    // 缩略图高度（长方形 slot，对应裁切的 540×320 缩略图）
    val THUMBNAIL_HEIGHT = 48.dp

    // 缩略图宽度（按 540:320 = 1.6875:1 比例，48 * 1.6875 ≈ 81dp）
    val THUMBNAIL_WIDTH = 81.dp

    // 进度条高度
    val PROGRESS_HEIGHT = 4.dp
}
