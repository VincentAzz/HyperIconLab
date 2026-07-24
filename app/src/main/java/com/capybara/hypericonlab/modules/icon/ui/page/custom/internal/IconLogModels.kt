package com.capybara.hypericonlab.modules.icon.ui.page.custom.internal

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 日志类型
enum class LogType { INFO, ERROR, SUCCESS }

data class LogEntry(
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType = LogType.INFO,
    val duration: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(
            Date(timestamp)
        )
}
