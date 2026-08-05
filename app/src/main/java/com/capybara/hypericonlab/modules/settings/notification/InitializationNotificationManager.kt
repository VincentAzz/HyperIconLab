package com.capybara.hypericonlab.modules.settings.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.capybara.hypericonlab.MainActivity
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationState
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTask
import com.capybara.hypericonlab.modules.icon.domain.model.InitializationTaskStatus

// 初始化任务通知管理器
class InitializationNotificationManager(
    private val context: Context
) {
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    private var lastProgressTime = 0L

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.initialization_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.initialization_notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun updateProgress(state: InitializationState) {
        val runningTask = state.tasks.firstOrNull {
            it.status == InitializationTaskStatus.RUNNING
        } ?: return
        val now = System.currentTimeMillis()
        if (now - lastProgressTime < PROGRESS_THROTTLE_MS) return
        lastProgressTime = now

        val progress = (runningTask.progress * PROGRESS_MAX)
            .toInt()
            .coerceIn(0, PROGRESS_MAX)
        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(R.string.initialization_notification_title))
            .setContentText(
                context.getString(
                    R.string.initialization_notification_progress,
                    taskLabel(runningTask.task),
                    progress
                )
            )
            .setProgress(PROGRESS_MAX, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(buildContentIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notificationManager.notify(RUNNING_NOTIFICATION_ID, notification)
    }

    fun showTerminal(state: InitializationState) {
        notificationManager.cancel(RUNNING_NOTIFICATION_ID)
        val failedTask = state.failedTask
        val (titleRes, textRes) = when {
            state.isCompleted ->
                R.string.initialization_notification_completed_title to
                        R.string.initialization_notification_completed_text

            failedTask != null ->
                R.string.initialization_notification_failed_title to
                        R.string.initialization_notification_failed_text

            else -> return
        }
        val contentText = if (failedTask != null && !state.isCompleted) {
            context.getString(
                textRes,
                taskLabel(failedTask),
                state.failureMessage ?: context.getString(
                    R.string.initialization_notification_unknown_error
                )
            )
        } else {
            context.getString(textRes)
        }
        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(
                if (state.isCompleted) {
                    android.R.drawable.stat_sys_download_done
                } else {
                    android.R.drawable.stat_notify_error
                }
            )
            .setContentTitle(context.getString(titleRes))
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(TERMINAL_NOTIFICATION_ID, notification)
    }

    fun cancelRunning() {
        notificationManager.cancel(RUNNING_NOTIFICATION_ID)
    }

    private fun taskLabel(task: InitializationTask): String = when (task) {
        InitializationTask.LAWNICONS -> context.getString(R.string.initialization_task_lawnicons)
        InitializationTask.APK_TEMPLATE -> context.getString(R.string.initialization_task_template)
        InitializationTask.APP_M3_CACHE -> context.getString(R.string.initialization_task_app_m3)
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            CONTENT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val CHANNEL_ID = "initialization_channel"
        const val RUNNING_NOTIFICATION_ID = 3001

        private const val TERMINAL_NOTIFICATION_ID = 3002
        private const val CONTENT_REQUEST_CODE = 3000
        private const val PROGRESS_MAX = 100
        private const val PROGRESS_THROTTLE_MS = 500L
    }
}
