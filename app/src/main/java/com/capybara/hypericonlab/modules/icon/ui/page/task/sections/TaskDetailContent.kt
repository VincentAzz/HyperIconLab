package com.capybara.hypericonlab.modules.icon.ui.page.task.sections

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.capybara.hypericonlab.modules.build.domain.model.BuildTask
import com.capybara.hypericonlab.modules.build.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.icon.ui.page.task.component.TaskDetailSheetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// 任务详情内容区：加载预览位图 + LazyColumn 编排各分段卡片

@Composable
fun DetailContent(
    task: BuildTask,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewBitmap by remember(task.taskId, task.status) {
        mutableStateOf<Bitmap?>(null)
    }
    LaunchedEffect(task.taskId, task.status) {
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
