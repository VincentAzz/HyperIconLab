package com.capybara.hypericonlab.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppLogStore {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun add(message: String, type: LogType = LogType.INFO) {
        val durationRegex = "[,，]?\\s*耗时\\s*(\\d+ms)".toRegex()
        val match = durationRegex.find(message)
        val (finalMessage, duration) = if (match != null) {
            val value = match.groupValues[1]
            message.replace(durationRegex, "").trim() to value
        } else {
            message to null
        }
        _logs.update { it + LogEntry(finalMessage, type = type, duration = duration) }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
