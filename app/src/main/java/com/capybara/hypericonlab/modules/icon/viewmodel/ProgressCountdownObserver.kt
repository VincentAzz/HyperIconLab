package com.capybara.hypericonlab.modules.icon.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// 进度倒计时观察器：监听运行状态与进度，任务完成后启动 3 秒倒计时动画
// 倒计时结束后隐藏进度条并重置进度，运行中显示进度条
class ProgressCountdownObserver(
    private val scope: CoroutineScope,
    private val isRunning: StateFlow<Boolean>,
    private val currentProgress: StateFlow<Float>,
    private val onCountdownActiveChange: (Boolean) -> Unit,
    private val onCountdownProgressChange: (Float) -> Unit,
    private val onShowProgressChange: (Boolean) -> Unit,
    private val onCurrentProgressChange: (Float) -> Unit
) {
    // 倒计时是否激活
    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    // 倒计时进度（1f → 0f）
    private val _countdownProgress = MutableStateFlow(1f)
    val countdownProgress: StateFlow<Float> = _countdownProgress.asStateFlow()

    // 是否显示进度条
    private val _showProgress = MutableStateFlow(false)
    val showProgress: StateFlow<Boolean> = _showProgress.asStateFlow()

    // 启动观察：运行结束时若进度已满则启动倒计时，运行中显示进度条
    fun observe() {
        scope.launch {
            isRunning.collect { running ->
                if (!running && currentProgress.value >= 1f) {
                    _isCountdownActive.value = true
                    onCountdownActiveChange(true)
                    val startTime = System.currentTimeMillis()
                    val duration = ProgressCountdownConfig.COUNTDOWN_DURATION_MS
                    while (System.currentTimeMillis() - startTime < duration) {
                        val progress =
                            1f - (System.currentTimeMillis() - startTime).toFloat() / duration
                        _countdownProgress.value = progress
                        onCountdownProgressChange(progress)
                        delay(ProgressCountdownConfig.COUNTDOWN_FRAME_INTERVAL)
                    }
                    _isCountdownActive.value = false
                    onCountdownActiveChange(false)
                    _showProgress.value = false
                    onShowProgressChange(false)
                    onCurrentProgressChange(0f)
                } else if (running) {
                    _showProgress.value = true
                    onShowProgressChange(true)
                    _isCountdownActive.value = false
                    onCountdownActiveChange(false)
                }
            }
        }
    }

    private object ProgressCountdownConfig {
        // 倒计时总时长
        const val COUNTDOWN_DURATION_MS = 3000L

        // 倒计时帧间隔
        val COUNTDOWN_FRAME_INTERVAL = 16.milliseconds
    }
}
