package com.capybara.hypericonlab.modules.iconpack.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.capybara.hypericonlab.MainActivity
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_INSTALL_APK_URI
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_TAB_INDEX
import com.capybara.hypericonlab.core.designsystem.navigation.TAB_INDEX_TASK
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTask
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.iconpack.domain.model.ProductType

// 构建通知管理器
class BuildNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)


    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NotificationConfig.CHANNEL_ID,
            context.getString(R.string.build_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.build_notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }


    fun updateProgress(task: BuildTask) {
        if (task.status != BuildTaskStatus.RUNNING) return
        val now = System.currentTimeMillis()
        if (now - lastProgressTime < NotificationConfig.PROGRESS_THROTTLE_MS) return
        lastProgressTime = now

        val current = (task.progress * task.iconCount).toInt().coerceIn(0, task.iconCount)
        val contentText = if (task.currentPackage != null) {
            context.getString(
                R.string.build_notification_progress_fmt,
                task.currentPackage,
                current,
                task.iconCount
            )
        } else {
            context.getString(R.string.build_notification_preparing, task.iconSetLabel)
        }

        val notification = NotificationCompat.Builder(context, NotificationConfig.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                context.getString(
                    R.string.build_notification_title_fmt,
                    task.iconSetLabel
                )
            )
            .setContentText(contentText)
            .setProgress(task.iconCount, current, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(buildContentIntent())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(RUNNING_NOTIFICATION_ID, notification)
    }


    fun showTerminal(task: BuildTask) {
        // 取消运行中通知
        notificationManager.cancel(RUNNING_NOTIFICATION_ID)

        val (titleRes, contentText) = when (task.status) {
            BuildTaskStatus.SUCCESS -> {
                val durationSec = (task.durationMs ?: 0L) / 1000.0
                R.string.build_notification_success_title to
                        context.getString(
                            R.string.build_notification_success_fmt,
                            task.iconSetLabel,
                            task.iconCount,
                            durationSec
                        )
            }

            BuildTaskStatus.FAILED -> {
                R.string.build_notification_failed_title to
                        context.getString(
                            R.string.build_notification_failed_fmt,
                            task.iconSetLabel,
                            task.errorMessage
                                ?: context.getString(R.string.build_notification_unknown_error)
                        )
            }

            else -> return
        }

        val notification = NotificationCompat.Builder(context, NotificationConfig.CHANNEL_ID)
            .setSmallIcon(
                if (task.status == BuildTaskStatus.SUCCESS)
                    android.R.drawable.stat_sys_download_done
                else
                    android.R.drawable.stat_notify_error
            )
            .setContentTitle(context.getString(titleRes))
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (task.status == BuildTaskStatus.SUCCESS &&
                    task.productType == ProductType.APK &&
                    task.artifactUri != null
                ) {
                    addAction(
                        android.R.drawable.stat_sys_download_done,
                        context.getString(R.string.build_notification_install),
                        buildInstallPendingIntent(task)
                    )
                }
            }
            .build()

        notificationManager.notify(task.taskId.hashCode(), notification)
    }

    fun cancelRunning() {
        notificationManager.cancel(RUNNING_NOTIFICATION_ID)
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TAB_INDEX, TAB_INDEX_TASK)
        }
        return PendingIntent.getActivity(
            context,
            ContentIntentConfig.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildInstallPendingIntent(task: BuildTask): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TAB_INDEX, TAB_INDEX_TASK)
            putExtra(EXTRA_INSTALL_APK_URI, task.artifactUri)
        }
        return PendingIntent.getActivity(
            context,
            task.taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val RUNNING_NOTIFICATION_ID = 1001

        private object NotificationConfig {
            // NotificationChannel id
            const val CHANNEL_ID = "build_task_channel"

            // 进度更新节流间隔（毫秒）
            const val PROGRESS_THROTTLE_MS = 500L
        }

        private object ContentIntentConfig {
            const val REQUEST_CODE = 0
        }
    }

    private var lastProgressTime: Long = 0L
}
