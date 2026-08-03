package com.capybara.hypericonlab.modules.iconpack.data.local

import android.content.Context
import android.graphics.Bitmap
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

/**
 * 已完成构建任务的持久化存储。
 *
 * - 任务列表：filesDir/build_tasks.json（kotlinx-serialization JSON）
 * - 缩略图：filesDir/build_thumbnails/<taskId>.png（2 图标，约 540×320）
 * - 预览图：filesDir/build_previews/<taskId>.png（8 图标，1080×640）
 *
 * 已完成列表上限 50 条，超出时按完成时间删除最旧任务及其图片文件。
 *
 * 注意：本类只负责文件读写与容量限制，不负责 PENDING/RUNNING 任务的状态管理
 * （进程被杀时未完成任务自然丢失，符合既定策略）。
 */
class BuildTaskStore(private val context: Context) {

    private val filesDir = context.filesDir
    private val taskListFile = File(filesDir, StoreConfig.TASK_LIST_FILENAME)
    private val thumbnailDir = File(filesDir, StoreConfig.THUMBNAIL_DIRNAME)
    private val previewDir = File(filesDir, StoreConfig.PREVIEW_DIRNAME)

    // JSON 配置：忽略未知字段以兼容未来版本，输出默认值以便回写
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // List<BuildTask> 序列化器（泛型类型显式指定，避免 encode/decode 重载推断失败）
    private val taskListSerializer = ListSerializer(BuildTask.serializer())


    // 异步加载已完成任务列表
    suspend fun loadFinishedTasks(): List<BuildTask> = withContext(Dispatchers.IO) {
        if (!taskListFile.exists()) return@withContext emptyList()
        try {
            val text = taskListFile.readText()
            json.decodeFromString(taskListSerializer, text)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to load build tasks, deleting corrupt file")
            taskListFile.delete()
            emptyList()
        }
    }


    // 异步持久化已完成任务列表，自动应用容量上限
    suspend fun saveFinishedTasks(tasks: List<BuildTask>) = withContext(Dispatchers.IO) {
        val trimmed = applyCapacityLimit(tasks)
        try {
            taskListFile.parentFile?.mkdirs()
            taskListFile.writeText(json.encodeToString(taskListSerializer, trimmed))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save build tasks")
        }
    }


    // 保存任务缩略图（2 图标）为 PNG
    suspend fun saveThumbnail(taskId: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        thumbnailDir.mkdirs()
        val file = File(thumbnailDir, "$taskId${StoreConfig.IMAGE_EXT}")
        try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, StoreConfig.IMAGE_QUALITY, out)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save thumbnail for $taskId")
        }
    }


    // 保存任务预览图（8 图标）为 PNG
    suspend fun savePreview(taskId: String, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        previewDir.mkdirs()
        val file = File(previewDir, "$taskId${StoreConfig.IMAGE_EXT}")
        try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, StoreConfig.IMAGE_QUALITY, out)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save preview for $taskId")
        }
    }


    // 读取任务缩略图文件，不存在返回 null
    suspend fun loadThumbnail(taskId: String): File? = withContext(Dispatchers.IO) {
        val file = File(thumbnailDir, "$taskId${StoreConfig.IMAGE_EXT}")
        if (file.exists()) file else null
    }


    // 读取任务预览图文件，不存在返回 null
    suspend fun loadPreview(taskId: String): File? = withContext(Dispatchers.IO) {
        val file = File(previewDir, "$taskId${StoreConfig.IMAGE_EXT}")
        if (file.exists()) file else null
    }


    // 删除单条任务：返回新列表
    suspend fun deleteTask(
        taskId: String,
        currentList: List<BuildTask>
    ): List<BuildTask> = withContext(Dispatchers.IO) {
        File(thumbnailDir, "$taskId${StoreConfig.IMAGE_EXT}").delete()
        File(previewDir, "$taskId${StoreConfig.IMAGE_EXT}").delete()
        currentList.filterNot { it.taskId == taskId }
    }

    // 应用容量上限：超出时按 finishedAt 升序删除最旧任务及其图片文件
    private fun applyCapacityLimit(tasks: List<BuildTask>): List<BuildTask> {
        if (tasks.size <= StoreConfig.MAX_FINISHED_TASKS) return tasks
        val sorted = tasks.sortedBy { it.finishedAt ?: it.submittedAt }
        val toRemove = sorted.take(tasks.size - StoreConfig.MAX_FINISHED_TASKS)
        toRemove.forEach { task ->
            File(thumbnailDir, "${task.taskId}${StoreConfig.IMAGE_EXT}").delete()
            File(previewDir, "${task.taskId}${StoreConfig.IMAGE_EXT}").delete()
        }
        val removeIds = toRemove.map { it.taskId }.toSet()
        return tasks.filterNot { it.taskId in removeIds }
    }

    companion object {
        private const val TAG = "BuildTaskStore"

        private object StoreConfig {
            // 已完成任务列表 JSON 文件名（位于 filesDir 根下）
            const val TASK_LIST_FILENAME = "build_tasks.json"

            // 缩略图目录名
            const val THUMBNAIL_DIRNAME = "build_thumbnails"

            // 预览图目录名
            const val PREVIEW_DIRNAME = "build_previews"

            // 图片文件扩展名
            const val IMAGE_EXT = ".png"

            // 图片压缩质量
            const val IMAGE_QUALITY = 100

            // 已完成列表上限，超出时按完成时间删除最旧的
            const val MAX_FINISHED_TASKS = 50
        }
    }
}
