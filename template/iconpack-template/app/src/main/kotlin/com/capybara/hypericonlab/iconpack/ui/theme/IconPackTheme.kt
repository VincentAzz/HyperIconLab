package com.capybara.hypericonlab.iconpack.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

private object IconPackThemeConfig {
    val FALLBACK_KEY_COLOR = Color(0xFF6750A4)
}

/**
 * 模板主题：使用系统 accent 作为取色来源，并固定采用 Monochrome 调色板。
 */
@Composable
fun IconPackTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val keyColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(id = android.R.color.system_accent1_500)
    } else {
        IconPackThemeConfig.FALLBACK_KEY_COLOR
    }
    val colorScheme = remember(keyColor, darkTheme) {
        dynamicColorScheme(
            seedColor = keyColor,
            isDark = darkTheme,
            style = PaletteStyle.Monochrome,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
