package com.capybara.hypericonlab.modules.icon.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// 日志管理器
class IconLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun addLog(message: String, type: LogType = LogType.INFO) {
        val durationRegex = "[,，]?\\s*耗时\\s*(\\d+ms)".toRegex()
        val match = durationRegex.find(message)
        val (finalMessage, duration) = if (match != null) {
            val d = match.groupValues[1]
            val m = message.replace(durationRegex, "").trim()
            m to d
        } else {
            message to null
        }
        _logs.update { it + LogEntry(finalMessage, type = type, duration = duration) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
