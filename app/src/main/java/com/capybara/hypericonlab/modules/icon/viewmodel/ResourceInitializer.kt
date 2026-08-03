package com.capybara.hypericonlab.modules.icon.viewmodel

import android.content.Context
import com.capybara.hypericonlab.core.color.AppColorSchemesLoader
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.UpdateState
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import com.capybara.hypericonlab.modules.iconpack.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.iconrender.AppM3ColorCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.io.File
import kotlin.reflect.KClass

// 资源初始化器：负责启动时的图标集扫描、自动解压、配色加载、云端更新自动拉取
// 通过 onLog/onMapperReady/onPreviewNeeded 回调解耦对 ViewModel 的依赖
// 资源读取统一通过 LawniconsAssetFacade，支持 assets/云端来源切换
class ResourceInitializer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val manageResourcesUseCase: ManageResourcesUseCase,
    private val buildTaskManager: BuildTaskManager,
    private val assetsFacade: LawniconsAssetFacade,
    private val onLog: (String, LogType) -> Unit,
    private val onMapperReady: () -> Unit,
    private val onPreviewNeeded: () -> Unit
) {
    // 可用图标集列表
    private val _availableIconSets = MutableStateFlow<List<IconSetInfo>>(emptyList())
    val availableIconSets: StateFlow<List<IconSetInfo>> = _availableIconSets.asStateFlow()

    // mapper 是否就绪
    val mapperExists = MutableStateFlow(false)

    // app 配色方案映射，供预览/打包按 packageName 解析颜色
    var appColorSchemes: Map<String, Pair<String, String>> = emptyMap()
        private set

    // 扫描当前激活资源下支持的图标集，解析每个图标集的图标数量
    fun loadAvailableIconSets() {
        scope.launch(Dispatchers.IO) {
            val provider = assetsFacade.getProvider()
            val list = IconSetInfo.SUPPORTED_SETS.map { id ->
                // 解析图标数量，失败时返回 0
                val count = try {
                    provider.openIconMapper(IconSetInfo.mapperFileName(id))
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

    // 观察资源来源变化，切换来源后自动重新加载图标集映射数
    fun observeResourceChanges() {
        scope.launch {
            // 跳过初始值，仅在来源变化时重新加载
            assetsFacade.currentVersion.drop(1).collect {
                loadAvailableIconSets()
            }
        }
    }

    // 自动初始化：检测 lawnicons 目录，未解压则执行解压，并标记 mapper 就绪
    // assets 解压完成后，后台异步检查云端更新（不阻塞 mapper 就绪与预览生成）
    suspend fun autoInitializeResources() {
        scope.launch(Dispatchers.IO) {
            val lawniconsBase = File(context.filesDir, "lawnicons")

            if (!lawniconsBase.exists() || lawniconsBase.list()?.isEmpty() == true) {
                onLog("检测到资源未初始化，开始自动解压...", LogType.INFO)
                val startTime = System.currentTimeMillis()
                try {
                    manageResourcesUseCase.performUnzip { /* silent progress */ }
                    val duration = System.currentTimeMillis() - startTime
                    onLog("资源解压完成，耗时 ${duration}ms", LogType.SUCCESS)
                    // 解压完成后刷新 manager，使其检测到已解压的 svgs
                    assetsFacade.refresh()
                } catch (e: Exception) {
                    onLog("资源解压失败: ${e.message}", LogType.ERROR)
                }
            } else {
                onLog("资源已就绪", LogType.INFO)
            }

            // assets 解压完成后再后台检查云端更新（独立协程，不阻塞后续初始化）
            autoCheckCloudUpdate()

            // mapper 已直接打包在 assets 中，无需自动生成
            onLog("映射器已就绪", LogType.INFO)
            mapperExists.value = true
            onMapperReady()

            // 资源和映射就绪后，自动生成初始预览图
            if (mapperExists.value) {
                onLog("自动生成初始预览图...", LogType.INFO)
                onPreviewNeeded()
            }
        }
    }

    // 后台静默检查云端更新：失败不发通知（首次启动不打扰用户），state 仍更新供 assets tab 观察
    // 观察更新状态并记录关键日志
    private fun autoCheckCloudUpdate() {
        scope.launch(Dispatchers.IO) {
            onLog("开始后台检查云端更新...", LogType.INFO)

            // 观察更新状态变化，在状态类型切换时记录日志
            val observerJob = launch {
                var lastStateClass: KClass<out UpdateState>? = null
                assetsFacade.updateState.collect { state ->
                    val stateClass = state::class
                    // 仅在状态类型变化时记录日志，忽略同类进度更新（如 Downloading 0.1→0.2）
                    if (stateClass != lastStateClass) {
                        lastStateClass = stateClass
                        logUpdateState(state)
                    }
                }
            }

            try {
                assetsFacade.checkAndInstallSilently()
            } finally {
                observerJob.cancel()
            }
        }
    }

    // 将更新状态映射为日志文案
    private fun logUpdateState(state: UpdateState) {
        when (state) {
            is UpdateState.Checking -> onLog("云端更新：正在检查版本...", LogType.INFO)
            is UpdateState.Downloading -> onLog("云端更新：正在下载资源包...", LogType.INFO)
            is UpdateState.Extracting -> onLog("云端更新：正在解压资源...", LogType.INFO)
            is UpdateState.Success -> onLog(
                "云端更新：已切换到版本 ${state.newVersion}",
                LogType.SUCCESS
            )

            is UpdateState.Failed -> onLog("云端更新：失败（${state.reason}）", LogType.ERROR)
            UpdateState.UpToDate -> onLog("云端更新：已是最新版本", LogType.INFO)
            UpdateState.Idle -> {} // Idle 不记录
        }
    }

    // 加载 app 配色方案并同步给 BuildTaskManager
    // 获取当前资源的 color_schemes
    // 同时加载 App-M3 持久化缓存到内存（跨启动复用）
    fun loadColorSchemes() {
        scope.launch(Dispatchers.IO) {
            val provider = assetsFacade.getProvider()
            appColorSchemes = try {
                provider.openColorSchemes("app_color_schemes.xml")
                    .use { AppColorSchemesLoader.loadFromStream(it) }
            } catch (_: Exception) {
                emptyMap()
            }
            // 同步给 BuildTaskManager，供 executor 执行任务时使用
            buildTaskManager.updateAppColorSchemes(appColorSchemes)
            // 加载 App-M3 持久化缓存（使用与 WallpaperUiState 一致的默认配置）
            // App-M3 不暴露 paletteStyle/colorSpec 配置，使用固定默认值
            AppM3ColorCache.loadFromFile(
                context = context,
                paletteStyle = PaletteStyle.TonalSpot,
                colorSpec = ThemeColorSpec.SPEC_2021
            )
        }
    }
}
