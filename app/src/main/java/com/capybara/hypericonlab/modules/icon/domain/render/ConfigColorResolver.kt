package com.capybara.hypericonlab.modules.icon.domain.render

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.HslColorUtils
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.designsystem.theme.ctc.CTCPresets
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState

// 图标颜色解析器，按 colorSource 解析前景与背景颜色
object ConfigColorResolver {

    // 解析配置颜色，返回 hex 字符串
    fun resolveConfigColors(
        isFg: Boolean,
        config: IconConfigState,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>,
        packageName: String? = null,
        layerIndex: Int = 0
    ): String {
        val bgLayer2 = config.bgLayer2
        val source = when {
            isFg -> config.fgColorSource
            layerIndex == 1 -> bgLayer2.colorSource
            else -> config.bgColorSource
        }
        val customColor = when {
            isFg -> config.fgColor
            layerIndex == 1 -> bgLayer2.color
            else -> config.bgColor
        }
        // 下层背景使用独立的 monet 变体，与上层分离
        val themeMode = if (layerIndex == 1) bgLayer2.previewThemeMode else config.previewThemeMode
        val defaultColor = if (isFg) "#FFFFFFFF" else "#FF3F51B5"

        if (source == "preset" && config.fgStyle == "sticker") {
            val scheme = dynamicColorScheme(
                keyColor = androidx.compose.ui.graphics.Color(config.preset.seedColor),
                isDark = themeMode == "dark",
                style = config.preset.paletteStyle,
                colorSpec = config.preset.colorSpec
            )
            return if (isFg) String.format("#%08X", scheme.primary.toArgb())
            else String.format("#%08X", scheme.surfaceContainer.toArgb())
        }

        return when (source) {
            "wallpaper" -> {
                // - light (亮色): fg=light.primary, bg=light.primaryContainer
                // - neutral (中性, 反转亮色): fg=light.primaryContainer, bg=light.primary
                // - dark (暗色): fg=dark.onPrimaryContainer, bg=dark.onPrimary
                wallpaperColorScheme?.let { scheme ->
                    when (themeMode) {
                        "light" -> if (isFg) scheme.light.primary else scheme.light.primaryContainer
                        "neutral" -> if (isFg) scheme.light.primaryContainer else scheme.light.primary
                        "dark" -> if (isFg) scheme.dark.onPrimaryContainer else scheme.dark.onPrimary
                        else -> defaultColor
                    }
                } ?: defaultColor
            }

            "app" -> {
                packageName?.let { pkg ->
                    appColorSchemes[pkg]?.let { scheme ->
                        val originalFg = scheme.first
                        val originalBg = scheme.second
                        if (isFg && config.bgStyle == "none" && isColorWhite(originalFg)) originalBg
                        else if (isFg) originalFg else originalBg
                    }
                } ?: defaultColor
            }

            "custom" -> customColor
            "black_white" -> if (isFg) "#FF000000" else "#FFFFFFFF"
            "preset" -> {
                val scheme = dynamicColorScheme(
                    keyColor = androidx.compose.ui.graphics.Color(config.preset.seedColor),
                    isDark = themeMode == "dark",
                    style = config.preset.paletteStyle,
                    colorSpec = config.preset.colorSpec
                )
                if (isFg) String.format("#%08X", scheme.primary.toArgb())
                else String.format("#%08X", scheme.surface.toArgb())
            }

            "ctc" -> {
                val ctc = config.ctc
                if (ctc.type == "monochromatic") {
                    CTCPresets.MonochromaticSchemes.getOrNull(ctc.selectedIndex)?.let { scheme ->
                        when (ctc.variant) {
                            "light" -> if (isFg) scheme.lightPrimary else scheme.lightBg
                            "dark" -> if (isFg) scheme.darkPrimary else scheme.darkBg
                            "neutral" -> if (isFg) scheme.neutralPrimary else scheme.neutralBg
                            else -> if (isFg) scheme.lightPrimary else scheme.lightBg
                        }.hex
                    } ?: defaultColor
                } else {
                    CTCPresets.ContrastSchemes.getOrNull(ctc.selectedIndex)?.let { scheme ->
                        val isSwapped = ctc.variant == "swapped"
                        (if (isFg) (if (isSwapped) scheme.bg else scheme.primary)
                        else (if (isSwapped) scheme.primary else scheme.bg)).hex
                    } ?: defaultColor
                }
            }

            else -> defaultColor
        }.let { resolvedColor ->
            // 下层背景（layerIndex=1）同源优化：
            // 当上下层 colorSource 相同且为 app/ctc 单色源时，对下层应用亮度互补，
            // 确保两层有可见对比度。wallpaper/preset 有完整 scheme 由用户选 monet 变体互补，不自动处理。
            if (!isFg && layerIndex == 1 &&
                source == config.bgColorSource && (source == "app" || source == "ctc")
            ) {
                HslColorUtils.adjustLuminanceForContrast(resolvedColor)
            } else {
                resolvedColor
            }
        }
    }

    // 判断是否接近白色
    fun isColorWhite(hex: String): Boolean {
        return try {
            val color = hex.toColorInt()
            Color.red(color) > 240 && Color.green(color) > 240 && Color.blue(color) > 240
        } catch (_: Exception) {
            false
        }
    }
}
