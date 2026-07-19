package com.capybara.hypericonlab.modules.icon.domain.model

import kotlinx.serialization.Serializable

// 图标构建配置
@Serializable
data class IconBuildConfig(
    val iconSize: Int = 512,
    val iconScale: Float = 0.8f,
    val strokeWidthRatio: Float = 1.0f,
    val fgColorHex: String = "#FF000000",
    val bgColorHex: String = "#FFFFFFFF",
    val subtractIcon: Boolean = false,
    val packMtz: Boolean = true,
    val colorMode: ColorMode = ColorMode.CUSTOM,
    val useStreaming: Boolean = true,
    val masks: List<String> = listOf("m3_round"),
    val fgStyle: String = "line",
    val bgStyle: String = "solid",
    val stickerConfig: StickerConfig? = null,
    val glassConfig: GlassConfig? = null,
    // 静态图片背景：已选图片引用列表
    val selectedStaticImages: List<String> = emptyList(),
    // 图片填充背景：已选图片引用列表
    val selectedFillingImages: List<String> = emptyList(),
    // 图片填充配置
    val imageFillingRandomRotation: Boolean = false,
    val imageFillingScaleMode: String = "scale",
    // 双层背景：总开关、大小差异
    val dualLayerEnabled: Boolean = false,
    val dualLayerSizeDiff: Float = 0.08f,
    // 前景颜色来源（用于 IconPipelineUseCase 按 packageName 解析 app 源）
    val fgColorSource: String = "custom",
    // 上层背景颜色来源（用于 IconPipelineUseCase 判断同源优化 + app 源解析）
    val bgColorSource: String = "custom",
    // 下层背景扁平化字段
    val bgStyle2: String = "solid",
    val bgColor2: String = "#FF3F51B5",
    val bgColorSource2: String = "wallpaper",
    // 下层独立的 monet 变体
    val bgPreviewThemeMode2: String = "dark",
    // 下层图层级透明度 0~255
    val bgLayer2Alpha: Int = 255,
    val selectedMasks2: List<String> = listOf("m3_round"),
    val selectedStaticImages2: List<String> = emptyList(),
    val selectedFillingImages2: List<String> = emptyList(),
    val imageFilling2RandomRotation: Boolean = false,
    val imageFilling2ScaleMode: String = "scale"
)

@Serializable
data class StickerConfig(
    val fillStyle: String = "none", // "none", "fill", "glow"
    val strokeWidth: Float = 0.15f,
    val glowIntensity: Float = 0.5f,
    val lineColor: String = "#FF000000",
    val fillColor: String = "#00000000"
)

@Serializable
data class GlassConfig(
    val strokeDiff: Float = -0.4f,
    val angle: Float = -45f,
    val shadowEnabled: Boolean = true
)

@Serializable
enum class ColorMode {
    MONET, CUSTOM, COLORFUL, BLACK_WHITE
}
