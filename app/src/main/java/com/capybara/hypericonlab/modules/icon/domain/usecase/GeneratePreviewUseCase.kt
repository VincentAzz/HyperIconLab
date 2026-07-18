package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.HslColorUtils
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

        // 下层背景独立 mask（双层启用时加载）
        val maskBitmaps2 = if (config.dualLayerEnabled) {
            withContext(Dispatchers.IO) {
                config.bgLayer2.selectedMasks.mapNotNull { name ->
                    try {
                        context.assets.open("masks/mask_${name}_512.png").use {
                            BitmapFactory.decodeStream(it)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } else emptyList()

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
            maskBitmaps2,
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
            maskBitmaps2,
            config,
            wallpaperColorScheme,
            appColorSchemes
        )
        val mainPreview =
            PreviewGenerator.generateMainPreview(context, fullPreviewIcons, wallpaperBitmap)

        fullPreviewIcons.forEach { it.recycle() }
        maskBitmaps.forEach { it.recycle() }
        maskBitmaps2.forEach { it.recycle() }

        mainPreview
    }

    private suspend fun generateBitmaps(
        iconMap: Map<String, String>,
        svgDir: File,
        maskBitmaps: List<Bitmap>,
        maskBitmaps2: List<Bitmap>,
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
                        val upperBg = when (config.bgStyle) {
                            "img_static" -> {
                                val imgRef = config.selectedStaticImages.randomOrNull()
                                if (imgRef != null) {
                                    BackgroundGenerator.createStaticImageBackground(
                                        context, imgRef, 512
                                    )
                                } else {
                                    BackgroundGenerator.createBackground(512, finalBg, maskBmp)
                                }
                            }

                            "img_filling" -> {
                                val imgRef = config.selectedFillingImages.randomOrNull()
                                if (imgRef != null) {
                                    BackgroundGenerator.createImageFillingBackground(
                                        context = context,
                                        imageRef = imgRef,
                                        iconSize = 512,
                                        maskBitmap = maskBmp,
                                        randomRotation = config.imageFilling.randomRotation,
                                        scaleMode = config.imageFilling.scaleMode
                                    )
                                } else {
                                    BackgroundGenerator.createBackground(512, finalBg, maskBmp)
                                }
                            }

                            else -> BackgroundGenerator.createBackground(512, finalBg, maskBmp)
                        } ?: BackgroundGenerator.createBackground(512, finalBg, maskBmp)

                        // 双层背景合成：下层背景（较大）居中放置，上层背景铺满覆盖
                        val bgBitmap = if (config.dualLayerEnabled && config.bgStyle != "none") {
                            val bgLayer2 = config.bgLayer2
                            // 下层背景尺寸 = iconSize × (1 + sizeDiff)
                            val lowerSize =
                                kotlin.math.ceil(512 * (1 + config.dualLayerSizeDiff)).toInt()
                            val maskBmp2 =
                                if (maskBitmaps2.isNotEmpty()) maskBitmaps2.random() else null
                            val currentBg2 = resolveConfigColors(
                                isFg = false,
                                config = config,
                                wallpaperColorScheme = wallpaperColorScheme,
                                appColorSchemes = appColorSchemes,
                                packageName = packageName,
                                layerIndex = 1
                            )
                            val finalBg2 = if (bgLayer2.style == "none") "#00000000" else currentBg2
                            val lowerBg = when (bgLayer2.style) {
                                "img_static" -> {
                                    val imgRef2 = bgLayer2.selectedStaticImages.randomOrNull()
                                    if (imgRef2 != null) {
                                        BackgroundGenerator.createStaticImageBackground(
                                            context, imgRef2, lowerSize
                                        )
                                    } else null
                                }

                                "img_filling" -> {
                                    val imgRef2 = bgLayer2.selectedFillingImages.randomOrNull()
                                    if (imgRef2 != null) {
                                        BackgroundGenerator.createImageFillingBackground(
                                            context = context,
                                            imageRef = imgRef2,
                                            iconSize = lowerSize,
                                            maskBitmap = maskBmp2,
                                            randomRotation = bgLayer2.imageFilling.randomRotation,
                                            scaleMode = bgLayer2.imageFilling.scaleMode
                                        )
                                    } else null
                                }

                                else -> BackgroundGenerator.createBackground(
                                    lowerSize, finalBg2, maskBmp2
                                )
                            }
                            if (lowerBg != null) {
                                BackgroundGenerator.mergeDualLayerBackground(
                                    lowerBg = lowerBg,
                                    upperBg = upperBg,
                                    iconSize = 512,
                                    lowerAlpha = bgLayer2.alpha
                                ).also {
                                    lowerBg.recycle()
                                    upperBg.recycle()
                                }
                            } else upperBg
                        } else upperBg

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
        packageName: String? = null,
        layerIndex: Int = 0
    ): String {
        // layerIndex=0 读上层背景字段，layerIndex=1 读下层背景字段（bgLayer2）
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

    private fun isColorWhite(hex: String): Boolean {
        return try {
            val color = hex.toColorInt()
            Color.red(color) > 240 && Color.green(color) > 240 && Color.blue(color) > 240
        } catch (_: Exception) {
            false
        }
    }
}
