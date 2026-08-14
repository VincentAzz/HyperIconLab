package com.capybara.hypericonlab.modules.icon.ui.page.task.sections

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.BaseWidget
import com.capybara.hypericonlab.core.designsystem.component.SegmentedColumn
import com.capybara.hypericonlab.core.designsystem.component.currentSegmentedColumnOuterCornerRadius
import com.capybara.hypericonlab.core.designsystem.config.PreviewCornerInset
import com.capybara.hypericonlab.core.designsystem.config.insetCornerRadius
import com.capybara.hypericonlab.core.designsystem.config.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.core.designsystem.font.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.modules.build.domain.model.BuildTask
import com.capybara.hypericonlab.modules.build.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.ConfigCard
import com.capybara.hypericonlab.modules.icon.ui.page.task.component.TaskDetailSheetConfig

// 任务详情各分段卡片
@Composable
fun PreviewSection(
    task: BuildTask,
    bitmap: Bitmap?
) {
    val bgColor = when (task.status) {
        BuildTaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TaskDetailSheetConfig.PREVIEW_HEIGHT)
            .clip(
                rememberKyantRoundedRectangleShape(
                    insetCornerRadius(
                        currentSegmentedColumnOuterCornerRadius(),
                        PreviewCornerInset
                    )
                )
            )
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

// 任务信息分段卡片
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskInfoSection(
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
                        text = "${task.iconSetLabel} · ${task.iconCount} 映射",
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
fun ProgressCard(
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
fun ErrorCard(
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
