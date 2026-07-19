package com.capybara.hypericonlab.modules.icon.domain.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import kotlinx.serialization.Serializable

@Serializable
data class StickerUiState(
    val fillStyle: String = "none",
    val strokeWidth: Float = 0.12f,
    val glowIntensity: Float = 0.6f,
    val lineColor: String = "#FF000000",
    val fillColor: String = "#FFB0C4DE"
)

@Serializable
data class GlassUiState(
    val angle: Float = -45f,
    val strokeDiff: Float = -0.4f,
    val shadowEnabled: Boolean = true
)

@Serializable
data class CtcUiState(
    val type: String = "monochromatic",
    val variant: String = "light",
    val selectedIndex: Int = 0
)

@Serializable
data class PresetUiState(
    val seedColor: Int = PresetColors[0].color.toArgb(),
    val paletteStyle: PaletteStyle = PaletteStyle.Expressive,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025
)

// 默认 TonalSpot + SPEC_2021
@Serializable
data class WallpaperUiState(
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2021
)

@Serializable
data class IconConfigState(
    val colorMode: ColorMode = ColorMode.CUSTOM,
    val fgColor: String = "#FFFFFFFF",
    val bgColor: String = "#FF3F51B5",
    val strokeWidthRatio: Float = 1.0f,
    val iconScale: Float = 0.75f,
    val selectedTab: Int = 0,
    val fgStyle: String = "line",
    val bgStyle: String = "solid",
    val fgColorSource: String = "wallpaper",
    val bgColorSource: String = "wallpaper",
    val selectedMasks: List<String> = listOf("m3_round"),
    val previewThemeMode: String = "dark",
    val sticker: StickerUiState = StickerUiState(),
    val glass: GlassUiState = GlassUiState(),
    val ctc: CtcUiState = CtcUiState(),
    val preset: PresetUiState = PresetUiState(),
    val wallpaper: WallpaperUiState = WallpaperUiState(),
    // 前景与背景颜色来源是否同步联动，默认启用
    val syncColorSource: Boolean = true,
    // 静态图片背景：已选图片引用列表（最多5个），独立保存
    val selectedStaticImages: List<String> = emptyList(),
    // 图片填充背景：已选图片引用列表（最多5个），独立保存
    val selectedFillingImages: List<String> = emptyList(),
    // 图片填充配置
    val imageFilling: ImageFillingUiState = ImageFillingUiState(),
    // 双层背景总开关，默认关闭
    val dualLayerEnabled: Boolean = false,
    // 下层背景独立配置
    val bgLayer2: BgLayerUiState = BgLayerUiState(),
    // 下层背景相对上层的大小差异，0.0~0.3，步进 0.02（15 档）
    val dualLayerSizeDiff: Float = 0.08f,
    // 双层启用时，上层颜色来源变更是否同步应用到下层（仅同步 colorSource，下层 monet 变体保持独立）
    val syncDualLayerColorSource: Boolean = false
)

/**
 * 下层背景独立配置。
 * 与上层背景字段一一对应但不共享，上下层颜色完全分离处理。
 */
@Serializable
data class BgLayerUiState(
    // 背景样式：none/solid/img_static/img_filling
    val style: String = "solid",
    // 自定义颜色 hex
    val color: String = "#FF3F51B5",
    // 颜色来源：wallpaper/app/preset/ctc/custom/black_white
    val colorSource: String = "wallpaper",
    // 下层独立的 monet 变体，与上层分离；默认中性，与上层默认暗色形成可见差异
    val previewThemeMode: String = "neutral",
    // 下层图层级透明度 0~255，默认不透明
    val alpha: Int = 255,
    // 形状遮罩
    val selectedMasks: List<String> = listOf("m3_round"),
    // 静态图片背景：已选图片引用列表
    val selectedStaticImages: List<String> = emptyList(),
    // 图片填充背景：已选图片引用列表
    val selectedFillingImages: List<String> = emptyList(),
    // 图片填充配置
    val imageFilling: ImageFillingUiState = ImageFillingUiState()
)

@Serializable
data class ImageFillingUiState(
    // 是否随机旋转
    val randomRotation: Boolean = false,
    // 缩放模式："scale"=缩放填充(默认), "crop"=居中裁切
    val scaleMode: String = "scale"
)

data class PreviewState(
    val storePreview: Bitmap? = null,
    val mainPreview: Bitmap? = null
)
