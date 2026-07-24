package com.capybara.hypericonlab.modules.icon.ui.page.custom

import android.annotation.SuppressLint
import android.app.Application
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.core.image.BgImageDir
import com.capybara.hypericonlab.core.image.BgImageLoader
import com.capybara.hypericonlab.modules.icon.domain.model.BgLayerUiState
import com.capybara.hypericonlab.modules.icon.domain.model.BuildTask
import com.capybara.hypericonlab.modules.icon.domain.model.CtcUiState
import com.capybara.hypericonlab.modules.icon.domain.model.GlassUiState
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import com.capybara.hypericonlab.modules.icon.domain.model.ImageFillingUiState
import com.capybara.hypericonlab.modules.icon.domain.model.InnerShadowUiState
import com.capybara.hypericonlab.modules.icon.domain.model.PresetUiState
import com.capybara.hypericonlab.modules.icon.domain.model.ProductType
import com.capybara.hypericonlab.modules.icon.domain.model.StickerUiState
import com.capybara.hypericonlab.modules.icon.domain.model.WallpaperUiState
import com.capybara.hypericonlab.modules.icon.domain.render.ConfigColorResolver
import com.capybara.hypericonlab.modules.icon.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.usecase.GeneratePreviewUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.IconPipelineUseCase
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.BuildTaskCoordinator
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.IconLogger
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.InnerShadowAssetScanner
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.LogEntry
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.LogType
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.PreviewCoordinator
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.ResourceInitializer
import com.capybara.hypericonlab.modules.icon.ui.page.custom.internal.WallpaperManager
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
import kotlin.time.Duration.Companion.milliseconds

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

    // 壁纸管理器：封装壁纸位图与配色方案状态，configProvider 提供当前 wallpaper 配置，更新后触发预览重生成
    private val wallpaperManager = WallpaperManager(
        context = context,
        scope = viewModelScope,
        configProvider = { _config.value.wallpaper },
        onWallpaperUpdated = { generateLivePreview() }
    )
    val wallpaperBitmap: StateFlow<Bitmap?> = wallpaperManager.wallpaperBitmap
    private val wallpaperColorScheme: StateFlow<MonetColorExtractor.WallpaperColorScheme?> =
        wallpaperManager.wallpaperColorScheme

    // 资源初始化器：管理图标集扫描/自动解压/配色加载，通过回调解耦日志与预览触发
    private val resourceInitializer = ResourceInitializer(
        context = context,
        scope = viewModelScope,
        manageResourcesUseCase = manageResourcesUseCase,
        buildTaskManager = buildTaskManager,
        onLog = { message, type -> addLog(message, type) },
        onMapperReady = { },
        onPreviewNeeded = { generateLivePreview() }
    )

    // 资源初始化器管理的状态，此处转发对外暴露
    val availableIconSets: StateFlow<List<IconSetInfo>> = resourceInitializer.availableIconSets
    val mapperExists: StateFlow<Boolean> = resourceInitializer.mapperExists
    val appColorSchemes: Map<String, Pair<String, String>>
        get() = resourceInitializer.appColorSchemes

    // 内阴影资源扫描器：扫描 assets/shadow_baked/ 构建形状 → 样式映射
    private val innerShadowAssetScanner = InnerShadowAssetScanner(
        context = context,
        scope = viewModelScope
    )
    val shadowAssetsMap: StateFlow<Map<String, List<String>>> =
        innerShadowAssetScanner.shadowAssetsMap
    val useStreaming = MutableStateFlow(true)
    private val _lastPackDuration = MutableStateFlow<Long?>(null)
    val lastPackDuration: StateFlow<Long?> = _lastPackDuration.asStateFlow()

    // 日志管理器：封装日志状态流与添加/清空，ViewModel 转发对外暴露
    private val logger = IconLogger()
    val logs: StateFlow<List<LogEntry>> = logger.logs

    // 预览协调器：管理 store/main 预览位图与生成流程，通过 provider/回调解耦 ViewModel 状态
    private val previewCoordinator = PreviewCoordinator(
        scope = viewModelScope,
        generatePreviewUseCase = generatePreviewUseCase,
        configProvider = { _config.value },
        wallpaperBitmapProvider = { wallpaperBitmap.value },
        wallpaperColorSchemeProvider = { wallpaperColorScheme.value },
        appColorSchemesProvider = { appColorSchemes },
        isRunningProvider = { _isRunning.value },
        onLog = { message, type -> addLog(message, type) },
        onStatusTextChange = { _statusText.value = it },
        onProgressChange = { _currentProgress.value = it },
        onRunningChange = { _isRunning.value = it }
    )
    val storePreviewBitmap: StateFlow<Bitmap?> = previewCoordinator.storePreviewBitmap
    val mainPreviewBitmap: StateFlow<Bitmap?> = previewCoordinator.mainPreviewBitmap

    // 构建任务协调器：管理打包流程、任务提交/重试/取消/删除、权限检查
    // 通过 provider 回调解耦状态读取，通过回调解耦状态写回与配置切换
    private val buildTaskCoordinator = BuildTaskCoordinator(
        context = context,
        scope = viewModelScope,
        pipeline = pipeline,
        buildTaskManager = buildTaskManager,
        configProvider = { _config.value },
        wallpaperBitmapProvider = { wallpaperBitmap.value },
        wallpaperColorSchemeProvider = { wallpaperColorScheme.value },
        appColorSchemesProvider = { appColorSchemes },
        useStreamingProvider = { useStreaming.value },
        storePreviewProvider = { previewCoordinator.storePreviewBitmap.value },
        mainPreviewProvider = { previewCoordinator.mainPreviewBitmap.value },
        isRunningProvider = { _isRunning.value },
        onLog = { message, type -> addLog(message, type) },
        onStatusTextChange = { _statusText.value = it },
        onProgressChange = { _currentProgress.value = it },
        onRunningChange = { _isRunning.value = it },
        onLastPackDurationChange = { _lastPackDuration.value = it },
        onConfigSwap = { newConfig -> _config.value = newConfig },
        onRegeneratePreview = { generateLivePreview() }
    )

    // 构建任务队列与已完成列表（转发 BuildTaskCoordinator 的 StateFlow）
    val activeBuildTasks: StateFlow<List<BuildTask>> = buildTaskCoordinator.activeBuildTasks
    val finishedBuildTasks: StateFlow<List<BuildTask>> = buildTaskCoordinator.finishedBuildTasks

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

            // 资源初始化（自动解压/mapper 就绪/初始预览）由 ResourceInitializer 统一管理
            resourceInitializer.autoInitializeResources()
        }
        resourceInitializer.loadColorSchemes()
        observeRunningState()
        observeConfigChanges()
        loadDefaultWallpaper()
        resourceInitializer.loadAvailableIconSets()
    }

    private fun addLog(message: String, type: LogType = LogType.INFO) = logger.addLog(message, type)

    fun clearLogs() = logger.clearLogs()

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
        innerShadowAssetScanner.scan()
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

    fun generateLivePreview() = previewCoordinator.generateLivePreview()

    fun refreshPreview() = previewCoordinator.refreshPreview()

    fun generatePreview(isLive: Boolean = false) = previewCoordinator.generatePreview(isLive)

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
                resourceInitializer.mapperExists.value = true
                addLog("手动生成映射器成功，耗时 ${duration}ms", LogType.SUCCESS)
            }.onFailure {
                _statusText.value = "错误：${it.message}"
                addLog("手动生成映射器失败: ${it.message}", LogType.ERROR)
            }
            _isRunning.value = false
        }
    }

    fun runPipeline(mapperName: String) = buildTaskCoordinator.runPipeline(mapperName)

    private fun loadDefaultWallpaper() = wallpaperManager.loadDefaultWallpaper()

    fun updateWallpaperFromUri(uri: Uri) = wallpaperManager.updateWallpaperFromUri(uri)

    fun updateWallpaper(bmp: Bitmap) = wallpaperManager.updateWallpaper(bmp)

    private fun reextractWallpaperColors() = wallpaperManager.reextractWallpaperColors()

    fun submitBuildTask(
        productType: ProductType,
        iconSetId: String,
        iconSetLabel: String
    ): BuildTask? = buildTaskCoordinator.submitBuildTask(productType, iconSetId, iconSetLabel)

    fun retryBuildTask(originalTaskId: String): BuildTask? =
        buildTaskCoordinator.retryBuildTask(originalTaskId)

    fun cancelBuildTask(taskId: String) = buildTaskCoordinator.cancelBuildTask(taskId)

    fun deleteFinishedBuildTask(taskId: String) =
        buildTaskCoordinator.deleteFinishedBuildTask(taskId)

    fun buildPermissionsMissing(): List<String> = buildTaskCoordinator.buildPermissionsMissing()
}
