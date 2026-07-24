package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.MonetColorExtractor
import com.capybara.hypericonlab.core.image.BackgroundGenerator
import com.capybara.hypericonlab.core.image.GlassProcessor
import com.capybara.hypericonlab.core.image.InnerShadowBitmapLoader
import com.capybara.hypericonlab.core.image.LayerMerger
import com.capybara.hypericonlab.core.mapper.IconMapperProcessor
import com.capybara.hypericonlab.core.preview.PreviewGenerator
import com.capybara.hypericonlab.core.utils.ZipUtils
import com.capybara.hypericonlab.modules.icon.domain.model.GlassConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.render.ConfigColorResolver
import com.capybara.hypericonlab.modules.icon.domain.render.IconForegroundRenderer
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
        val innerShadowBitmap = if (config.innerShadow.enabled && !config.dualLayerEnabled) {
            config.innerShadow.styleName?.let { styleName ->
                config.selectedMasks.firstOrNull()?.let { shapeName ->
                    withContext(Dispatchers.IO) {
                        InnerShadowBitmapLoader.loadAndMerge(
                            shapeName,
                            styleName,
                            InnerShadowPreviewConfig.SHADOW_SIZE,
                            config.innerShadow.intensityLayers,
                            context
                        )
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
                val currentFg = ConfigColorResolver.resolveConfigColors(
                    true,
                    config,
                    wallpaperColorScheme,
                    appColorSchemes,
                    packageName
                )
                val currentBg = ConfigColorResolver.resolveConfigColors(
                    false,
                    config,
                    wallpaperColorScheme,
                    appColorSchemes,
                    packageName
                )

                val finalFg =
                    if (config.fgStyle == "hollow" || config.fgStyle == "glass") "#00000000" else currentFg
                val finalBg = if (config.bgStyle == "none") "#00000000" else currentBg

                val iconBitmap = IconForegroundRenderer.renderBaseSvg(
                    svgFile = svgFile,
                    fgStyle = config.fgStyle,
                    strokeWidthRatio = config.strokeWidthRatio,
                    currentFg = currentFg,
                    iconSize = 512,
                    iconScale = config.iconScale
                )

                if (iconBitmap != null) {
                    val processedIcon = when (config.fgStyle) {
                        "sticker" -> IconForegroundRenderer.renderStickerFromUiState(
                            iconBitmap, config, currentFg, currentBg, drawableName
                        )

                        "glass" -> {
                            val glassThemeColor =
                                if (!ConfigColorResolver.isColorWhite(currentFg)) currentFg else currentBg
                            GlassProcessor.process(
                                svgFile = svgFile,
                                baseStrokeWidthRatio = config.strokeWidthRatio,
                                iconSize = 512,
                                iconScale = config.iconScale,
                                themeColorHex = glassThemeColor,
                                isFgWhite = ConfigColorResolver.isColorWhite(currentFg),
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
                            val currentBg2 = ConfigColorResolver.resolveConfigColors(
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

    // 预览场景内阴影参数
    private object InnerShadowPreviewConfig {
        // 预览图标尺寸（与 finalIconSize 一致）
        const val SHADOW_SIZE = 512
    }
}
