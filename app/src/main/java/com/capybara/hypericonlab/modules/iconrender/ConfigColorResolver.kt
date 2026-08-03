package com.capybara.hypericonlab.modules.iconrender

import android.content.Context
import android.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.HslColorUtils
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.designsystem.theme.ctc.CTCPresets
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import timber.log.Timber
import androidx.compose.ui.graphics.Color as ComposeColor

// 图标颜色解析器，按 colorSource 解析前景与背景颜色
object ConfigColorResolver {

    // preset scheme 缓存：避免每个图标都重新计算 dynamicColorScheme
    // 一次预览生成中 seedColor/paletteStyle/colorSpec 不变，只有 isDark 不同，最多缓存 2 个 scheme
    private var presetCacheKey: String? = null
    private var presetLightScheme: ColorScheme? = null
    private var presetDarkScheme: ColorScheme? = null

    // App-M3 默认配置（不暴露给用户，固定使用 TonalSpot + SPEC_2021）
    private val APP_M3_PALETTE_STYLE = PaletteStyle.TonalSpot
    private val APP_M3_COLOR_SPEC = ThemeColorSpec.SPEC_2021

    // 获取或计算 preset scheme（带缓存）
    private fun getPresetScheme(
        seedColor: Int,
        paletteStyle: PaletteStyle,
        colorSpec: ThemeColorSpec,
        isDark: Boolean
    ): ColorScheme {
        val key = "${seedColor}_${paletteStyle}_${colorSpec}"
        if (key != presetCacheKey) {
            presetCacheKey = key
            presetLightScheme = null
            presetDarkScheme = null
        }
        if (isDark) {
            if (presetDarkScheme == null) {
                presetDarkScheme = dynamicColorScheme(
                    keyColor = ComposeColor(seedColor),
                    isDark = true,
                    style = paletteStyle,
                    colorSpec = colorSpec
                )
            }
            return presetDarkScheme!!
        } else {
            if (presetLightScheme == null) {
                presetLightScheme = dynamicColorScheme(
                    keyColor = ComposeColor(seedColor),
                    isDark = false,
                    style = paletteStyle,
                    colorSpec = colorSpec
                )
            }
            return presetLightScheme!!
        }
    }

    fun resolveConfigColors(
        isFg: Boolean,
        config: IconConfigState,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>,
        packageName: String? = null,
        layerIndex: Int = 0,
        // App-M3 缓存所需 context，仅 app_m3 源使用
        context: Context? = null
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
            val scheme = getPresetScheme(
                config.preset.seedColor,
                config.preset.paletteStyle,
                config.preset.colorSpec,
                themeMode == "dark"
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
                // 上层 reduceWhiteBg 取 config.appReduceWhiteBg；下层取 bgLayer2.appReduceWhiteBg
                val reduceWhiteBg = if (layerIndex == 1) config.bgLayer2.appReduceWhiteBg
                else config.appReduceWhiteBg
                resolveAppColors(
                    isFg = isFg,
                    appColorSchemes = appColorSchemes,
                    packageName = packageName,
                    reduceWhiteBg = reduceWhiteBg,
                    bgStyleNone = config.bgStyle == "none",
                    defaultColor = defaultColor
                )
            }

            "app_m3" -> {
                // app_m3：用应用颜色作为 M3 种子色，从生成的 scheme 中取 fg/bg
                val reduceWhiteBg = if (layerIndex == 1) config.bgLayer2.appReduceWhiteBg
                else config.appReduceWhiteBg
                resolveAppM3Colors(
                    isFg = isFg,
                    context = context,
                    appColorSchemes = appColorSchemes,
                    packageName = packageName,
                    themeMode = themeMode,
                    reduceWhiteBg = reduceWhiteBg,
                    bgStyleNone = config.bgStyle == "none",
                    defaultColor = defaultColor
                )
            }

            "custom" -> customColor
            "black_white" -> if (isFg) "#FF000000" else "#FFFFFFFF"
            "preset" -> {
                val scheme = getPresetScheme(
                    config.preset.seedColor,
                    config.preset.paletteStyle,
                    config.preset.colorSpec,
                    themeMode == "dark"
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
            // 确保两层有可见对比度。wallpaper/preset/app_m3 有完整 scheme 由用户选 monet 变体互补，不自动处理。
            if (!isFg && layerIndex == 1 &&
                source == config.bgColorSource && (source == "app" || source == "ctc")
            ) {
                HslColorUtils.adjustLuminanceForContrast(resolvedColor)
            } else {
                resolvedColor
            }
        }
    }

    // 解析"基于应用-原色"颜色
    // reduceWhiteBg 启用时：白色背景交换 fg/bg，使背景变为非白色的前景色
    fun resolveAppColors(
        isFg: Boolean,
        appColorSchemes: Map<String, Pair<String, String>>,
        packageName: String?,
        reduceWhiteBg: Boolean,
        bgStyleNone: Boolean,
        defaultColor: String
    ): String {
        return packageName?.let { pkg ->
            appColorSchemes[pkg]?.let { scheme ->
                val originalFg = scheme.first
                val originalBg = scheme.second
                when {
                    // 减少白色背景：bg 为白色时交换 fg/bg
                    reduceWhiteBg && isColorWhite(originalBg) -> {
                        if (isFg) originalBg else originalFg
                    }
                    // 原逻辑：无背景且 fg 为白色时，fg 改用 bg
                    isFg && bgStyleNone && isColorWhite(originalFg) -> originalBg
                    isFg -> originalFg
                    else -> originalBg
                }
            }
        } ?: defaultColor
    }

    // 解析"基于应用-M3"颜色
    // 用应用背景色作为种子色生成 M3 scheme，从 scheme 中按 monet 变体取 fg/bg
    // reduceWhiteBg 启用时：白色背景交换 fg/bg，使背景变为非白色的前景色作为种子色
    // 中性变体：参考"基于壁纸"，交换浅色 fg/bg
    fun resolveAppM3Colors(
        isFg: Boolean,
        context: Context?,
        appColorSchemes: Map<String, Pair<String, String>>,
        packageName: String?,
        themeMode: String,
        reduceWhiteBg: Boolean,
        bgStyleNone: Boolean,
        defaultColor: String
    ): String {
        if (context == null) return defaultColor

        val (seedColorHex, _) = pickAppSeedColor(
            appColorSchemes = appColorSchemes,
            packageName = packageName,
            reduceWhiteBg = reduceWhiteBg,
            defaultColor = defaultColor
        ) ?: return defaultColor

        val seedColorArgb = try {
            seedColorHex.toColorInt()
        } catch (_: Exception) {
            return defaultColor
        }

        // 从缓存获取或计算 M3 关键颜色
        // 异常防护：dynamicColorScheme 可能对极端种子色抛异常，捕获后回退 defaultColor 避免预览静默失败
        val colors = try {
            AppM3ColorCache.getOrCompute(
                context = context,
                seedColor = seedColorArgb,
                paletteStyle = APP_M3_PALETTE_STYLE,
                colorSpec = APP_M3_COLOR_SPEC
            )
        } catch (e: Exception) {
            Timber.tag("ConfigColorResolver").e(e, "App-M3 compute failed for $seedColorHex")
            return defaultColor
        }

        // 按 monet 变体取 fg/bg，与 wallpaper 源一致
        return when (themeMode) {
            "light" -> if (isFg) formatHex(colors.lightPrimary) else formatHex(colors.lightPrimaryContainer)
            "neutral" -> if (isFg) formatHex(colors.lightPrimaryContainer) else formatHex(colors.lightPrimary)
            "dark" -> if (isFg) formatHex(colors.darkOnPrimaryContainer) else formatHex(colors.darkOnPrimary)
            else -> defaultColor
        }
    }

    // 提取应用的种子色：优先背景色，reduceWhiteBg 时白色背景改用前景色
    // 返回 (seedColorHex, isSwapped)
    private fun pickAppSeedColor(
        appColorSchemes: Map<String, Pair<String, String>>,
        packageName: String?,
        reduceWhiteBg: Boolean,
        defaultColor: String
    ): Pair<String, Boolean>? {
        return packageName?.let { pkg ->
            appColorSchemes[pkg]?.let { scheme ->
                val originalFg = scheme.first
                val originalBg = scheme.second
                if (reduceWhiteBg && isColorWhite(originalBg)) {
                    // 白色背景时改用前景色作为种子色
                    originalFg to true
                } else {
                    originalBg to false
                }
            }
        }
    }

    // 格式化 Int 颜色值为 #AARRGGBB 字符串
    private fun formatHex(colorInt: Int): String {
        return String.format("#%08X", colorInt)
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
