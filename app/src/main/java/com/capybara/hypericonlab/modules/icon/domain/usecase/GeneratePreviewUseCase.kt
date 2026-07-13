package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.designsystem.theme.ctc.CTCPresets
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.capybara.hypericonlab.core.image.BackgroundGenerator
import com.capybara.hypericonlab.core.image.GlassProcessor
import com.capybara.hypericonlab.core.image.LayerMerger
import com.capybara.hypericonlab.core.image.StickerProcessor
import com.capybara.hypericonlab.core.image.SvgProcessor
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.preview.PreviewGenerator
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.domain.model.GlassConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.StickerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class GeneratePreviewUseCase(private val context: Context) {

    private val filesDir = context.filesDir
    private val lawniconsBase = File(filesDir, "lawnicons")
    private val mapperBase = File(filesDir, "icon_mapper")

    suspend fun execute(
        config: IconConfigState,
        wallpaperBitmap: Bitmap?,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>,
        onStorePreviewGenerated: (Bitmap) -> Unit
    ): Bitmap? = withContext(Dispatchers.Default) {
        val mapperFile = withContext(Dispatchers.IO) {
            val file = ZipUtils.findFileRecursive(mapperBase, "icon_mapper_preview.xml")
            if (file == null || !file.exists()) {
                ZipUtils.findFileRecursive(mapperBase, "icon_mapper.xml")
            } else file
        }
        val svgDir = withContext(Dispatchers.IO) {
            ZipUtils.findDirRecursive(lawniconsBase, "svgs")
        }

        if (mapperFile == null || svgDir == null) return@withContext null

        val masks = config.selectedMasks
        val maskBitmaps = withContext(Dispatchers.IO) {
            masks.mapNotNull { name ->
                try {
                    context.assets.open("masks/mask_${name}_512.png").use {
                        BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        val fullMap = IconMapperProcessor.parseIconMapper(mapperFile)
        val targets = listOf(
            "com.android.contacts.activities.TwelveKeyDialer",
            "com.google.android.apps.messaging",
            "com.android.chrome",
            "com.google.android.googlequicksearchbox",
            "com.tencent.mm",
            "tv.danmaku.bili",
            "com.coolapk.market",
            "com.xingin.xhs"
        )
        val previewIconMap =
            fullMap.filterKeys { k -> targets.any { k.contains(it) } }.toList().take(8).toMap()
        val fullPreviewIconMap = fullMap.toList().take(32).toMap()

        // Generate small preview
        val previewIcons = generateBitmaps(
            previewIconMap,
            svgDir,
            maskBitmaps,
            config,
            wallpaperColorScheme,
            appColorSchemes
        )
        val storePreview =
            PreviewGenerator.generateStorePreview(context, previewIcons, wallpaperBitmap)
        onStorePreviewGenerated(storePreview)
        previewIcons.forEach { it.recycle() }

        // Generate large preview
        val fullPreviewIcons = generateBitmaps(
            fullPreviewIconMap,
            svgDir,
            maskBitmaps,
            config,
            wallpaperColorScheme,
            appColorSchemes
        )
        val mainPreview =
            PreviewGenerator.generateMainPreview(context, fullPreviewIcons, wallpaperBitmap)

        fullPreviewIcons.forEach { it.recycle() }
        maskBitmaps.forEach { it.recycle() }

        mainPreview
    }

    private suspend fun generateBitmaps(
        iconMap: Map<String, String>,
        svgDir: File,
        maskBitmaps: List<Bitmap>,
        config: IconConfigState,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>
    ): List<Bitmap> {
        val results = mutableListOf<Bitmap>()
        iconMap.forEach { (packageName, drawableName) ->
            currentCoroutineContext().ensureActive()
            val svgFile = File(svgDir, "$drawableName.svg")
            if (svgFile.exists()) {
                val maskBmp = if (maskBitmaps.isNotEmpty()) maskBitmaps.random() else null
                val currentFg = resolveConfigColors(
                    true,
                    config,
                    wallpaperColorScheme,
                    appColorSchemes,
                    packageName
                )
                val currentBg = resolveConfigColors(
                    false,
                    config,
                    wallpaperColorScheme,
                    appColorSchemes,
                    packageName
                )

                val finalFg =
                    if (config.fgStyle == "hollow" || config.fgStyle == "glass") "#00000000" else currentFg
                val finalBg = if (config.bgStyle == "none") "#00000000" else currentBg

                val iconBitmap = SvgProcessor.processSvgFile(
                    svgFile = svgFile,
                    strokeWidthRatio = config.strokeWidthRatio,
                    fgColorHex = if (config.fgStyle == "sticker") "#FF000000" else finalFg,
                    iconSize = 512,
                    iconScale = if (config.fgStyle == "sticker") 1.0f else config.iconScale
                )

                if (iconBitmap != null) {
                    val processedIcon = when (config.fgStyle) {
                        "sticker" -> {
                            val stickerConfig = StickerConfig(
                                fillStyle = if (config.fgColorSource == "black_white") "fill" else config.sticker.fillStyle,
                                strokeWidth = config.sticker.strokeWidth,
                                glowIntensity = config.sticker.glowIntensity,
                                lineColor = if (config.fgColorSource == "custom") config.sticker.lineColor else {
                                    if (config.fgColorSource == "black_white") "#FF000000" else currentFg
                                },
                                fillColor = if (config.fgColorSource == "custom") config.sticker.fillColor else {
                                    if (config.fgColorSource == "black_white") "#FFFFFFFF" else {
                                        try {
                                            val base = currentFg.toColorInt()
                                            String.format("#%02X%06X", 20, base and 0xFFFFFF)
                                        } catch (_: Exception) {
                                            currentBg
                                        }
                                    }
                                }
                            )
                            StickerProcessor.process(
                                iconBitmap,
                                stickerConfig,
                                drawableName,
                                config.iconScale
                            )
                        }

                        "glass" -> {
                            val glassThemeColor =
                                if (!isColorWhite(currentFg)) currentFg else currentBg
                            GlassProcessor.process(
                                svgFile = svgFile,
                                baseStrokeWidthRatio = config.strokeWidthRatio,
                                iconSize = 512,
                                iconScale = config.iconScale,
                                themeColorHex = glassThemeColor,
                                isFgWhite = isColorWhite(currentFg),
                                glassConfig = GlassConfig(
                                    strokeDiff = config.glass.strokeDiff,
                                    angle = config.glass.angle,
                                    shadowEnabled = config.glass.shadowEnabled
                                )
                            )
                        }

                        else -> iconBitmap
                    }

                    if (processedIcon != null) {
                        val bgBitmap = BackgroundGenerator.createBackground(512, finalBg, maskBmp)
                        val finalBitmap = LayerMerger.merge(
                            background = bgBitmap,
                            icon = processedIcon,
                            subtractIcon = config.fgStyle == "hollow",
                            fgAlpha = if (config.fgStyle == "glass") 1.0f else try {
                                Color.alpha(finalFg.toColorInt()) / 255f
                            } catch (_: Exception) {
                                1.0f
                            }
                        )
                        results.add(finalBitmap)
                        if (processedIcon !== iconBitmap) processedIcon.recycle()
                        bgBitmap.recycle()
                    }
                    if (config.fgStyle != "glass") iconBitmap.recycle()
                }
            }
        }
        return results
    }

    fun resolveConfigColors(
        isFg: Boolean,
        config: IconConfigState,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>,
        packageName: String? = null
    ): String {
        val source = if (isFg) config.fgColorSource else config.bgColorSource
        val defaultColor = if (isFg) "#FFFFFFFF" else "#FF3F51B5"

        if (source == "preset" && config.fgStyle == "sticker") {
            val scheme = dynamicColorScheme(
                keyColor = androidx.compose.ui.graphics.Color(config.preset.seedColor),
                isDark = config.previewThemeMode == "dark",
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
                    when (config.previewThemeMode) {
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

            "custom" -> if (isFg) config.fgColor else config.bgColor
            "black_white" -> if (isFg) "#FF000000" else "#FFFFFFFF"
            "preset" -> {
                val scheme = dynamicColorScheme(
                    keyColor = androidx.compose.ui.graphics.Color(config.preset.seedColor),
                    isDark = config.previewThemeMode == "dark",
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
        }
    }

    private fun isColorWhite(hex: String): Boolean {
        return try {
            val color = hex.toColorInt()
            Color.red(color) > 240 && Color.green(color) > 240 && Color.blue(color) > 240
        } catch (_: Exception) {
            false
        }
    }
}
