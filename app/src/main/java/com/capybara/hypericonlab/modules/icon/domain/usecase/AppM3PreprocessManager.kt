package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.modules.render.AppM3ColorCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AppM3PreprocessManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    sealed class PreprocessState {
        data object Idle : PreprocessState()
        data class Running(val computed: Int, val total: Int) : PreprocessState()
        data object Done : PreprocessState()
    }

    private val _state = MutableStateFlow<PreprocessState>(PreprocessState.Idle)
    val state: StateFlow<PreprocessState> = _state.asStateFlow()

    private var appColorSchemes: Map<String, Pair<String, String>> = emptyMap()
    private var preprocessJob: Job? = null
    private val preprocessMutex = Mutex()

    fun updateAppColorSchemes(schemes: Map<String, Pair<String, String>>) {
        appColorSchemes = schemes
    }

    fun startPreprocess(reduceWhiteBg: Boolean = true) {
        if (preprocessJob?.isActive == true) return
        preprocessJob = scope.launch(Dispatchers.Default) {
            preprocessMutex.withLock { runPreprocess(reduceWhiteBg) }
        }
    }

    suspend fun preprocessNow(reduceWhiteBg: Boolean = true) {
        preprocessJob?.let { job ->
            if (job.isActive) {
                job.join()
                if (_state.value !is PreprocessState.Done) {
                    error("App-M3 颜色映射缓存生成失败")
                }
                return
            }
        }
        preprocessMutex.withLock { runPreprocess(reduceWhiteBg) }
    }

    fun cancelPreprocess() {
        preprocessJob?.cancel()
        preprocessJob = null
        _state.value = PreprocessState.Idle
    }

    private suspend fun runPreprocess(reduceWhiteBg: Boolean) {
        if (appColorSchemes.isEmpty()) {
            _state.value = PreprocessState.Done
            return
        }

        _state.value = PreprocessState.Running(0, 0)
        AppM3ColorCache.preprocessAppColorSchemes(
            context = context,
            paletteStyle = PaletteStyle.TonalSpot,
            colorSpec = ThemeColorSpec.SPEC_2021,
            appColorSchemes = appColorSchemes,
            reduceWhiteBg = reduceWhiteBg,
            onProgress = { computed, total ->
                _state.value = PreprocessState.Running(computed, total)
            }
        )
        _state.value = PreprocessState.Done
    }
}
