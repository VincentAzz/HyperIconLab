package com.capybara.hypericonlab.modules.settings.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.logging.AppLogStore
import com.capybara.hypericonlab.core.logging.LogType
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskStatus
import com.capybara.hypericonlab.modules.icon.domain.usecase.InitializationCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

// 初始化前台服务，承载初始化期间的进度通知
class InitializationForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    private val initializationCoordinator: InitializationCoordinator by lazy {
        GlobalContext.get().get()
    }
    private val notificationManager: InitializationNotificationManager by lazy {
        GlobalContext.get().get()
    }
    private val appLogStore: AppLogStore by lazy {
        GlobalContext.get().get()
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
        startForegroundInternal(buildPlaceholderNotification())
        appLogStore.add("初始化通知：前台服务已启动", LogType.INFO)
        observeInitialization()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        notificationManager.cancelRunning()
        appLogStore.add("初始化通知：前台服务已停止", LogType.INFO)
        super.onDestroy()
    }

    private fun observeInitialization() {
        observeJob = scope.launch {
            launch {
                initializationCoordinator.state.collectLatest { state ->
                    if (initializationCoordinator.assetUpdateState.value != null) return@collectLatest
                    val isRunning = state.tasks.any {
                        it.status == InitializationTaskStatus.RUNNING
                    }
                    val hasPendingTasks = state.tasks.any {
                        it.status == InitializationTaskStatus.PENDING
                    }
                    val isTerminalState = !isRunning && !hasPendingTasks
                    val isTerminalFailure = state.failedTask != null && isTerminalState
                    when {
                        isRunning -> notificationManager.updateProgress(state)
                        state.isCompleted || isTerminalFailure -> {
                            notificationManager.showTerminal(state)
                            stopSelf()
                        }

                        state.requiresManualStart || isTerminalState -> stopSelf()
                    }
                }
            }
            launch {
                initializationCoordinator.assetUpdateState.collectLatest { assetState ->
                    if (assetState == null) return@collectLatest
                    val running = assetState.tasks.any {
                        it.status == InitializationTaskStatus.RUNNING
                    }
                    val failedTask = assetState.tasks.firstOrNull {
                        it.status == InitializationTaskStatus.FAILED
                    }
                    when {
                        running -> notificationManager.updateProgress(assetState)
                        failedTask != null -> {
                            notificationManager.showTerminal(
                                InitializationState(
                                    tasks = assetState.tasks,
                                    failedTask = failedTask.task,
                                    failureMessage = failedTask.message
                                )
                            )
                            stopSelf()
                        }
                    }
                }
            }
        }
    }

    private fun startForegroundInternal(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                InitializationNotificationManager.RUNNING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(
                InitializationNotificationManager.RUNNING_NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun buildPlaceholderNotification(): Notification =
        NotificationCompat.Builder(this, InitializationNotificationManager.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.initialization_notification_title))
            .setContentText(getString(R.string.initialization_notification_preparing))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, InitializationForegroundService::class.java)
    }
}
