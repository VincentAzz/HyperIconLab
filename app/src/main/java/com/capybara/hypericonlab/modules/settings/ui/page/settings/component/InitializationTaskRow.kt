package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.core.designsystem.component.LocalSegmentedItemShape
import com.capybara.hypericonlab.core.designsystem.symbol.arrow_right_alt
import com.capybara.hypericonlab.core.designsystem.symbol.check_circle
import com.capybara.hypericonlab.core.designsystem.symbol.circle
import com.capybara.hypericonlab.core.designsystem.symbol.error
import com.capybara.hypericonlab.core.designsystem.theme.AppMaterialSymbols
import com.capybara.hypericonlab.core.designsystem.theme.GoogleSansCodeFontFamily
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskStatus

object InitializationTaskRowDefaults {
    val IconSize = 24.dp
    val IndicatorSize = 18.dp
    val HorizontalPadding = 16.dp
    val VerticalPadding = 8.dp
    val IconToTextGap = 16.dp
    val TextLineGap = 2.dp

    // val IndicatorTopOffset = 0.dp
    // val IconTopOffset = 0.dp
}


@Composable
private fun InitializationTaskItem(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    isRunning: Boolean = false,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    shape: Shape = LocalSegmentedItemShape.current
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val descColor =
        if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = InitializationTaskRowDefaults.HorizontalPadding,
                vertical = InitializationTaskRowDefaults.VerticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(InitializationTaskRowDefaults.IconToTextGap)
    ) {
        // 图标区域：固定对齐 Row 的顶部
        Box(
            modifier = Modifier.size(InitializationTaskRowDefaults.IconSize),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier
                        // .padding(top = InitializationTaskRowDefaults.IndicatorTopOffset)
                        .size(InitializationTaskRowDefaults.IndicatorSize),
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp,
                    strokeCap = StrokeCap.Round
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        // .padding(top = InitializationTaskRowDefaults.IconTopOffset)
                        .size(InitializationTaskRowDefaults.IconSize)
                )
            }
        }

        // 文本内容区域
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    // style = MaterialTheme.typography.bodyLarge,
                    style = MaterialTheme.typography.bodyMedium,
                    color = titleColor,
                    fontWeight = FontWeight.Normal
                )
                if (trailingContent != null) {
                    trailingContent()
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    // style = MaterialTheme.typography.bodyMedium,
                    style = MaterialTheme.typography.bodySmall,
                    color = descColor,
                    modifier = Modifier.padding(top = InitializationTaskRowDefaults.TextLineGap)
                )
            }
        }
    }
}

@Composable
fun InitializationTaskRow(
    taskState: InitializationTaskState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isError = taskState.status == InitializationTaskStatus.FAILED
    val isRunning = taskState.status == InitializationTaskStatus.RUNNING

    val icon = when (taskState.status) {
        InitializationTaskStatus.COMPLETED -> AppMaterialSymbols.check_circle
        InitializationTaskStatus.FAILED -> AppMaterialSymbols.error
        else -> AppMaterialSymbols.circle
    }

    val iconColor = when (taskState.status) {
        InitializationTaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        InitializationTaskStatus.FAILED -> MaterialTheme.colorScheme.error
        InitializationTaskStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    val title = when (taskState.task) {
        InitializationTask.LAWNICONS -> "从仓库拉取 Lawnicons 资源"
        InitializationTask.APK_TEMPLATE -> "从仓库拉取图标包 APK 模板"
        InitializationTask.APP_M3_CACHE -> "生成颜色映射缓存"
    }

    val description = if (isError) {
        when (taskState.task) {
            InitializationTask.LAWNICONS -> "拉取失败，请重试。已回退至内置资源"
            InitializationTask.APK_TEMPLATE -> "拉取失败，请重试。暂无法构建图标包 APK 产物"
            InitializationTask.APP_M3_CACHE -> "生成失败，请重试"
        }
    } else {
        when (taskState.task) {
            InitializationTask.LAWNICONS -> "从 HyperIconLab/releases 获取 Lawnicons SVG\n用于生成图标"
            InitializationTask.APK_TEMPLATE -> "从 HyperIconLab/releases 获取模板\n用于构建可安装的图标包 APK 产物"
            InitializationTask.APP_M3_CACHE -> "加快预览和构建速度"
        }
    }

    InitializationTaskItem(
        title = title,
        description = description,
        icon = icon,
        iconColor = iconColor,
        isRunning = isRunning,
        isError = isError,
        onClick = if (isError) onRetry else null
    )
}

@Composable
fun AssetUpdateTaskRow(
    title: String,
    oldVersion: String?,
    newVersion: String?,
    status: InitializationTaskStatus,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    val isRunning = status == InitializationTaskStatus.RUNNING

    val icon = if (status == InitializationTaskStatus.COMPLETED) {
        AppMaterialSymbols.check_circle
    } else {
        AppMaterialSymbols.circle
    }

    val iconColor = when (status) {
        InitializationTaskStatus.COMPLETED -> MaterialTheme.colorScheme.primary
        InitializationTaskStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    InitializationTaskItem(
        title = title,
        description = description,
        icon = icon,
        iconColor = iconColor,
        isRunning = isRunning,
        trailingContent = {
            if (oldVersion != null && newVersion != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = oldVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = GoogleSansCodeFontFamily,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Icon(
                        imageVector = AppMaterialSymbols.arrow_right_alt,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = newVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = GoogleSansCodeFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}
