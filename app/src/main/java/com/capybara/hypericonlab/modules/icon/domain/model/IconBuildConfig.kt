package com.capybara.hypericonlab.modules.icon.domain.model

// 图标构建配置
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
    val glassConfig: GlassConfig? = null
)

data class StickerConfig(
    val fillStyle: String = "none", // "none", "fill", "glow"
    val strokeWidth: Float = 0.15f,
    val glowIntensity: Float = 0.5f,
    val lineColor: String = "#FF000000",
    val fillColor: String = "#00000000"
)

data class GlassConfig(
    val strokeDiff: Float = -0.4f,
    val angle: Float = -45f,
    val shadowEnabled: Boolean = true
)

enum class ColorMode {
    MONET, CUSTOM, COLORFUL, BLACK_WHITE
}
