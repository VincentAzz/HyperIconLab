package com.capybara.hypericonlab.modules.icon.ui.page.task.component

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

    var thumbnail by remember(task.taskId, task.status) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }
    LaunchedEffect(task.taskId, task.status) {
        thumbnail = withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "build_thumbnails/${task.taskId}.png")
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThumbnailSlot(
                    thumbnail = thumbnail,
                    status = task.status,
                    modifier = Modifier
                        .height(TaskCardConfig.THUMBNAIL_HEIGHT)
                        .width(TaskCardConfig.THUMBNAIL_WIDTH)
                )

                Spacer(Modifier.weight(1f))

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
                contentScale = ContentScale.Fit
            )
        }
    }
}

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


private fun statusLabel(status: BuildTaskStatus): String = when (status) {
    BuildTaskStatus.PENDING -> "等待中"
    BuildTaskStatus.RUNNING -> "构建中"
    BuildTaskStatus.SUCCESS -> "已完成"
    BuildTaskStatus.FAILED -> "失败"
    BuildTaskStatus.CANCELLED -> "已取消"
}


private fun statusColor(status: BuildTaskStatus): Color = when (status) {
    BuildTaskStatus.PENDING -> Color(0xFF9E9E9E)
    BuildTaskStatus.RUNNING -> Color(0xFF2196F3)
    BuildTaskStatus.SUCCESS -> Color(0xFF4CAF50)
    BuildTaskStatus.FAILED -> Color(0xFFF44336)
    BuildTaskStatus.CANCELLED -> Color(0xFF9E9E9E)
}

private fun statusDescription(task: BuildTask): String = when (task.status) {
    BuildTaskStatus.PENDING -> "等待执行"
    BuildTaskStatus.RUNNING -> if (task.currentPackage != null) "正在处理：${task.currentPackage}" else "准备中"
    BuildTaskStatus.SUCCESS -> "耗时 ${(task.durationMs ?: 0L) / 1000.0} 秒"
    BuildTaskStatus.FAILED -> "错误：${task.errorMessage ?: "未知"}"
    BuildTaskStatus.CANCELLED -> "已取消"
}


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
