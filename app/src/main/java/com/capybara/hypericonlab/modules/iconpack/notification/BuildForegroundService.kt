package com.capybara.hypericonlab.modules.iconpack.notification

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.iconpack.domain.usecase.BuildTaskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

// 构建前台服务
class BuildForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    private val taskManager: BuildTaskManager by lazy {
        GlobalContext.get().get<BuildTaskManager>()
    }
    private val notificationManager: BuildNotificationManager by lazy {
        GlobalContext.get().get<BuildNotificationManager>()
    }

    private var currentDisplayTaskId: String? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
        startForegroundInternal(buildPlaceholderNotification())
        observeTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        notificationManager.cancelRunning()
        super.onDestroy()
    }

    private fun observeTasks() {
        observeJob = scope.launch {
            taskManager.activeTasks.collectLatest { tasks ->
                if (tasks.isEmpty()) {
                    stopSelf()
                    return@collectLatest
                }
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
        scope.launch {
            taskManager.finishedTasks.collectLatest { finished ->
                val latest = finished.lastOrNull() ?: return@collectLatest
                if (latest.status == BuildTaskStatus.SUCCESS ||
                    latest.status == BuildTaskStatus.FAILED
                ) {
                    notificationManager.showTerminal(latest)
                }
            }
        }
    }

    private fun startForegroundInternal(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
        fun createIntent(context: android.content.Context): Intent =
            Intent(context, BuildForegroundService::class.java)

        private object BuildNotificationManagerConfig {
            const val PLACEHOLDER_CHANNEL_ID = "build_task_channel"
        }
    }
}
