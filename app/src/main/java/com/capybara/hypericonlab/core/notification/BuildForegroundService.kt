package com.capybara.hypericonlab.core.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.icon.domain.usecase.BuildTaskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * 构建前台服务：在任务执行期间保持运行，降低后台被杀概率。
 *
 * 启停策略：
 * - 由 [BuildTaskManager] 在有 PENDING/RUNNING 任务时调用 [start] 启动
 * - 当 activeTasks 为空（所有任务已终态或被取消）时自动 [stopSelf]
 *
 * 通知策略：
 * - 启动时调用 [startForeground] 显示运行中通知（FOREGROUND_SERVICE_TYPE_DATA_SYNC，Android 14+ 必须声明类型）
 * - 通过 collect [BuildTaskManager.activeTasks] 实时更新通知内容
 * - 第一个 RUNNING 任务的进度作为通知进度
 * - 任务终态时显示对应成功/失败通知
 *
 * 注意：本服务为 START_NOT_STICKY，进程被杀后不自动重启（符合 Q2 策略：PENDING/RUNNING 任务丢失）
 */
class BuildForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    // 由 GlobalContext 获取单例（避免 Service 重建时丢失依赖注入）
    private val taskManager: BuildTaskManager by lazy {
        GlobalContext.get().get<BuildTaskManager>()
    }
    private val notificationManager: BuildNotificationManager by lazy {
        GlobalContext.get().get<BuildNotificationManager>()
    }

    // 当前正在显示进度通知的任务 id，避免重复更新
    private var currentDisplayTaskId: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
        // 启动时立即显示占位前台通知，避免 ANR
        startForegroundInternal(buildPlaceholderNotification())
        observeTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 重新确认前台状态（系统可能在某些场景下需要重新调用 startForeground）
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        notificationManager.cancelRunning()
        super.onDestroy()
    }

    // 观察 activeTasks 实时更新通知
    private fun observeTasks() {
        observeJob = scope.launch {
            taskManager.activeTasks.collectLatest { tasks ->
                if (tasks.isEmpty()) {
                    // 无活动任务：停止服务
                    stopSelf()
                    return@collectLatest
                }
                // 优先显示 RUNNING 任务，其次显示队首 PENDING 任务
                val displayTask = tasks.firstOrNull { it.status == BuildTaskStatus.RUNNING }
                    ?: tasks.firstOrNull()
                if (displayTask != null && displayTask.taskId != currentDisplayTaskId) {
                    currentDisplayTaskId = displayTask.taskId
                }
                if (displayTask != null) {
                    notificationManager.updateProgress(displayTask)
                }
            }
        }
        // 同时观察 finishedTasks，显示刚结束任务的结果通知
        scope.launch {
            taskManager.finishedTasks.collectLatest { finished ->
                // 仅显示最近一个任务（队首）的终态通知
                val latest = finished.lastOrNull() ?: return@collectLatest
                if (latest.status == BuildTaskStatus.SUCCESS ||
                    latest.status == BuildTaskStatus.FAILED
                ) {
                    notificationManager.showTerminal(latest)
                }
            }
        }
    }

    // 启动前台服务并显示通知（兼容 Android 14+ 类型声明）
    private fun startForegroundInternal(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ 必须声明具体前台服务类型
            startForeground(
                BuildNotificationManager.RUNNING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                BuildNotificationManager.RUNNING_NOTIFICATION_ID,
                notification
            )
        }
    }

    // 占位通知：服务刚启动时还没有任务进度，显示"准备中"
    private fun buildPlaceholderNotification(): android.app.Notification {
        return androidx.core.app.NotificationCompat.Builder(
            this,
            BuildNotificationManagerConfig.PLACEHOLDER_CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                getString(
                    com.capybara.hypericonlab.R.string.build_notification_preparing,
                    "HyperIconLab"
                )
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        // 服务启动 Intent 构造助手
        fun createIntent(context: android.content.Context): Intent =
            Intent(context, BuildForegroundService::class.java)

        // 服务相关常量
        private object BuildNotificationManagerConfig {
            // 占位通知使用的 channel id（复用 BuildNotificationManager 的 channel）
            const val PLACEHOLDER_CHANNEL_ID = "build_task_channel"
        }
    }
}
