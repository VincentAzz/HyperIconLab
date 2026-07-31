package com.capybara.hypericonlab.modules.icon.viewmodel

import android.content.Context
import com.capybara.hypericonlab.core.color.AppColorSchemesLoader
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsResourceManager
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import com.capybara.hypericonlab.modules.icon.domain.render.AppM3ColorCache
import com.capybara.hypericonlab.modules.icon.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.usecase.ManageResourcesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// 资源初始化器：负责启动时的图标集扫描、自动解压、配色加载
// 通过 onLog/onMapperReady/onPreviewNeeded 回调解耦对 ViewModel 的依赖
// 资源读取统一通过 LawniconsResourceManager，支持 assets/云端来源切换
class ResourceInitializer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val manageResourcesUseCase: ManageResourcesUseCase,
    private val buildTaskManager: BuildTaskManager,
    private val resourceManager: LawniconsResourceManager,
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
            val provider = resourceManager.getProvider()
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

    // 自动初始化：检测 lawnicons 目录，未解压则执行解压，并标记 mapper 就绪
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
                    resourceManager.refresh()
                } catch (e: Exception) {
                    onLog("资源解压失败: ${e.message}", LogType.ERROR)
                }
            } else {
                onLog("资源已就绪", LogType.INFO)
            }

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

    // 加载 app 配色方案并同步给 BuildTaskManager
    // 通过 resourceManager 获取当前激活资源的 color_schemes
    // 同时加载 App-M3 持久化缓存到内存（跨启动复用）
    fun loadColorSchemes() {
        scope.launch(Dispatchers.IO) {
            val provider = resourceManager.getProvider()
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
