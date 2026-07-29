package com.capybara.hypericonlab.modules.icon.viewmodel

import android.content.Context
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.modules.icon.domain.render.AppM3ColorCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// App-M3 预处理管理器：管理预处理协程和进度状态
// 预处理时遍历 appColorSchemes 全部配置，提取唯一色并计算 M3 scheme，实时持久化
// 中断后已处理部分保留在文件中，下次继续时跳过已处理项
class AppM3PreprocessManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val appColorSchemesProvider: () -> Map<String, Pair<String, String>>
) {
    // 预处理状态
    sealed class PreprocessState {
        // 空闲：未触发预处理
        data object Idle : PreprocessState()

        // 进行中：computed/total 表示进度
        data class Running(val computed: Int, val total: Int) : PreprocessState()

        // 已完成
        data object Done : PreprocessState()
    }

    private val _state = MutableStateFlow<PreprocessState>(PreprocessState.Idle)
    val state: StateFlow<PreprocessState> = _state.asStateFlow()

    // 当前预处理协程
    private var preprocessJob: Job? = null

    // 启动预处理
    // reduceWhiteBg 启用时，白色背景改用前景色作为种子色（与运行时逻辑一致）
    fun startPreprocess(reduceWhiteBg: Boolean = true) {
        if (preprocessJob?.isActive == true) return

        val appColorSchemes = appColorSchemesProvider()
        if (appColorSchemes.isEmpty()) {
            _state.value = PreprocessState.Done
            return
        }

        _state.value = PreprocessState.Running(0, 0)
        preprocessJob = scope.launch(Dispatchers.Default) {
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

    // 取消预处理
    fun cancelPreprocess() {
        preprocessJob?.cancel()
        preprocessJob = null
        _state.value = PreprocessState.Idle
    }
}
