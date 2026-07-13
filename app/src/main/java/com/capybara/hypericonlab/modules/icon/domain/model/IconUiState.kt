package com.capybara.hypericonlab.modules.icon.domain.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.toArgb
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.PresetColors
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec

data class StickerUiState(
    val fillStyle: String = "none",
    val strokeWidth: Float = 0.12f,
    val glowIntensity: Float = 0.6f,
    val lineColor: String = "#FF000000",
    val fillColor: String = "#FFB0C4DE"
)

data class GlassUiState(
    val angle: Float = -45f,
    val strokeDiff: Float = -0.4f,
    val shadowEnabled: Boolean = true
)

data class CtcUiState(
    val type: String = "monochromatic",
    val variant: String = "light",
    val selectedIndex: Int = 0
)

data class PresetUiState(
    val seedColor: Int = PresetColors[0].color.toArgb(),
    val paletteStyle: PaletteStyle = PaletteStyle.Expressive,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025
)

// 默认 TonalSpot + SPEC_2021
data class WallpaperUiState(
    val paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2021
)

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
    val wallpaper: WallpaperUiState = WallpaperUiState()
)

data class PreviewState(
    val storePreview: Bitmap? = null,
    val mainPreview: Bitmap? = null
)
