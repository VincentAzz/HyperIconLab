package com.capybara.hypericonlab.modules.icon.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.ContextCompat
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.image.InnerShadowBitmapLoader
import com.capybara.hypericonlab.core.image.MaskAssetLoader
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.InnerShadowConfig
import com.capybara.hypericonlab.modules.icon.domain.model.StickerConfig
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTask
import com.capybara.hypericonlab.modules.iconpack.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.iconpack.domain.model.ProductType
import com.capybara.hypericonlab.modules.iconpack.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.iconrender.ConfigColorResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 构建任务协调器：管理打包流程、任务提交/重试/取消/删除、权限检查
// 通过 provider 回调解耦对 ViewModel 配置/壁纸/预览/运行状态的读取，
// 通过 onLog/onStatusTextChange/onProgressChange/onRunningChange/onLastPackDurationChange 回调解耦状态写回，
// 通过 onConfigSwap/onRegeneratePreview 回调解耦 retryBuildTask 的配置切换与预览重生成
// 资源读取统一通过 LawniconsAssetFacade，支持 assets/云端来源切换
class BuildTaskCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val pipeline: IconPipelineUseCase,
    private val buildTaskManager: BuildTaskManager,
    private val assetsFacade: LawniconsAssetFacade,
    private val configProvider: () -> IconConfigState,
    private val wallpaperBitmapProvider: () -> Bitmap?,
    private val wallpaperColorSchemeProvider: () -> MonetColorExtractor.WallpaperColorScheme?,
    private val appColorSchemesProvider: () -> Map<String, Pair<String, String>>,
    private val useStreamingProvider: () -> Boolean,
    private val storePreviewProvider: () -> Bitmap?,
    private val mainPreviewProvider: () -> Bitmap?,
    private val isRunningProvider: () -> Boolean,
    private val onLog: (String, LogType) -> Unit,
    private val onStatusTextChange: (String) -> Unit,
    private val onProgressChange: (Float) -> Unit,
    private val onRunningChange: (Boolean) -> Unit,
    private val onLastPackDurationChange: (Long) -> Unit,
    private val onConfigSwap: (IconConfigState) -> Unit,
    private val onRegeneratePreview: () -> Unit
) {
    // 转发 BuildTaskManager 的任务列表
    val activeBuildTasks: StateFlow<List<BuildTask>> = buildTaskManager.activeTasks
    val finishedBuildTasks: StateFlow<List<BuildTask>> = buildTaskManager.finishedTasks

    // 读取当前资源的 mapper 并执行 pipeline
    fun runPipeline(mapperName: String) {
        if (isRunningProvider()) return
        onRunningChange(true)
        onLog("开始打包: $mapperName", LogType.INFO)
        val startTime = System.currentTimeMillis()
        scope.launch {
            try {
                onStatusTextChange("准备 $mapperName...")
                val filesDir = context.filesDir
                val provider = assetsFacade.getProvider()

                val mapperMap = withContext(Dispatchers.IO) {
                    provider.openIconMapper(mapperName)
                        .use { stream -> IconMapperProcessor.parseIconMapper(stream) }
                }
                val svgDir = withContext(Dispatchers.IO) { provider.getSvgDir() }
                val configValue = configProvider()
                val maskBitmaps = configValue.selectedMasks.mapNotNull { name ->
                    MaskAssetLoader.loadBitmap(context, name)
                }

                // 下层 mask bitmaps（仅双层启用时加载）
                val maskBitmaps2 = if (configValue.dualLayerEnabled) {
                    configValue.bgLayer2.selectedMasks.mapNotNull { name ->
                        MaskAssetLoader.loadBitmap(context, name)
                    }
                } else emptyList()
                val buildConfig = buildIconBuildConfig(configValue)

                // 内阴影 bitmap（仅单层背景且启用内阴影时加载，加载后预合并多层强度）
                val innerShadowBitmap =
                    if (buildConfig.innerShadow.enabled && !buildConfig.dualLayerEnabled) {
                        buildConfig.innerShadow.styleName?.let { styleName ->
                            buildConfig.masks.firstOrNull()?.let { shapeName ->
                                InnerShadowBitmapLoader.loadAndMerge(
                                    shapeName,
                                    styleName,
                                    buildConfig.iconSize,
                                    buildConfig.innerShadow.intensityLayers,
                                    context
                                )
                            }
                        }
                    } else null

                val out = File(filesDir, "${mapperName.removeSuffix(".xml")}.mtz")
                pipeline.executeWithFiles(
                    buildConfig,
                    mapperMap,
                    svgDir!!,
                    maskBitmaps,
                    out,
                    appColorSchemesProvider(),
                    maskBitmaps2,
                    innerShadowBitmap
                )
                    .collect { state ->
                        when (state) {
                            is IconPipelineUseCase.PipelineProgress.Processing -> {
                                onStatusTextChange("打包中: ${state.packageName}")
                                onProgressChange(state.current.toFloat() / state.total)
                            }

                            is IconPipelineUseCase.PipelineProgress.Complete -> {
                                val duration = System.currentTimeMillis() - startTime
                                onLastPackDurationChange(duration)
                                onStatusTextChange("已保存到 ${out.name} (${duration}ms)")
                                onProgressChange(1.0f)
                                onRunningChange(false)
                                maskBitmaps.forEach { it.recycle() }
                                maskBitmaps2.forEach { it.recycle() }
                                onLog(
                                    "打包完成: ${out.name}，总耗时 ${duration}ms",
                                    LogType.SUCCESS
                                )
                            }

                            else -> {}
                        }
                    }
            } catch (e: Exception) {
                onStatusTextChange("发生错误")
                onRunningChange(false)
                onLog("打包失败 ($mapperName): ${e.message}", LogType.ERROR)
            }
        }
    }

    // 基于当前 UI 配置构造 IconBuildConfig（用于调试展示）
    fun buildCurrentConfig(): IconBuildConfig = buildIconBuildConfig(configProvider())

    // 构造 IconBuildConfig
    private fun buildIconBuildConfig(configValue: IconConfigState): IconBuildConfig {
        val wallpaperColorScheme = wallpaperColorSchemeProvider()
        val appColorSchemes = appColorSchemesProvider()

        val resolvedFgColor =
            if (configValue.fgColorSource != "app" && configValue.fgColorSource != "app_m3") {
            ConfigColorResolver.resolveConfigColors(
                isFg = true,
                config = configValue,
                wallpaperColorScheme = wallpaperColorScheme,
                appColorSchemes = appColorSchemes
            )
        } else configValue.fgColor

        val resolvedBgColor =
            if (configValue.bgColorSource != "app" && configValue.bgColorSource != "app_m3") {
            ConfigColorResolver.resolveConfigColors(
                isFg = false,
                config = configValue,
                wallpaperColorScheme = wallpaperColorScheme,
                appColorSchemes = appColorSchemes
            )
        } else configValue.bgColor

        val resolvedBgColor2 = if (configValue.dualLayerEnabled &&
            configValue.bgLayer2.colorSource != "app" && configValue.bgLayer2.colorSource != "app_m3"
        ) {
            ConfigColorResolver.resolveConfigColors(
                isFg = false,
                config = configValue,
                wallpaperColorScheme = wallpaperColorScheme,
                appColorSchemes = appColorSchemes,
                layerIndex = 1
            )
        } else configValue.bgLayer2.color

        return IconBuildConfig(
            fgColorHex = resolvedFgColor,
            bgColorHex = resolvedBgColor,
            strokeWidthRatio = configValue.strokeWidthRatio,
            iconScale = configValue.iconScale,
            colorMode = configValue.colorMode,
            useStreaming = useStreamingProvider(),
            masks = configValue.selectedMasks,
            fgStyle = configValue.fgStyle,
            bgStyle = configValue.bgStyle,
            fgColorSource = configValue.fgColorSource,
            bgColorSource = configValue.bgColorSource,
            stickerConfig = if (configValue.fgStyle == "sticker") StickerConfig(
                fillStyle = configValue.sticker.fillStyle,
                strokeWidth = configValue.sticker.strokeWidth,
                glowIntensity = configValue.sticker.glowIntensity,
                lineColor = configValue.sticker.lineColor,
                fillColor = configValue.sticker.fillColor
            ) else null,
            selectedStaticImages = configValue.selectedStaticImages,
            selectedFillingImages = configValue.selectedFillingImages,
            imageFillingRandomRotation = configValue.imageFilling.randomRotation,
            imageFillingScaleMode = configValue.imageFilling.scaleMode,
            dualLayerEnabled = configValue.dualLayerEnabled,
            dualLayerSizeDiff = configValue.dualLayerSizeDiff,
            bgStyle2 = configValue.bgLayer2.style,
            bgColor2 = resolvedBgColor2,
            bgColorSource2 = configValue.bgLayer2.colorSource,
            previewThemeMode = configValue.previewThemeMode,
            bgPreviewThemeMode2 = configValue.bgLayer2.previewThemeMode,
            bgLayer2Alpha = configValue.bgLayer2.alpha,
            selectedMasks2 = configValue.bgLayer2.selectedMasks,
            selectedStaticImages2 = configValue.bgLayer2.selectedStaticImages,
            selectedFillingImages2 = configValue.bgLayer2.selectedFillingImages,
            imageFilling2RandomRotation = configValue.bgLayer2.imageFilling.randomRotation,
            imageFilling2ScaleMode = configValue.bgLayer2.imageFilling.scaleMode,
            innerShadow = InnerShadowConfig(
                enabled = configValue.innerShadow.enabled && !configValue.dualLayerEnabled,
                styleName = configValue.innerShadow.styleName,
                intensityLayers = configValue.innerShadow.intensityLayers
            ),
            appReduceWhiteBg = configValue.appReduceWhiteBg,
            bgLayer2AppReduceWhiteBg = configValue.bgLayer2.appReduceWhiteBg
        )
    }

    // 提交构建任务
    fun submitBuildTask(
        productType: ProductType,
        iconSetId: String,
        iconSetLabel: String
    ): BuildTask? {
        val storePreview = storePreviewProvider() ?: run {
            onLog("提交失败：store 预览图未就绪", LogType.ERROR)
            return null
        }
        val mainPreview = mainPreviewProvider() ?: run {
            onLog("提交失败：main 预览图未就绪", LogType.ERROR)
            return null
        }

        val configValue = configProvider()
        val buildConfig = buildIconBuildConfig(configValue)
        val wallpaperUri = wallpaperBitmapProvider()?.let { "embedded:wallpaper" }

        return buildTaskManager.submit(
            config = buildConfig,
            configSnapshot = configValue,
            productType = productType,
            iconSetId = iconSetId,
            iconSetLabel = iconSetLabel,
            iconCount = BuildTaskConfig.PLACEHOLDER_ICON_COUNT,
            wallpaperUri = wallpaperUri,
            storePreview = storePreview,
            mainPreview = mainPreview
        ).also {
            onLog("已提交构建任务: ${it.taskId}", LogType.INFO)
        }
    }

    // 重试失败任务
    fun retryBuildTask(originalTaskId: String): BuildTask? {
        val original = finishedBuildTasks.value.find { it.taskId == originalTaskId } ?: return null
        if (original.status != BuildTaskStatus.FAILED) return null

        onConfigSwap(original.configSnapshot)
        onRegeneratePreview()

        val storePreview = storePreviewProvider()
        val mainPreview = mainPreviewProvider()

        onConfigSwap(configProvider())
        onRegeneratePreview()

        if (storePreview == null || mainPreview == null) {
            onLog("重试失败：预览图未就绪，请稍后再试", LogType.ERROR)
            return null
        }

        return buildTaskManager.retry(
            originalTaskId = originalTaskId,
            storePreview = storePreview,
            mainPreview = mainPreview
        )?.also {
            onLog("已重试任务: ${it.taskId}", LogType.INFO)
        }
    }

    // 取消任务
    fun cancelBuildTask(taskId: String) {
        buildTaskManager.cancel(taskId)
        onLog("已取消任务: $taskId", LogType.INFO)
    }

    // 删除已完成任务
    fun deleteFinishedBuildTask(taskId: String) {
        buildTaskManager.deleteFinished(taskId)
        onLog("已删除任务: $taskId", LogType.INFO)
    }

    // 权限检查
    fun buildPermissionsMissing(): List<String> {
        val missing = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) missing.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) missing.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return missing
    }

    companion object {
        private object BuildTaskConfig {
            const val PLACEHOLDER_ICON_COUNT = 0
        }
    }
}
