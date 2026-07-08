package com.capybara.hypericonlab.core.color

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.RequiresPermission
import androidx.palette.graphics.Palette

/**
 * Utility to extract Monet-style colors from the system wallpaper or a provided bitmap.
 */
object MonetColorExtractor {

    data class ColorScheme(
        val foregroundColor: String,
        val backgroundColor: String
    )

    enum class ThemeMode {
        LIGHT, DARK, NEUTRAL
    }

    /**
     * Extracts colors from the current system wallpaper based on the desired mode.
     */
    @RequiresPermission(anyOf = ["android.permission.READ_WALLPAPER_INTERNAL", Manifest.permission.MANAGE_EXTERNAL_STORAGE])
    fun extractFromSystem(context: Context, mode: ThemeMode): ColorScheme {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val wallpaperDrawable = try {
            wallpaperManager.drawable
        } catch (e: SecurityException) {
            null
        }

        return if (wallpaperDrawable is BitmapDrawable) {
            extractFromBitmap(wallpaperDrawable.bitmap, mode)
        } else {
            getDefaultScheme(mode)
        }
    }

    /**
     * Extracts foreground and background colors using the Palette API and requested mode.
     */
    fun extractFromBitmap(bitmap: Bitmap, mode: ThemeMode): ColorScheme {
        val palette = Palette.from(bitmap).generate()

        return when (mode) {
            ThemeMode.LIGHT -> {
                val bg = palette.getLightMutedColor(Color.WHITE)
                val fg = palette.getVibrantColor(Color.BLACK)
                ColorScheme(colorToHex(fg), colorToHex(bg))
            }

            ThemeMode.DARK -> {
                val bg = palette.getDarkMutedColor(Color.DKGRAY)
                val fg = palette.getLightVibrantColor(Color.LTGRAY)
                ColorScheme(colorToHex(fg), colorToHex(bg))
            }

            ThemeMode.NEUTRAL -> {
                val bg = palette.getDominantColor(Color.GRAY)
                val fg = if (isColorDark(bg)) Color.LTGRAY else Color.DKGRAY
                ColorScheme(colorToHex(fg), colorToHex(bg))
            }
        }
    }

    private fun getDefaultScheme(mode: ThemeMode): ColorScheme {
        return when (mode) {
            ThemeMode.LIGHT -> ColorScheme("#3F51B5", "#F5F5F5")
            ThemeMode.DARK -> ColorScheme("#C5CAE9", "#1A1A1A")
            ThemeMode.NEUTRAL -> ColorScheme("#212121", "#E0E0E0")
        }
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%08X", color)
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}