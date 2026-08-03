package com.capybara.hypericonlab.modules.iconpack.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.iconpack.data.local.BuildTaskStore
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTask
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.iconpack.domain.model.ProductType
import com.capybara.hypericonlab.modules.iconpack.notification.BuildForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 构建任务管理器：全局单例，维护任务队列与执行调度。
 *
 * 队列模型：
 * - [activeTasks]：PENDING + RUNNING 任务列表（内存中，不持久化）
 * - [finishedTasks]：SUCCESS + FAILED 任务列表（持久化到 [BuildTaskStore]）
 *
 * 执行调度：
 * - 单线程串行执行（[buildDispatcher]），一次只运行一个任务
 * - PENDING 任务按提交顺序排队，前一个完成（成功/失败/取消）后自动调度下一个
 * - 每个任务在独立的协程中执行，失败/取消不影响后续任务
 *
 * 进程被杀策略（已确认 Q2）：
 * - PENDING/RUNNING 任务丢失，不持久化恢复
 * - 已完成列表已持久化，进程重启后通过 [loadFinishedTasksOnStart] 恢复
 *
 * Bitmap 缓存：
 * - 提交时缓存的预览图保存在 [previewCache]，任务执行后移除
 * - 进程被杀时缓存丢失，PENDING 任务也无法恢复，符合策略
 */
class BuildTaskManager(
    private val context: Context,
    private val executor: BuildTaskExecutor,
    private val taskStore: BuildTaskStore
) {

    data class ApkInstallRequest(
        val taskId: String,
        val artifactUri: String
    )

    // 活动任务（PENDING + RUNNING），内存中，不持久化
    private val _activeTasks = MutableStateFlow<List<BuildTask>>(emptyList())
    val activeTasks: StateFlow<List<BuildTask>> = _activeTasks.asStateFlow()

    // 已完成任务（SUCCESS + FAILED），持久化
    private val _finishedTasks = MutableStateFlow<List<BuildTask>>(emptyList())
    val finishedTasks: StateFlow<List<BuildTask>> = _finishedTasks.asStateFlow()

    private val _installRequests = MutableSharedFlow<ApkInstallRequest>(
        extraBufferCapacity = ManagerConfig.INSTALL_REQUEST_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val installRequests = _installRequests.asSharedFlow()

    // 任务协程 Job（仅当前正在执行的任务，串行执行下同时只有一个）
    private var currentJob: Job? = null

    // 预览图缓存：taskId -> 预览图包，任务执行后移除
    private val previewCache = mutableMapOf<String, PreviewBundle>()

    // appColorSchemes 缓存（由 IconViewModel 加载完成后调用 updateAppColorSchemes）
    private var appColorSchemes: Map<String, Pair<String, String>> = emptyMap()

    // 前台服务启动标志位，避免重复 startService
    private var foregroundServiceStarted = false

    // 单线程串行调度器，保证一次只执行一个构建任务
    private val buildDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    // 应用级 Scope，独立于 ViewModel 生命周期（任务跨页面执行）
    private val scope = CoroutineScope(SupervisorJob() + buildDispatcher)

    init {
        // 启动时异步加载已完成任务
        scope.launch {
            _finishedTasks.value = taskStore.loadFinishedTasks()
        }
        // 观察活动任务变化，自动启停前台服务
        scope.launch {
            _activeTasks.collect { tasks ->
                if (tasks.isNotEmpty()) {
                    startForegroundServiceIfNeeded()
                } else {
                    stopForegroundServiceIfNeeded()
                }
            }
        }
    }

    // 启动前台服务（仅在有活动任务时）
    private fun startForegroundServiceIfNeeded() {
        if (foregroundServiceStarted) return
        foregroundServiceStarted = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(BuildForegroundService.createIntent(context))
            } else {
                context.startService(BuildForegroundService.createIntent(context))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to start BuildForegroundService")
            foregroundServiceStarted = false
        }
    }

    // 停止前台服务（无活动任务时由服务自身 stopSelf，这里只重置标志位）
    private fun stopForegroundServiceIfNeeded() {
        if (!foregroundServiceStarted) return
        foregroundServiceStarted = false
        try {
            context.stopService(BuildForegroundService.createIntent(context))
        } catch (e: Exception) {
            // 忽略停止失败
        }
    }

    // 更新 appColorSchemes 缓存
    fun updateAppColorSchemes(schemes: Map<String, Pair<String, String>>) {
        appColorSchemes = schemes
    }

    // 提交构建任务
    fun submit(
        config: IconBuildConfig,
        configSnapshot: IconConfigState,
        productType: ProductType,
        iconSetId: String,
        iconSetLabel: String,
        iconCount: Int,
        wallpaperUri: String?,
        storePreview: Bitmap,
        mainPreview: Bitmap
    ): BuildTask {
        val taskId = generateTaskId(iconSetId)
        val task = BuildTask(
            taskId = taskId,
            config = config,
            configSnapshot = configSnapshot,
            productType = productType,
            iconSetId = iconSetId,
            iconSetLabel = iconSetLabel,
            iconCount = iconCount,
            wallpaperUri = wallpaperUri,
            submittedAt = System.currentTimeMillis()
        )
        // 缓存预览图，供执行时使用
        previewCache[taskId] = PreviewBundle(storePreview, mainPreview)
        // 立即持久化预览图与缩略图，任务卡片在 PENDING 状态即可显示
        scope.launch(Dispatchers.IO) {
            val thumbnail = executor.cropThumbnail(storePreview)
            taskStore.saveThumbnail(taskId, thumbnail)
            taskStore.savePreview(taskId, storePreview)
            if (thumbnail !== storePreview) thumbnail.recycle()
        }
        _activeTasks.value += task
        scheduleNext()
        return task
    }

    // 取消任务
    fun cancel(taskId: String) {
        val task = _activeTasks.value.find { it.taskId == taskId } ?: return
        if (task.status == BuildTaskStatus.RUNNING) {
            currentJob?.cancel()
        } else {
            // PENDING 状态：直接从队列移除
            _activeTasks.value = _activeTasks.value.filterNot { it.taskId == taskId }
            previewCache.remove(taskId)
        }
    }

    // 重试失败任务
    fun retry(
        originalTaskId: String,
        storePreview: Bitmap,
        mainPreview: Bitmap
    ): BuildTask? {
        val original = _finishedTasks.value.find { it.taskId == originalTaskId }
            ?: return null
        if (original.status != BuildTaskStatus.FAILED) return null

        // 从 finishedTasks 移除原任务，同步清理图片
        scope.launch {
            val updated = taskStore.deleteTask(originalTaskId, _finishedTasks.value)
            _finishedTasks.value = updated
            taskStore.saveFinishedTasks(updated)
        }

        // 创建新任务（新 taskId）
        val newTaskId = generateTaskId(original.iconSetId)
        val newTask = original.copy(
            taskId = newTaskId,
            submittedAt = System.currentTimeMillis(),
            startedAt = null,
            finishedAt = null,
            durationMs = null,
            status = BuildTaskStatus.PENDING,
            progress = 0f,
            currentPackage = null,
            errorMessage = null,
            artifactPath = null
        )
        previewCache[newTaskId] = PreviewBundle(storePreview, mainPreview)
        _activeTasks.value = _activeTasks.value + newTask
        scheduleNext()
        return newTask
    }


    // 删除已完成任务
    fun deleteFinished(taskId: String) {
        scope.launch {
            val updated = taskStore.deleteTask(taskId, _finishedTasks.value)
            _finishedTasks.value = updated
            taskStore.saveFinishedTasks(updated)
        }
    }

    // 调度下一个 PENDING 任务执行
    private fun scheduleNext() {
        if (currentJob != null && currentJob?.isActive == true) return
        val nextTask = _activeTasks.value.find { it.status == BuildTaskStatus.PENDING } ?: return
        currentJob = scope.launch {
            executeTask(nextTask)
        }
    }

    // 执行单个任务
    private suspend fun executeTask(task: BuildTask) {
        val bundle = previewCache[task.taskId]
        if (bundle == null) {
            Timber.tag(TAG).w("Preview bundle missing for ${task.taskId}, skip execution")
            _activeTasks.value = _activeTasks.value.filterNot { it.taskId == task.taskId }
            return
        }
        try {
            val result = executor.execute(
                task = task,
                appColorSchemes = appColorSchemes,
                storePreview = bundle.storePreview,
                mainPreview = bundle.mainPreview
            ) { updated ->
                // 实时更新 activeTasks 中对应任务的状态
                _activeTasks.value = _activeTasks.value.map {
                    if (it.taskId == updated.taskId) updated else it
                }
            }
            // 终态任务：从 activeTasks 移到 finishedTasks
            _activeTasks.value = _activeTasks.value.filterNot { it.taskId == result.taskId }
            if (BuildTask.isTerminalStatus(result.status)) {
                _finishedTasks.value = _finishedTasks.value + result
                scope.launch(Dispatchers.IO) {
                    taskStore.saveFinishedTasks(_finishedTasks.value)
                }
            }
            if (result.status == BuildTaskStatus.SUCCESS &&
                result.productType == ProductType.APK
            ) {
                result.artifactUri?.let { uri ->
                    _installRequests.tryEmit(
                        ApkInstallRequest(
                            taskId = result.taskId,
                            artifactUri = uri
                        )
                    )
                }
            }
        } catch (e: CancellationException) {
            // 任务被取消：从 activeTasks 移除，CANCELLED 不进入 finishedTasks
            _activeTasks.value = _activeTasks.value.filterNot { it.taskId == task.taskId }
        } finally {
            previewCache.remove(task.taskId)
            currentJob = null
            // 调度下一个任务
            scheduleNext()
        }
    }

    // 生成任务 id：yyyyMMdd_HHmmss_<iconSetId>，同秒冲突时追加 _2、_3
    private fun generateTaskId(iconSetId: String): String {
        val timestamp = SimpleDateFormat(BuildTask.TASK_ID_DATE_FORMAT, Locale.getDefault())
            .format(Date())
        val existingIds = (_activeTasks.value + _finishedTasks.value).map { it.taskId }.toSet()
        var candidate = "${timestamp}_$iconSetId"
        var suffix = 2
        while (candidate in existingIds) {
            candidate = "${timestamp}_${iconSetId}_$suffix"
            suffix++
        }
        return candidate
    }

    // 预览图缓存数据包
    private data class PreviewBundle(
        val storePreview: Bitmap,
        val mainPreview: Bitmap
    )

    companion object {
        private const val TAG = "BuildTaskManager"

        private object ManagerConfig {
            const val INSTALL_REQUEST_BUFFER_CAPACITY = 4
        }
    }
}
