package com.capybara.hypericonlab.modules.icon.ui.page.custom

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capybara.hypericonlab.core.color.AppColorSchemesLoader
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.image.BgImageDir
import com.capybara.hypericonlab.core.image.BgImageLoader
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.domain.model.CtcUiState
import com.capybara.hypericonlab.modules.icon.domain.model.GlassUiState
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.ImageFillingUiState
import com.capybara.hypericonlab.modules.icon.domain.model.PresetUiState
import com.capybara.hypericonlab.modules.icon.domain.model.StickerConfig
import com.capybara.hypericonlab.modules.icon.domain.model.StickerUiState
import com.capybara.hypericonlab.modules.icon.domain.model.WallpaperUiState
import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

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

@SuppressLint("MissingPermission")
class IconViewModel(
    application: Application,
    private val manageResourcesUseCase: ManageResourcesUseCase,
    private val generatePreviewUseCase: GeneratePreviewUseCase,
    private val pipeline: IconPipelineUseCase
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Configuration State
    private val _config = MutableStateFlow(IconConfigState())
    val config: StateFlow<IconConfigState> = _config.asStateFlow()

    val selectedTab =
        _config.map { it.selectedTab }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val fgStyle = _config.map { it.fgStyle }.stateIn(viewModelScope, SharingStarted.Eagerly, "line")
    val fgColorSource = _config.map { it.fgColorSource }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "wallpaper")
    val fgColor =
        _config.map { it.fgColor }.stateIn(viewModelScope, SharingStarted.Eagerly, "#FFFFFFFF")
    val bgColor =
        _config.map { it.bgColor }.stateIn(viewModelScope, SharingStarted.Eagerly, "#FF3F51B5")
    val strokeWidthRatio =
        _config.map { it.strokeWidthRatio }.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)
    val iconScale =
        _config.map { it.iconScale }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.75f)
    val bgColorSource = _config.map { it.bgColorSource }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "wallpaper")
    val bgStyle =
        _config.map { it.bgStyle }.stateIn(viewModelScope, SharingStarted.Eagerly, "solid")
    val selectedMasks = _config.map { it.selectedMasks }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf("m3_round"))
    val previewThemeMode =
        _config.map { it.previewThemeMode }.stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    val sticker = _config.map { it.sticker }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        StickerUiState()
    )
    val glass = _config.map { it.glass }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        GlassUiState()
    )
    val ctc = _config.map { it.ctc }.stateIn(viewModelScope, SharingStarted.Eagerly, CtcUiState())
    val preset = _config.map { it.preset }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        PresetUiState()
    )
    val wallpaperConfig = _config.map { it.wallpaper }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        WallpaperUiState()
    )
    val imageFilling = _config.map { it.imageFilling }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        ImageFillingUiState()
    )
    val selectedStaticImages = _config.map { it.selectedStaticImages }.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    val selectedFillingImages = _config.map { it.selectedFillingImages }.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    // UI Status State
    private val _statusText = MutableStateFlow("就绪")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _currentProgress = MutableStateFlow(0f)
    val currentProgress: StateFlow<Float> = _currentProgress.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showProgress = MutableStateFlow(false)
    val showProgress: StateFlow<Boolean> = _showProgress.asStateFlow()

    private val _isCountdownActive = MutableStateFlow(false)
    val isCountdownActive: StateFlow<Boolean> = _isCountdownActive.asStateFlow()

    private val _countdownProgress = MutableStateFlow(1f)
    val countdownProgress: StateFlow<Float> = _countdownProgress.asStateFlow()

    // Data State

    val wallpaperBitmap = MutableStateFlow<Bitmap?>(null)
    private val wallpaperColorScheme =
        MutableStateFlow<MonetColorExtractor.WallpaperColorScheme?>(null)

    val mapperExists = MutableStateFlow(false)
    val useStreaming = MutableStateFlow(true)
    private val _lastPackDuration = MutableStateFlow<Long?>(null)
    val lastPackDuration: StateFlow<Long?> = _lastPackDuration.asStateFlow()

    // Previews
    private val _storePreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val storePreviewBitmap: StateFlow<Bitmap?> = _storePreviewBitmap.asStateFlow()

    private val _mainPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val mainPreviewBitmap: StateFlow<Bitmap?> = _mainPreviewBitmap.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var previewJob: kotlinx.coroutines.Job? = null
    private var appColorSchemes: Map<String, Pair<String, String>> = emptyMap()

    init {
        viewModelScope.launch {
            mapperExists.value = manageResourcesUseCase.checkMapperExists()
            autoInitializeResources()
        }
        loadColorSchemes()
        observeRunningState()
        observeConfigChanges()
        loadDefaultWallpaper()
    }

    private fun addLog(message: String, type: LogType = LogType.INFO) {
        val durationRegex = "[,，]?\\s*耗时\\s*(\\d+ms)".toRegex()
        val match = durationRegex.find(message)
        val (finalMessage, duration) = if (match != null) {
            val d = match.groupValues[1]
            val m = message.replace(durationRegex, "").trim()
            m to d
        } else {
            message to null
        }
        _logs.value += LogEntry(finalMessage, type = type, duration = duration)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    private suspend fun autoInitializeResources() {
        viewModelScope.launch(Dispatchers.IO) {
            val lawniconsBase = File(context.filesDir, "lawnicons")

            if (!lawniconsBase.exists() || lawniconsBase.list()?.isEmpty() == true) {
                addLog("检测到资源未初始化，开始自动解压...")
                val startTime = System.currentTimeMillis()
                try {
                    manageResourcesUseCase.performUnzip { /* silent progress */ }
                    val duration = System.currentTimeMillis() - startTime
                    addLog("资源解压完成，耗时 ${duration}ms", LogType.SUCCESS)
                } catch (e: Exception) {
                    addLog("资源解压失败: ${e.message}", LogType.ERROR)
                }
            } else {
                addLog("资源已就绪")
            }

            if (!manageResourcesUseCase.checkMapperExists()) {
                addLog("检测到映射器未生成，开始自动生成...")
                val startTime = System.currentTimeMillis()
                val result = manageResourcesUseCase.generateMapper()
                val duration = System.currentTimeMillis() - startTime
                result.onSuccess {
                    addLog("映射器生成完成，耗时 ${duration}ms", LogType.SUCCESS)
                    mapperExists.value = true
                }.onFailure {
                    addLog("映射器生成失败: ${it.message}", LogType.ERROR)
                }
            } else {
                addLog("映射器已就绪")
                mapperExists.value = true
            }

            // 资源和映射就绪后，自动生成初始预览图
            if (mapperExists.value) {
                addLog("自动生成初始预览图...")
                generateLivePreview()
            }
        }
    }

    private fun loadColorSchemes() {
        viewModelScope.launch(Dispatchers.IO) {
            appColorSchemes = AppColorSchemesLoader.loadFromAssets(context)
        }
    }

    private fun observeRunningState() {
        viewModelScope.launch {
            isRunning.collect { running ->
                if (!running && currentProgress.value >= 1f) {
                    _isCountdownActive.value = true
                    val startTime = System.currentTimeMillis()
                    val duration = 3000L
                    while (System.currentTimeMillis() - startTime < duration) {
                        _countdownProgress.value =
                            1f - (System.currentTimeMillis() - startTime).toFloat() / duration
                        delay(16.milliseconds)
                    }
                    _isCountdownActive.value = false
                    _showProgress.value = false
                    _currentProgress.value = 0f
                } else if (running) {
                    _showProgress.value = true
                    _isCountdownActive.value = false
                }
            }
        }
    }

    private fun observeConfigChanges() {
        viewModelScope.launch {
            _config.collect { config ->
                // Sync logic
                if (config.fgStyle != "sticker" && config.fgColorSource == "black_white") {
                    updateConfig { it.copy(fgColorSource = "wallpaper") }
                }
                if (config.fgStyle == "sticker" && config.fgColorSource == "black_white" && config.sticker.fillStyle == "none") {
                    updateConfig { it.copy(sticker = it.sticker.copy(fillStyle = "fill")) }
                }
                if (config.fgStyle == "hollow" && config.bgStyle == "none") {
                    updateConfig { it.copy(bgStyle = "solid", bgColorSource = "wallpaper") }
                }
                if (config.bgStyle == "static" && config.selectedStaticImages.isEmpty()) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.STATIC)
                    if (presets.isNotEmpty()) {
                        updateConfig { it.copy(selectedStaticImages = listOf(presets.first())) }
                    }
                }
                if (config.bgStyle == "image" && config.selectedFillingImages.isEmpty()) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.FILLING)
                    if (presets.isNotEmpty()) {
                        updateConfig { it.copy(selectedFillingImages = listOf(presets.first())) }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.Default) {
            _config
                .map { it.wallpaper }
                .distinctUntilChanged()
                .collect { reextractWallpaperColors() }
        }
        // 仅在业务字段变化时触发，忽略selectedTab变化
        viewModelScope.launch {
            _config
                .map { it.copy(selectedTab = 0) }
                .distinctUntilChanged()
                .collect { generateLivePreview() }
        }
    }

    fun updateConfig(update: (IconConfigState) -> IconConfigState) {
        _config.value = update(_config.value)
    }

    // 切换到自定义颜色时，继承当前实际使用的颜色作为初始值
    fun switchToCustomColor(isFg: Boolean) {
        val config = _config.value
        val resolvedColor = generatePreviewUseCase.resolveConfigColors(
            isFg = isFg,
            config = config,
            wallpaperColorScheme = wallpaperColorScheme.value,
            appColorSchemes = appColorSchemes
        )
        updateConfig {
            if (isFg) it.copy(fgColorSource = "custom", fgColor = resolvedColor)
            else it.copy(bgColorSource = "custom", bgColor = resolvedColor)
        }
    }


    /**
     * 图片背景选择确认回调。
     * @param isStatic true=静态图片，false=图片填充
     * @param images 新的图片引用列表
     * @param deletedRefs 被删除的自选图片引用列表（用于清理磁盘文件）
     */
    fun confirmImageSelection(isStatic: Boolean, images: List<String>, deletedRefs: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            // 清理被删除的自选图片磁盘文件
            deletedRefs.forEach { ref ->
                BgImageLoader.deleteCustomFile(context, ref)
            }
        }
        updateConfig {
            if (isStatic) it.copy(selectedStaticImages = images)
            else it.copy(selectedFillingImages = images)
        }
    }

    fun generateLivePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            delay(300.milliseconds)
            generatePreview(isLive = true)
        }
    }

    fun refreshPreview() {
        generateLivePreview()
    }

    fun generatePreview(isLive: Boolean = false) {
        if (_isRunning.value && !isLive) return
        if (!isLive) _isRunning.value = true

        val typeStr = if (isLive) "实时预览" else "大预览图"
        val startTime = System.currentTimeMillis()

        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            try {
                if (!isLive) _statusText.value = "正在加载图标..."
                val result = generatePreviewUseCase.execute(
                    config = _config.value,
                    wallpaperBitmap = wallpaperBitmap.value,
                    wallpaperColorScheme = wallpaperColorScheme.value,
                    appColorSchemes = appColorSchemes,
                    onStorePreviewGenerated = { _storePreviewBitmap.value = it }
                )
                _mainPreviewBitmap.value = result
                val duration = System.currentTimeMillis() - startTime
                if (!isLive) {
                    _statusText.value = "预览已生成。"
                    _currentProgress.value = 1.0f
                    _isRunning.value = false
                    addLog("生成 $typeStr 成功，耗时 ${duration}ms", LogType.SUCCESS)
                } else {
                    addLog("生成 $typeStr 成功，耗时 ${duration}ms", LogType.INFO)
                }
            } catch (e: Exception) {
                if (!isLive) {
                    _statusText.value = "预览错误"
                    _isRunning.value = false
                    addLog("生成 $typeStr 失败: ${e.message}", LogType.ERROR)
                }
            }
        }
    }

    fun unzipResources() {
        if (_isRunning.value) return
        _isRunning.value = true
        addLog("开始手动解压资源...")
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                _statusText.value = "正在解压资源..."
                manageResourcesUseCase.performUnzip { _currentProgress.value = it }
                _statusText.value = "资源就绪。"
                _currentProgress.value = 1.0f
                val duration = System.currentTimeMillis() - startTime
                addLog("手动解压完成，耗时 ${duration}ms", LogType.SUCCESS)
            } catch (e: Exception) {
                _statusText.value = "解压错误"
                addLog("手动解压失败: ${e.message}", LogType.ERROR)
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun generateMapper() {
        if (_isRunning.value) return
        addLog("开始手动生成映射器...")
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            _isRunning.value = true
            _statusText.value = "正在生成映射器..."
            val result = manageResourcesUseCase.generateMapper()
            val duration = System.currentTimeMillis() - startTime
            result.onSuccess {
                _statusText.value = "映射器已就绪。"
                _currentProgress.value = 1.0f
                mapperExists.value = true
                addLog("手动生成映射器成功，耗时 ${duration}ms", LogType.SUCCESS)
            }.onFailure {
                _statusText.value = "错误：${it.message}"
                addLog("手动生成映射器失败: ${it.message}", LogType.ERROR)
            }
            _isRunning.value = false
        }
    }

    fun runPipeline(mapperName: String) {
        if (_isRunning.value) return
        _isRunning.value = true
        addLog("开始打包: $mapperName")
        val startTime = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                _statusText.value = "准备 $mapperName..."
                val filesDir = context.filesDir
                val mapperBase = File(filesDir, "icon_mapper")
                val lawniconsBase = File(filesDir, "lawnicons")

                val mapperFile = withContext(Dispatchers.IO) {
                    ZipUtils.findFileRecursive(
                        mapperBase,
                        mapperName
                    )
                }
                val svgDir =
                    withContext(Dispatchers.IO) { ZipUtils.findDirRecursive(lawniconsBase, "svgs") }
                val maskBitmaps = _config.value.selectedMasks.mapNotNull { name ->
                    try {
                        context.assets.open("masks/mask_${name}_512.png")
                            .use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) {
                        null
                    }
                }

                val configValue = _config.value
                val buildConfig = IconBuildConfig(
                    fgColorHex = configValue.fgColor,
                    bgColorHex = configValue.bgColor,
                    strokeWidthRatio = configValue.strokeWidthRatio,
                    iconScale = configValue.iconScale,
                    colorMode = configValue.colorMode,
                    useStreaming = useStreaming.value,
                    masks = configValue.selectedMasks,
                    fgStyle = configValue.fgStyle,
                    bgStyle = configValue.bgStyle,
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
                    imageFillingScaleMode = configValue.imageFilling.scaleMode
                )

                val out = File(filesDir, "${mapperName.removeSuffix(".xml")}.mtz")
                pipeline.executeWithFiles(
                    buildConfig,
                    IconMapperProcessor.parseIconMapper(mapperFile!!),
                    svgDir!!,
                    maskBitmaps,
                    out,
                    appColorSchemes
                )
                    .collect { state ->
                        when (state) {
                            is IconPipelineUseCase.PipelineProgress.Processing -> {
                                _statusText.value = "打包中: ${state.packageName}"
                                _currentProgress.value = state.current.toFloat() / state.total
                            }

                            is IconPipelineUseCase.PipelineProgress.Complete -> {
                                val duration = System.currentTimeMillis() - startTime
                                _lastPackDuration.value = duration
                                _statusText.value = "已保存到 ${out.name} (${duration}ms)"
                                _currentProgress.value = 1.0f
                                _isRunning.value = false
                                maskBitmaps.forEach { it.recycle() }
                                addLog(
                                    "打包完成: ${out.name}，总耗时 ${duration}ms",
                                    LogType.SUCCESS
                                )
                            }

                            else -> {}
                        }
                    }
            } catch (e: Exception) {
                _statusText.value = "发生错误"
                _isRunning.value = false
                addLog("打包失败 ($mapperName): ${e.message}", LogType.ERROR)
            }
        }
    }

    private fun loadDefaultWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.assets.open("wallpapers/wallpaper.jpg").use {
                    BitmapFactory.decodeStream(it)?.let { bmp -> updateWallpaper(bmp) }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateWallpaperFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)?.let { bmp -> updateWallpaper(bmp) }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun updateWallpaper(bmp: Bitmap) {
        wallpaperBitmap.value = bmp
        reextractWallpaperColors()
        generateLivePreview()
    }


    private fun reextractWallpaperColors() {
        val bmp = wallpaperBitmap.value ?: return
        val wp = _config.value.wallpaper
        wallpaperColorScheme.value = MonetColorExtractor.extractFromBitmap(
            bitmap = bmp,
            paletteStyle = wp.paletteStyle,
            colorSpec = wp.colorSpec
        )
    }
}
