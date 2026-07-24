package com.capybara.hypericonlab.modules.icon.ui.page.custom.internal

import android.content.Context
import com.capybara.hypericonlab.core.image.BgImageDir
import com.capybara.hypericonlab.core.image.BgImageLoader
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.InnerShadowUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// 配置同步观察器：监听 config 变化，自动修正非法配置组合、提取壁纸颜色、
// 触发预览重生成、重置内阴影状态、扫描内阴影资源
class ConfigSyncObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val configFlow: StateFlow<IconConfigState>,
    private val onConfigUpdate: ((IconConfigState) -> IconConfigState) -> Unit,
    private val onReextractWallpaperColors: () -> Unit,
    private val onRegeneratePreview: () -> Unit,
    private val onScanInnerShadowAssets: () -> Unit
) {
    // 启动所有配置同步观察协程
    fun observe() {
        observeConfigSyncRules()
        observeWallpaperColorExtraction()
        observePreviewTrigger()
        observeShapeChangeResetsInnerShadow()
        observeDualLayerResetsInnerShadow()
        // 初始化时扫描 assets/shadow_baked/ 目录，构建形状 → 样式列表映射
        onScanInnerShadowAssets()
    }

    // 配置同步规则：自动修正非法配置组合（如 hollow 风格不允许 none 背景）
    private fun observeConfigSyncRules() {
        scope.launch {
            configFlow.collect { config ->
                // 非 sticker 风格不允许 black_white 源（仅 sticker 支持）
                if (config.fgStyle != "sticker" && config.fgColorSource == "black_white") {
                    onConfigUpdate { it.copy(fgColorSource = "wallpaper") }
                }
                // sticker 的 none 填充不允许 black_white 源
                if (config.fgStyle == "sticker" && config.fgColorSource == "black_white" && config.sticker.fillStyle == "none") {
                    onConfigUpdate { it.copy(sticker = it.sticker.copy(fillStyle = "fill")) }
                }
                // hollow 风格必须有背景（none → solid + wallpaper）
                if (config.fgStyle == "hollow" && config.bgStyle == "none") {
                    onConfigUpdate { it.copy(bgStyle = "solid", bgColorSource = "wallpaper") }
                }
                // img_static 空列表自动填首个预设
                if (config.bgStyle == "img_static" && config.selectedStaticImages.isEmpty()) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.STATIC)
                    if (presets.isNotEmpty()) {
                        onConfigUpdate { it.copy(selectedStaticImages = listOf(presets.first())) }
                    }
                }
                // img_filling 空列表自动填首个预设
                if (config.bgStyle == "img_filling" && config.selectedFillingImages.isEmpty()) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.FILLING)
                    if (presets.isNotEmpty()) {
                        onConfigUpdate { it.copy(selectedFillingImages = listOf(presets.first())) }
                    }
                }
                // 双层启用时，上层背景不允许 none（强制切回 solid+wallpaper）
                if (config.dualLayerEnabled && config.bgStyle == "none") {
                    onConfigUpdate { it.copy(bgStyle = "solid", bgColorSource = "wallpaper") }
                }
                // 下层 img_static 空列表自动填首个预设
                if (config.dualLayerEnabled && config.bgLayer2.style == "img_static" &&
                    config.bgLayer2.selectedStaticImages.isEmpty()
                ) {
                    val presets = BgImageLoader.listPresetAssets(context, BgImageDir.STATIC)
                    if (presets.isNotEmpty()) {
                        onConfigUpdate {
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
                        onConfigUpdate {
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
    }

    // 壁纸配置变化时重新提取壁纸颜色
    private fun observeWallpaperColorExtraction() {
        scope.launch(Dispatchers.Default) {
            configFlow
                .map { it.wallpaper }
                .distinctUntilChanged()
                .collect { onReextractWallpaperColors() }
        }
    }

    // 仅在业务字段变化时触发预览重生成（忽略 selectedTab 变化）
    private fun observePreviewTrigger() {
        scope.launch {
            configFlow
                .map { it.copy(selectedTab = 0) }
                .distinctUntilChanged()
                .collect { onRegeneratePreview() }
        }
    }

    // 监听上层形状变化：切换形状时内阴影默认取消选择回到关闭状态
    private fun observeShapeChangeResetsInnerShadow() {
        scope.launch(Dispatchers.Default) {
            configFlow
                .map { it.selectedMasks }
                .distinctUntilChanged()
                .drop(1) // 跳过初始值，避免启动时误触发
                .collect {
                    onConfigUpdate { config ->
                        if (config.innerShadow.enabled || config.innerShadow.styleName != null) {
                            config.copy(innerShadow = InnerShadowUiState())
                        } else config
                    }
                }
        }
    }

    // 监听双层背景开关：启用双层时自动关闭内阴影（仅单层背景生效）
    private fun observeDualLayerResetsInnerShadow() {
        scope.launch(Dispatchers.Default) {
            configFlow
                .map { it.dualLayerEnabled }
                .distinctUntilChanged()
                .drop(1)
                .filter { it } // 仅在启用双层时触发
                .collect {
                    onConfigUpdate { config ->
                        if (config.innerShadow.enabled || config.innerShadow.styleName != null) {
                            config.copy(innerShadow = InnerShadowUiState())
                        } else config
                    }
                }
        }
    }
}
