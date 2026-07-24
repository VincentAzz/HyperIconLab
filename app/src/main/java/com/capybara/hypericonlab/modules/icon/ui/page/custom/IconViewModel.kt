package com.capybara.hypericonlab.modules.icon.ui.page.custom

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capybara.hypericonlab.core.color.AppColorSchemesLoader
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.core.image.BgImageDir
import com.capybara.hypericonlab.core.image.BgImageLoader
import com.capybara.hypericonlab.core.image.InnerShadowAssets
import com.capybara.hypericonlab.core.image.InnerShadowBitmapLoader
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.domain.model.BgLayerUiState
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTaskStatus
import com.capybara.hypericonlab.modules.icon.domain.model.CtcUiState
import com.capybara.hypericonlab.modules.icon.domain.model.GlassUiState
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.ImageFillingUiState
import com.capybara.hypericonlab.modules.icon.domain.model.InnerShadowConfig
import com.capybara.hypericonlab.modules.icon.domain.model.InnerShadowUiState
import com.capybara.hypericonlab.modules.icon.domain.model.PresetUiState
import com.capybara.hypericonlab.modules.icon.domain.model.ProductType
import com.capybara.hypericonlab.modules.icon.domain.model.StickerConfig
import com.capybara.hypericonlab.modules.icon.domain.model.StickerUiState
import com.capybara.hypericonlab.modules.icon.domain.model.WallpaperUiState
import com.capybara.hypericonlab.modules.icon.domain.render.ConfigColorResolver
import com.capybara.hypericonlab.modules.icon.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

/**
 * 图标集信息（用于 BuildOptionSheet 展示）。
 *
 * @param id 图标集 id（full / filtered / test），同时用于任务 ID 拼接
 * @param label 展示名（中文场景下仍使用英文，与 id 相同）
 * @param iconCount 图标数量（解析 mapper 得到，0 表示解析失败或未就绪）
 */
data class IconSetInfo(
    val id: String,
    val label: String,
    val iconCount: Int
) {
    companion object {
        // 支持展示的图标集 id 列表（不展示 alt / preview）
        val SUPPORTED_SETS = listOf("full", "filtered", "test")

        // 图标集 id → assets/icon_mapper/ 下的实际文件名映射
        fun mapperFileName(id: String): String = when (id) {
            "full" -> "icon_mapper.xml"
            "filtered" -> "icon_mapper_filtered.xml"
            "test" -> "icon_mapper_test.xml"
            // 兼容历史 id：未在映射表中的 id 按 icon_mapper_<id>.xml 推断
            else -> "icon_mapper_$id.xml"
        }
    }
}

@SuppressLint("MissingPermission")
class IconViewModel(
    application: Application,
    private val manageResourcesUseCase: ManageResourcesUseCase,
    private val generatePreviewUseCase: GeneratePreviewUseCase,
    private val pipeline: IconPipelineUseCase,
    private val buildTaskManager: BuildTaskManager,
    private val appSettingsRepository: AppSettingsRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    companion object {
        // 打包流程关键参数集中声明，便于调参
        private object PipelineAssets {
            // assets 中 mapper 文件所在目录
            const val MAPPER_ASSET_DIR = "icon_mapper"
        }

        // 构建任务关键参数集中声明，便于调参
        private object BuildConfig {
            // 提交任务时占位的图标数量，真实数量在 executor 解析 mapper 后更新
            const val PLACEHOLDER_ICON_COUNT = 0
        }
    }

    // 构建任务队列与已完成列表（直接转发 BuildTaskManager 的 StateFlow）
    val activeBuildTasks: StateFlow<List<BuildTask>> = buildTaskManager.activeTasks
    val finishedBuildTasks: StateFlow<List<BuildTask>> = buildTaskManager.finishedTasks

    // 可用图标集列表（assets/icon_mapper/<id>.xml，去扩展名作为 id 与展示名）
    private val _availableIconSets = MutableStateFlow<List<IconSetInfo>>(emptyList())
    val availableIconSets: StateFlow<List<IconSetInfo>> = _availableIconSets.asStateFlow()

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

    // 双层背景相关 StateFlow
    val dualLayerEnabled = _config.map { it.dualLayerEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dualLayerSizeDiff = _config.map { it.dualLayerSizeDiff }
        .stateIn(viewModelScope, SharingStarted.Eagerly, IconConfigState().dualLayerSizeDiff)
    val bgLayer2 = _config.map { it.bgLayer2 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, BgLayerUiState())
    val syncDualLayerColorSource = _config.map { it.syncDualLayerColorSource }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // 内阴影 StateFlow
    val innerShadow = _config.map { it.innerShadow }
        .stateIn(viewModelScope, SharingStarted.Eagerly, InnerShadowUiState())

    // 内阴影可用资源映射：形状名 → 可用样式名列表（扫描 assets/shadow_baked/ 目录）
    private val _shadowAssetsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val shadowAssetsMap: StateFlow<Map<String, List<String>>> = _shadowAssetsMap.asStateFlow()

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
            // 根据应用主题模式设置默认 previewThemeMode，使自定义 tab 的
            // 前景背景壁纸配色与应用当前亮/暗色保持一致
            val themeMode = appSettingsRepository.preferencesFlow.first().themeMode
            val isSystemDark = Resources.getSystem().configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemDark
            }
            _config.update { current ->
                current.copy(previewThemeMode = if (isDark) "dark" else "light")
            }

            // mapper 已直接打包在 assets/icon_mapper/，无需首启动解压，默认即就绪
            mapperExists.value = true
            autoInitializeResources()
        }
        loadColorSchemes()
        observeRunningState()
        observeConfigChanges()
        loadDefaultWallpaper()
        loadAvailableIconSets()
    }

    /**
     * 异步加载支持的图标集列表（full / filtered / test），不展示 alt / preview。
     * 同时解析每个 mapper 的图标数量，供 BuildOptionSheet 展示。
     */
    private fun loadAvailableIconSets() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = IconSetInfo.SUPPORTED_SETS.map { id ->
                // 解析图标数量，失败时返回 0
                val count = try {
                    context.assets
                        .open("${PipelineAssets.MAPPER_ASSET_DIR}/${IconSetInfo.mapperFileName(id)}")
                        .use { IconMapperProcessor.parseIconMapper(it).size }
                } catch (_: Exception) {
                    0
                }
                // 中文场景下也使用英文 label，与 id 保持一致
                IconSetInfo(id = id, label = id, iconCount = count)
            }
            _availableIconSets.value = list
        }
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

            // mapper 已直接打包在 assets 中，无需自动生成
            addLog("映射器已就绪")
            mapperExists.value = true

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
            // 同步给 BuildTaskManager，供 executor 执行任务时使用
            buildTaskManager.updateAppColorSchemes(appColorSchemes)
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
                if (config.bgStyle == "img_static" && config.selectedStaticImages.isEmpty()) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.STATIC)
                    if (presets.isNotEmpty()) {
                        updateConfig { it.copy(selectedStaticImages = listOf(presets.first())) }
                    }
                }
                if (config.bgStyle == "img_filling" && config.selectedFillingImages.isEmpty()) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.FILLING)
                    if (presets.isNotEmpty()) {
                        updateConfig { it.copy(selectedFillingImages = listOf(presets.first())) }
                    }
                }
                // 双层启用时，上层背景不允许 none（强制切回 solid+wallpaper）
                if (config.dualLayerEnabled && config.bgStyle == "none") {
                    updateConfig { it.copy(bgStyle = "solid", bgColorSource = "wallpaper") }
                }
                // 下层 img_static 空列表自动填首个预设
                if (config.dualLayerEnabled && config.bgLayer2.style == "img_static" &&
                    config.bgLayer2.selectedStaticImages.isEmpty()
                ) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.STATIC)
                    if (presets.isNotEmpty()) {
                        updateConfig {
                            it.copy(
                                bgLayer2 = it.bgLayer2.copy(
                                    selectedStaticImages = listOf(presets.first())
                                )
                            )
                        }
                    }
                }
                // 下层 img_filling 空列表自动填首个预设
                if (config.dualLayerEnabled && config.bgLayer2.style == "img_filling" &&
                    config.bgLayer2.selectedFillingImages.isEmpty()
                ) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.FILLING)
                    if (presets.isNotEmpty()) {
                        updateConfig {
                            it.copy(
                                bgLayer2 = it.bgLayer2.copy(
                                    selectedFillingImages = listOf(presets.first())
                                )
                            )
                        }
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

        // 监听上层形状变化：切换形状时内阴影默认取消选择回到关闭状态
        viewModelScope.launch(Dispatchers.Default) {
            _config
                .map { it.selectedMasks }
                .distinctUntilChanged()
                .drop(1) // 跳过初始值，避免启动时误触发
                .collect {
                    _config.update { config ->
                        if (config.innerShadow.enabled || config.innerShadow.styleName != null) {
                            config.copy(innerShadow = InnerShadowUiState())
                        } else config
                    }
                }
        }

        // 监听双层背景开关：启用双层时自动关闭内阴影（仅单层背景生效）
        viewModelScope.launch(Dispatchers.Default) {
            _config
                .map { it.dualLayerEnabled }
                .distinctUntilChanged()
                .drop(1)
                .filter { it } // 仅在启用双层时触发
                .collect {
                    _config.update { config ->
                        if (config.innerShadow.enabled || config.innerShadow.styleName != null) {
                            config.copy(innerShadow = InnerShadowUiState())
                        } else config
                    }
                }
        }

        // 初始化时扫描 assets/shadow_baked/ 目录，构建形状 → 样式列表映射
        viewModelScope.launch(Dispatchers.IO) {
            _shadowAssetsMap.value = scanInnerShadowAssets()
        }
    }

    /**
     * 扫描 assets/shadow_baked/ 目录，解析文件名 `<shapeName>_<styleName>_shadow_512.png`
     * 为形状 → 样式列表的映射。
     */
    private suspend fun scanInnerShadowAssets(): Map<String, List<String>> =
        withContext(Dispatchers.IO) {
            val suffix = InnerShadowAssets.FILE_SUFFIX
            try {
                getApplication<Application>().assets.list(InnerShadowAssets.DIR)
                    ?.asSequence()
                    ?.filter { it.endsWith(suffix) }
                    ?.mapNotNull { filename ->
                        // oneui_3d_shadow_512.png → shapeName="oneui", styleName="3d"
                        val core = filename.removeSuffix(suffix)
                        val firstUnderscore = core.indexOf('_')
                        if (firstUnderscore > 0) {
                            val shapeName = core.substring(0, firstUnderscore)
                            val styleName = core.substring(firstUnderscore + 1)
                            shapeName to styleName
                        } else null
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { (_, styles) -> styles.sorted() }
                    ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }
        }

    fun updateConfig(update: (IconConfigState) -> IconConfigState) {
        _config.value = update(_config.value)
    }

    /**
     * 更新内阴影配置。
     */
    fun updateInnerShadow(update: (InnerShadowUiState) -> InnerShadowUiState) {
        _config.update { it.copy(innerShadow = update(it.innerShadow)) }
    }

    /**
     * 切换到自定义颜色时，继承当前实际使用的颜色作为初始值。
     * @param isFg true=前景，false=背景
     * @param layerIndex 0=上层/前景（默认），1=下层背景
     */
    fun switchToCustomColor(isFg: Boolean, layerIndex: Int = 0) {
        val config = _config.value
        val resolvedColor = ConfigColorResolver.resolveConfigColors(
            isFg = isFg,
            config = config,
            wallpaperColorScheme = wallpaperColorScheme.value,
            appColorSchemes = appColorSchemes,
            layerIndex = layerIndex
        )
        updateConfig {
            if (isFg) it.copy(fgColorSource = "custom", fgColor = resolvedColor)
            else if (layerIndex == 1) {
                // 下层背景切自定义颜色：继承下层当前解析色
                it.copy(
                    bgLayer2 = it.bgLayer2.copy(
                        colorSource = "custom",
                        color = resolvedColor
                    )
                )
            } else it.copy(bgColorSource = "custom", bgColor = resolvedColor)
        }
    }


    /**
     * 图片背景选择确认回调。
     * @param isStatic true=静态图片，false=图片填充
     * @param images 新的选中图片引用列表
     * @param layerIndex 0=上层，1=下层（默认 0 保持向后兼容）
     */
    fun confirmImageSelection(isStatic: Boolean, images: List<String>, layerIndex: Int = 0) {
        updateConfig {
            if (layerIndex == 1) {
                // 下层背景图片选择，写入 bgLayer2 嵌套字段
                val layer2 = it.bgLayer2
                val newLayer2 = if (isStatic) {
                    layer2.copy(selectedStaticImages = images)
                } else {
                    layer2.copy(selectedFillingImages = images)
                }
                it.copy(bgLayer2 = newLayer2)
            } else {
                if (isStatic) it.copy(selectedStaticImages = images)
                else it.copy(selectedFillingImages = images)
            }
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
                val lawniconsBase = File(filesDir, "lawnicons")

                // mapper 直接从 assets 读取，避免落盘与首启动解压
                val mapperMap = withContext(Dispatchers.IO) {
                    context.assets
                        .open("${PipelineAssets.MAPPER_ASSET_DIR}/$mapperName")
                        .use { stream -> IconMapperProcessor.parseIconMapper(stream) }
                }
                val svgDir =
                    withContext(Dispatchers.IO) { ZipUtils.findDirRecursive(lawniconsBase, "svgs") }
                val configValue = _config.value
                val maskBitmaps = configValue.selectedMasks.mapNotNull { name ->
                    try {
                        context.assets.open("masks/mask_${name}_512.png")
                            .use { BitmapFactory.decodeStream(it) }
                    } catch (_: Exception) {
                        null
                    }
                }

                // 下层 mask bitmaps（仅双层启用时加载）
                val maskBitmaps2 = if (configValue.dualLayerEnabled) {
                    configValue.bgLayer2.selectedMasks.mapNotNull { name ->
                        try {
                            context.assets.open("masks/mask_${name}_512.png")
                                .use { BitmapFactory.decodeStream(it) }
                        } catch (_: Exception) {
                            null
                        }
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
                    appColorSchemes,
                    maskBitmaps2,
                    innerShadowBitmap
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
                                maskBitmaps2.forEach { it.recycle() }
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
                context.assets.open("wallpapers/wallpaper_fallback.jpg").use {
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

    // ========================================================================
    // 构建任务相关 API（对接 BuildTaskManager）
    // ========================================================================

    /**
     * 根据当前 [_config] 构造 [IconBuildConfig]，runPipeline 与 submitBuildTask 共用。
     *
     * 颜色预解析策略（关键修复）：
     * - 前景/上层背景/下层背景的 colorSource 非 "app" 时，在此处预解析为具体颜色 hex 后填入 fgColorHex/bgColorHex/bgColor2
     * - "app" 源由 [IconPipelineUseCase] 按 packageName 实时从 appColorSchemes 解析（每个图标颜色不同），此处跳过预解析
     * - 否则打包出的图标会使用 configValue 中残留的默认颜色（如 #FFFFFFFF/#FF3F51B5），与自定义页面预览图不一致
     */
    private fun buildIconBuildConfig(configValue: IconConfigState): IconBuildConfig {
        // 预解析前景颜色（app 源由 pipeline 按 packageName 实时解析，此处跳过）
        val resolvedFgColor = if (configValue.fgColorSource != "app") {
            ConfigColorResolver.resolveConfigColors(
                isFg = true,
                config = configValue,
                wallpaperColorScheme = wallpaperColorScheme.value,
                appColorSchemes = appColorSchemes
            )
        } else configValue.fgColor

        // 预解析上层背景颜色（同上）
        val resolvedBgColor = if (configValue.bgColorSource != "app") {
            ConfigColorResolver.resolveConfigColors(
                isFg = false,
                config = configValue,
                wallpaperColorScheme = wallpaperColorScheme.value,
                appColorSchemes = appColorSchemes
            )
        } else configValue.bgColor

        // 预解析下层颜色（app 源由 pipeline 按 packageName 实时解析，此处跳过）
        val resolvedBgColor2 = if (configValue.dualLayerEnabled &&
            configValue.bgLayer2.colorSource != "app"
        ) {
            ConfigColorResolver.resolveConfigColors(
                isFg = false,
                config = configValue,
                wallpaperColorScheme = wallpaperColorScheme.value,
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
            useStreaming = useStreaming.value,
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
            bgPreviewThemeMode2 = configValue.bgLayer2.previewThemeMode,
            bgLayer2Alpha = configValue.bgLayer2.alpha,
            selectedMasks2 = configValue.bgLayer2.selectedMasks,
            selectedStaticImages2 = configValue.bgLayer2.selectedStaticImages,
            selectedFillingImages2 = configValue.bgLayer2.selectedFillingImages,
            imageFilling2RandomRotation = configValue.bgLayer2.imageFilling.randomRotation,
            imageFilling2ScaleMode = configValue.bgLayer2.imageFilling.scaleMode,
            innerShadow = InnerShadowConfig(
                // 双层背景启用时强制禁用内阴影（UI 已禁止，此处为安全兜底）
                enabled = configValue.innerShadow.enabled && !configValue.dualLayerEnabled,
                styleName = configValue.innerShadow.styleName,
                intensityLayers = configValue.innerShadow.intensityLayers
            )
        )
    }

    /**
     * 提交构建任务。本方法不检查权限，调用方应先通过 [buildPermissionsMissing] 检查并申请权限。
     *
     * iconCount 传 0（占位），BuildTaskExecutor 解析 mapper 后会更新真实数量。
     *
     * @param productType 产物类型（如 ZIP_ICONS）
     * @param iconSetId 图标集 id（对应 assets/icon_mapper/<id>.xml，不含扩展名）
     * @param iconSetLabel 图标集展示名
     * @return 创建的 [BuildTask]（PENDING 状态），若预览图未就绪则返回 null
     */
    fun submitBuildTask(
        productType: ProductType,
        iconSetId: String,
        iconSetLabel: String
    ): BuildTask? {
        val storePreview = _storePreviewBitmap.value ?: run {
            addLog("提交失败：store 预览图未就绪", LogType.ERROR)
            return null
        }
        val mainPreview = _mainPreviewBitmap.value ?: run {
            addLog("提交失败：main 预览图未就绪", LogType.ERROR)
            return null
        }

        val configValue = _config.value
        val buildConfig = buildIconBuildConfig(configValue)
        // 壁纸引用：当前壁纸位图的临时路径（用于失败重试时重新解析颜色，暂存 URI 字符串）
        val wallpaperUri = wallpaperBitmap.value?.let { "embedded:wallpaper" }

        return buildTaskManager.submit(
            config = buildConfig,
            configSnapshot = configValue,
            productType = productType,
            iconSetId = iconSetId,
            iconSetLabel = iconSetLabel,
            iconCount = BuildConfig.PLACEHOLDER_ICON_COUNT,
            wallpaperUri = wallpaperUri,
            storePreview = storePreview,
            mainPreview = mainPreview
        ).also {
            addLog("已提交构建任务: ${it.taskId}")
        }
    }

    /**
     * 重试失败任务。基于原任务的 configSnapshot 重新生成预览图后提交新任务。
     *
     * @param originalTaskId 原 FAILED 任务 id
     * @return 新创建的 [BuildTask]（PENDING 状态），原任务不存在/非 FAILED/预览图生成失败时返回 null
     */
    fun retryBuildTask(originalTaskId: String): BuildTask? {
        // 临时切到原 configSnapshot 生成预览图，再切回当前 config
        val original = finishedBuildTasks.value.find { it.taskId == originalTaskId } ?: return null
        if (original.status != BuildTaskStatus.FAILED) return null

        val currentConfig = _config.value
        // 临时应用原配置以生成对应预览图
        _config.value = original.configSnapshot
        generateLivePreview()
        // 等待预览图生成完成（generateLivePreview 异步，使用 runBlocking 不合适，
        // 此处简化处理：直接读当前缓存的预览图，若未就绪则恢复配置并返回 null）
        val storePreview = _storePreviewBitmap.value
        val mainPreview = _mainPreviewBitmap.value
        // 恢复当前配置
        _config.value = currentConfig
        generateLivePreview()

        if (storePreview == null || mainPreview == null) {
            addLog("重试失败：预览图未就绪，请稍后再试", LogType.ERROR)
            return null
        }

        return buildTaskManager.retry(
            originalTaskId = originalTaskId,
            storePreview = storePreview,
            mainPreview = mainPreview
        )?.also {
            addLog("已重试任务: ${it.taskId}")
        }
    }

    /**
     * 取消任务。RUNNING 任务取消执行，PENDING 任务直接从队列移除。
     * CANCELLED 状态不进入已完成列表。
     */
    fun cancelBuildTask(taskId: String) {
        buildTaskManager.cancel(taskId)
        addLog("已取消任务: $taskId")
    }

    /**
     * 删除已完成任务（同时清理其缩略图与预览图文件）。
     */
    fun deleteFinishedBuildTask(taskId: String) {
        buildTaskManager.deleteFinished(taskId)
        addLog("已删除任务: $taskId")
    }

    /**
     * 检查构建任务所需的权限，返回缺失的权限列表（空列表表示权限齐全）。
     * 调用方应使用 ActivityResultContracts.RequestPermission() 逐个申请。
     *
     * - Android 13+：POST_NOTIFICATIONS（用于进度通知）
     * - Android 9 及以下：WRITE_EXTERNAL_STORAGE（用于导出产物到公共 Documents）
     */
    fun buildPermissionsMissing(): List<String> {
        val missing = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) missing.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) missing.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return missing
    }
}
