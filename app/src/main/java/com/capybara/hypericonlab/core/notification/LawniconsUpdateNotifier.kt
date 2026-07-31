package com.capybara.hypericonlab.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.capybara.hypericonlab.MainActivity
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_TAB_INDEX
import com.capybara.hypericonlab.core.designsystem.navigation.TAB_INDEX_SETTINGS
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.FailureReason

/**
 * 云端资源更新失败通知器：在 [LawniconsUpdateManager] 检测到失败时推送系统通知。
 *
 * 通知策略：
 * - **失败**：固定 id（[NotificationConfig.NOTIFICATION_ID]）的通知，文案按 [FailureReason] 分类，
 *   点击跳转 [MainActivity] 的设置 tab（资产页入口），自动取消
 * - **成功**：不发通知（避免打扰，仅资产页 UI 可见）
 *
 * 通知重要性：IMPORTANCE_DEFAULT（首次会响铃/弹出，后续仅状态栏），符合"失败提醒"场景
 * 通知渠道：独立 [NotificationConfig.CHANNEL_ID]，与构建任务通知互不干扰
 *
 * Android 13+ 需 [android.Manifest.permission.POST_NOTIFICATIONS] 运行时授权；
 * 用户拒绝时 notify 调用静默失败，不影响更新流程本身（state 仍会更新到 Failed）。
 */
class LawniconsUpdateNotifier(private val context: Context) {

    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        if (channelCreated) return
        val channel = NotificationChannel(
            NotificationConfig.CHANNEL_ID,
            context.getString(R.string.lawnicons_update_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.lawnicons_update_notification_channel_desc)
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
        channelCreated = true
    }

    // 显示更新失败通知。同一 id 覆盖旧通知（避免堆积）。
    fun notifyFailed(reason: FailureReason) {
        createChannel()
        val contentText = FailureMessage.forReason(reason)
        val notification = NotificationCompat.Builder(context, NotificationConfig.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(context.getString(R.string.lawnicons_update_notification_failed_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setAutoCancel(true)
            .setContentIntent(buildContentIntent())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NotificationConfig.NOTIFICATION_ID, notification)
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TAB_INDEX, TAB_INDEX_SETTINGS)
        }
        return PendingIntent.getActivity(
            context,
            ContentIntentConfig.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private object NotificationConfig {
            const val CHANNEL_ID = "lawnicons_update_channel"

            const val NOTIFICATION_ID = 2001
        }

        private object ContentIntentConfig {
            const val REQUEST_CODE = 0
        }
    }

    private var channelCreated: Boolean = false

    private object FailureMessage {
        const val RATE_LIMITED = "GitHub API 限速，请稍后重试或切换网络"

        const val NETWORK_ERROR = "连接失败，请检查网络连接"

        const val TIMEOUT = "连接超时，请检查网络连接"

        const val HTTP_ERROR = "连接异常，请稍后重试"

        const val CORRUPTED = "校验失败，请重新下载"

        const val PARSE_ERROR = "解析失败"

        const val EXTRACT_FAILED = "解压失败"

        const val ACTIVATE_FAILED = "版本切换失败"

        const val UNKNOWN = "更新失败，请重试"

        fun forReason(reason: FailureReason): String = when (reason) {
            FailureReason.RATE_LIMITED -> RATE_LIMITED
            FailureReason.NETWORK_ERROR -> NETWORK_ERROR
            FailureReason.TIMEOUT -> TIMEOUT
            FailureReason.HTTP_ERROR -> HTTP_ERROR
            FailureReason.CORRUPTED -> CORRUPTED
            FailureReason.PARSE_ERROR -> PARSE_ERROR
            FailureReason.EXTRACT_FAILED -> EXTRACT_FAILED
            FailureReason.ACTIVATE_FAILED -> ACTIVATE_FAILED
            FailureReason.UNKNOWN -> UNKNOWN
        }
    }
}
