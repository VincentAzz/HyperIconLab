package com.capybara.hypericonlab.modules.icon.viewmodel

import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.model.IconSetInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class ResourceInitializer(
    private val scope: CoroutineScope,
    private val assetsFacade: LawniconsAssetFacade,
    private val onPreviewNeeded: () -> Unit
) {
    // 可用图标集列表
    private val _availableIconSets = MutableStateFlow<List<IconSetInfo>>(emptyList())
    val availableIconSets: StateFlow<List<IconSetInfo>> = _availableIconSets.asStateFlow()

    // mapper 是否就绪
    val mapperExists = MutableStateFlow(false)

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

    fun markResourcesReady() {
        mapperExists.value = true
        onPreviewNeeded()
    }
}
