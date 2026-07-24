package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.HslColorUtils
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.designsystem.theme.ctc.CTCPresets
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme
import com.capybara.hypericonlab.core.image.BackgroundGenerator
import com.capybara.hypericonlab.core.image.GlassProcessor
import com.capybara.hypericonlab.core.image.InnerShadowAssets
import com.capybara.hypericonlab.core.image.InnerShadowProcessor
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

    /**
     * 双层背景图片类下层透明度策略。
     * 图片类下层（img_static / img_filling）始终保持完全不透明，不应用 alpha。
     */
    private object DualLayerImgAlphaPolicy {
        const val FORCED_ALPHA = 255
    }

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

        // 内阴影位图（仅单层背景且启用内阴影时加载，512 尺寸与预览图标一致）
        // 双层背景时 config.innerShadow.enabled 应为 false（ViewModel 已强制禁用）
        // 加载后预合并多层强度为单张阴影层，管线内每个图标只绘制一次
        val innerShadowBitmap = if (config.innerShadow.enabled && !config.dualLayerEnabled) {
            config.innerShadow.styleName?.let { styleName ->
                config.selectedMasks.firstOrNull()?.let { shapeName ->
                    withContext(Dispatchers.IO) {
                        loadInnerShadowBitmap(
                            shapeName,
                            styleName,
                            InnerShadowPreviewConfig.SHADOW_SIZE
                        )?.let { raw ->
                            val merged = InnerShadowProcessor.mergeShadowLayers(
                                raw, config.innerShadow.intensityLayers
                            )
                            raw.recycle()
                            merged
                        }
                    }
                }
            }
        } else null

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
            appColorSchemes,
            innerShadowBitmap
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
            appColorSchemes,
            innerShadowBitmap
        )
        val mainPreview =
            PreviewGenerator.generateMainPreview(context, fullPreviewIcons, wallpaperBitmap)

        fullPreviewIcons.forEach { it.recycle() }
        maskBitmaps.forEach { it.recycle() }
        maskBitmaps2.forEach { it.recycle() }
        innerShadowBitmap?.recycle()

        mainPreview
    }

    private suspend fun generateBitmaps(
        iconMap: Map<String, String>,
        svgDir: File,
        maskBitmaps: List<Bitmap>,
        maskBitmaps2: List<Bitmap>,
        config: IconConfigState,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>,
        innerShadowBitmap: Bitmap? = null
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
                        // 上层背景渲染尺寸（双层启用时缩小，否则为最终尺寸）
                        val finalIconSize = 512
                        val upperRenderSize =
                            if (config.dualLayerEnabled && config.bgStyle != "none") {
                                kotlin.math.ceil(finalIconSize * (1 - config.dualLayerSizeDiff))
                                    .toInt()
                                    .coerceAtLeast(1)
                            } else finalIconSize

                        val upperBg = when (config.bgStyle) {
                            "img_static" -> {
                                val imgRef = config.selectedStaticImages.randomOrNull()
                                if (imgRef != null) {
                                    BackgroundGenerator.createStaticImageBackground(
                                        context, imgRef, finalIconSize
                                    )
                                } else {
                                    BackgroundGenerator.createBackground(
                                        finalIconSize,
                                        finalBg,
                                        maskBmp
                                    )
                                }
                            }

                            "img_filling" -> {
                                val imgRef = config.selectedFillingImages.randomOrNull()
                                if (imgRef != null) {
                                    BackgroundGenerator.createImageFillingBackground(
                                        context = context,
                                        imageRef = imgRef,
                                        iconSize = finalIconSize,
                                        maskBitmap = maskBmp,
                                        randomRotation = config.imageFilling.randomRotation,
                                        scaleMode = config.imageFilling.scaleMode
                                    )
                                } else {
                                    BackgroundGenerator.createBackground(
                                        finalIconSize,
                                        finalBg,
                                        maskBmp
                                    )
                                }
                            }

                            else -> BackgroundGenerator.createBackground(
                                finalIconSize,
                                finalBg,
                                maskBmp
                            )
                        } ?: BackgroundGenerator.createBackground(finalIconSize, finalBg, maskBmp)

                        // 先合成上层背景 + 内阴影 + 图标 → upperComposite（finalIconSize×finalIconSize）
                        // 内阴影仅在单层背景生效：双层背景时 innerShadowBitmap 应为 null
                        val upperComposite = LayerMerger.merge(
                            background = upperBg,
                            icon = processedIcon,
                            innerShadow = innerShadowBitmap,
                            subtractIcon = config.fgStyle == "hollow",
                            fgAlpha = if (config.fgStyle == "glass") 1.0f else try {
                                Color.alpha(finalFg.toColorInt()) / 255f
                            } catch (_: Exception) {
                                1.0f
                            }
                        )
                        upperBg.recycle()

                        // 双层合成：上层（含图标）缩放后居中绘制到下层（铺满 finalIconSize）之上
                        val finalBitmap = if (config.dualLayerEnabled && config.bgStyle != "none") {
                            val bgLayer2 = config.bgLayer2
                            // 下层背景尺寸 = 最终图标尺寸（铺满画布）
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
                                            context, imgRef2, finalIconSize
                                        )
                                    } else null
                                }

                                "img_filling" -> {
                                    val imgRef2 = bgLayer2.selectedFillingImages.randomOrNull()
                                    if (imgRef2 != null) {
                                        BackgroundGenerator.createImageFillingBackground(
                                            context = context,
                                            imageRef = imgRef2,
                                            iconSize = finalIconSize,
                                            maskBitmap = maskBmp2,
                                            randomRotation = bgLayer2.imageFilling.randomRotation,
                                            scaleMode = bgLayer2.imageFilling.scaleMode
                                        )
                                    } else null
                                }

                                else -> BackgroundGenerator.createBackground(
                                    finalIconSize, finalBg2, maskBmp2
                                )
                            }
                            if (lowerBg != null) {
                                // 上层合成图缩放到 upperRenderSize（居中放置到下层之上）
                                val upperScaled =
                                    upperComposite.scale(upperRenderSize, upperRenderSize)
                                // 图片类下层背景始终保持完全不透明，不应用 alpha
                                val effectiveLowerAlpha = when (bgLayer2.style) {
                                    "img_static", "img_filling" -> DualLayerImgAlphaPolicy.FORCED_ALPHA
                                    else -> bgLayer2.alpha
                                }
                                BackgroundGenerator.mergeDualLayerBackground(
                                    lowerBg = lowerBg,
                                    upperBg = upperScaled,
                                    iconSize = finalIconSize,
                                    lowerAlpha = effectiveLowerAlpha
                                ).also {
                                    lowerBg.recycle()
                                    upperScaled.recycle()
                                    upperComposite.recycle()
                                }
                            } else {
                                upperComposite // 下层生成失败，回退到上层
                            }
                        } else {
                            upperComposite
                        }

                        results.add(finalBitmap)
                        if (processedIcon !== iconBitmap) processedIcon.recycle()
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

    /**
     * 加载烘焙内阴影 PNG 并缩放到目标尺寸。
     * 文件路径：assets/shadow_baked/<shapeName>_<styleName>_shadow_512.png
     */
    private fun loadInnerShadowBitmap(
        shapeName: String,
        styleName: String,
        targetSize: Int
    ): Bitmap? {
        return try {
            val path =
                "${InnerShadowAssets.DIR}/${shapeName}_${styleName}${InnerShadowAssets.FILE_SUFFIX}"
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { raw ->
                if (raw.width != targetSize) {
                    val scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
                    raw.recycle()
                    scaled
                } else raw
            }
        } catch (_: Exception) {
            null
        }
    }

    // 预览场景内阴影参数
    private object InnerShadowPreviewConfig {
        // 预览图标尺寸（与 finalIconSize 一致）
        const val SHADOW_SIZE = 512
    }
}
