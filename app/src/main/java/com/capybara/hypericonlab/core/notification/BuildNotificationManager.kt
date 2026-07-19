package com.capybara.hypericonlab.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.capybara.hypericonlab.MainActivity
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.notification.BuildNotificationManager.Companion.RUNNING_NOTIFICATION_ID
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus

/**
 * 构建通知管理器：负责 NotificationChannel 创建、进度通知构建与更新、终态通知切换。
 *
 * 通知策略：
 * - **运行中**：固定 id（[RUNNING_NOTIFICATION_ID]）的进度通知，带 ProgressBar，500ms 节流更新
 * - **终态**（成功/失败）：使用任务 hashCode 作为通知 id，显示结果摘要，自动取消
 * - 点击通知跳转 [MainActivity]
 *
 * 通知重要性：IMPORTANCE_LOW（不发声，仅状态栏+下拉通知），符合构建进度场景
 *
 * Android 13+ 需 [android.Manifest.permission.POST_NOTIFICATIONS] 运行时授权；
 * 用户拒绝时所有 notify 调用静默失败，不影响任务执行。
 */
class BuildNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    /**
     * 创建 NotificationChannel（Android 8.0+ 必须）。
     * 幂等：重复调用安全。
     */
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

    /**
     * 显示或更新运行中任务的进度通知。
     * 内部 500ms 节流：两次 [updateProgress] 间隔小于 500ms 时忽略。
     *
     * @param task 当前任务状态（RUNNING）
     */
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

    /**
     * 显示任务终态通知（成功/失败），并取消运行中通知。
     * 每个任务使用独立的通知 id（基于 taskId hashCode），避免覆盖其他任务的结果。
     *
     * @param task 终态任务（SUCCESS 或 FAILED）
     */
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
            .build()

        notificationManager.notify(task.taskId.hashCode(), notification)
    }

    /**
     * 取消运行中通知（任务被取消时调用）。
     */
    fun cancelRunning() {
        notificationManager.cancel(RUNNING_NOTIFICATION_ID)
    }

    // 构建点击通知跳转 Intent（跳转到 MainActivity，由 MainScreen 路由到任务页）
    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            ContentIntentConfig.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        // 运行中通知的固定 id（同时只执行一个任务，固定 id 便于更新）
        const val RUNNING_NOTIFICATION_ID = 1001

        // 通知关键参数集中声明，便于调参
        private object NotificationConfig {
            // NotificationChannel id
            const val CHANNEL_ID = "build_task_channel"

            // 进度更新节流间隔（毫秒）
            const val PROGRESS_THROTTLE_MS = 500L
        }

        // 内容 Intent 相关参数
        private object ContentIntentConfig {
            // PendingIntent requestCode（固定值，避免重复创建）
            const val REQUEST_CODE = 0
        }
    }

    // 上次进度更新时间戳（用于节流，注意：实例级状态，BuildNotificationManager 为 factory 注入）
    private var lastProgressTime: Long = 0L
}
