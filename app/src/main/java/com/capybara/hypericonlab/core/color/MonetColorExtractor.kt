package com.capybara.hypericonlab.core.color

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.materialkolor.ktx.themeColors


// 从壁纸 Bitmap 提取 M3 ColorScheme
object MonetColorExtractor {

    private const val SAMPLE_SIZE = 128

    data class WallpaperColorScheme(
        val light: RoleColors,
        val dark: RoleColors
    )


    data class RoleColors(
        val primary: String,
        val primaryContainer: String,
        val onPrimary: String,
        val onPrimaryContainer: String
    )


    // 默认 TonalSpot + SPEC_2021
    fun extractFromBitmap(
        bitmap: Bitmap,
        paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
        colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2021,
        sampleSize: Int = SAMPLE_SIZE
    ): WallpaperColorScheme {
        val scaledBitmap = downscaleBitmap(bitmap, sampleSize)
        val imageBitmap = scaledBitmap.asImageBitmap()
        val seedColor = imageBitmap.themeColors(fallback = Color.Blue).first()

        val lightScheme = dynamicColorScheme(
            keyColor = seedColor, isDark = false,
            style = paletteStyle, colorSpec = colorSpec
        )
        val darkScheme = dynamicColorScheme(
            keyColor = seedColor, isDark = true,
            style = paletteStyle, colorSpec = colorSpec
        )

        if (scaledBitmap !== bitmap) scaledBitmap.recycle()

        return WallpaperColorScheme(
            light = RoleColors(
                primary = lightScheme.primary.toHex(),
                primaryContainer = lightScheme.primaryContainer.toHex(),
                onPrimary = lightScheme.onPrimary.toHex(),
                onPrimaryContainer = lightScheme.onPrimaryContainer.toHex()
            ),
            dark = RoleColors(
                primary = darkScheme.primary.toHex(),
                primaryContainer = darkScheme.primaryContainer.toHex(),
                onPrimary = darkScheme.onPrimary.toHex(),
                onPrimaryContainer = darkScheme.onPrimaryContainer.toHex()
            )
        )
    }

    private fun downscaleBitmap(src: Bitmap, maxSize: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxSize && height <= maxSize) return src

        val scale = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return src.scale(newWidth, newHeight)
    }

    private fun Color.toHex(): String = String.format("#%08X", this.toArgb())
}
