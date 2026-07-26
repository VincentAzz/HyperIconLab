package com.capybara.hypericonlab.modules.icon.viewmodel

import android.graphics.Bitmap
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// 预览协调器
class PreviewCoordinator(
    private val scope: CoroutineScope,
    private val generatePreviewUseCase: GeneratePreviewUseCase,
    private val configProvider: () -> IconConfigState,
    private val wallpaperBitmapProvider: () -> Bitmap?,
    private val wallpaperColorSchemeProvider: () -> MonetColorExtractor.WallpaperColorScheme?,
    private val appColorSchemesProvider: () -> Map<String, Pair<String, String>>,
    private val isRunningProvider: () -> Boolean,
    private val onLog: (String, LogType) -> Unit,
    private val onStatusTextChange: (String) -> Unit,
    private val onProgressChange: (Float) -> Unit,
    private val onRunningChange: (Boolean) -> Unit
) {
    // store 预览图（用于固定预览区，5:3 长宽比）
    private val _storePreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val storePreviewBitmap: StateFlow<Bitmap?> = _storePreviewBitmap.asStateFlow()

    // main 预览图（用于全屏预览）
    private val _mainPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val mainPreviewBitmap: StateFlow<Bitmap?> = _mainPreviewBitmap.asStateFlow()

    // 预览生成协程句柄，新请求会取消旧任务
    private var previewJob: Job? = null

    // 实时预览：300ms 防抖
    fun generateLivePreview() {
        previewJob?.cancel()
        previewJob = scope.launch {
            delay(300.milliseconds)
            generatePreview(isLive = true)
        }
    }

    // 手动刷新：等同 generateLivePreview
    fun refreshPreview() {
        generateLivePreview()
    }

    // 生成预览图。isLive=true 为实时预览（不修改运行/进度状态），false 为大预览图
    fun generatePreview(isLive: Boolean = false) {
        if (isRunningProvider() && !isLive) return
        if (!isLive) onRunningChange(true)

        val typeStr = if (isLive) "实时预览" else "大预览图"
        val startTime = System.currentTimeMillis()

        previewJob?.cancel()
        previewJob = scope.launch {
            try {
                if (!isLive) onStatusTextChange("正在加载图标...")
                val result = generatePreviewUseCase.execute(
                    config = configProvider(),
                    wallpaperBitmap = wallpaperBitmapProvider(),
                    wallpaperColorScheme = wallpaperColorSchemeProvider(),
                    appColorSchemes = appColorSchemesProvider(),
                    onStorePreviewGenerated = { _storePreviewBitmap.value = it }
                )
                _mainPreviewBitmap.value = result
                val duration = System.currentTimeMillis() - startTime
                if (result == null) {
                    if (!isLive) {
                        onStatusTextChange("预览错误")
                        onRunningChange(false)
                    }
                    onLog("生成 $typeStr 失败：资源未就绪", LogType.ERROR)
                } else if (!isLive) {
                    onStatusTextChange("预览已生成。")
                    onProgressChange(1.0f)
                    onRunningChange(false)
                    onLog("生成 $typeStr 成功，耗时 ${duration}ms", LogType.SUCCESS)
                } else {
                    onLog("生成 $typeStr 成功，耗时 ${duration}ms", LogType.INFO)
                }
            } catch (e: Exception) {
                if (!isLive) {
                    onStatusTextChange("预览错误")
                    onRunningChange(false)
                    onLog("生成 $typeStr 失败: ${e.message}", LogType.ERROR)
                }
            }
        }
    }
}
