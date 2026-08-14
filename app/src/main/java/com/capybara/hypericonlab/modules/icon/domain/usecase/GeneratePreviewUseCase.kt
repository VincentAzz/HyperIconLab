package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.designsystem.color.utils.MonetColorExtractor
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.LawniconsAssetFacade
import com.capybara.hypericonlab.modules.icon.domain.lawnicons.mapper.IconMapperProcessor
import com.capybara.hypericonlab.modules.icon.domain.model.GlassConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.render.BackgroundLayerRenderer
import com.capybara.hypericonlab.modules.render.ConfigColorResolver
import com.capybara.hypericonlab.modules.render.DualLayerComposer
import com.capybara.hypericonlab.modules.render.IconForegroundRenderer
import com.capybara.hypericonlab.modules.render.RetroStyleRenderer
import com.capybara.hypericonlab.modules.render.image.BackgroundGenerator
import com.capybara.hypericonlab.modules.render.image.GlassProcessor
import com.capybara.hypericonlab.modules.render.image.InnerShadowBitmapLoader
import com.capybara.hypericonlab.modules.render.image.LayerMerger
import com.capybara.hypericonlab.modules.render.image.MaskAssetLoader
import com.capybara.hypericonlab.modules.render.preview.PreviewGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class GeneratePreviewUseCase(
    private val context: Context,
    private val assetsFacade: LawniconsAssetFacade
) {

    suspend fun execute(
        config: IconConfigState,
        wallpaperBitmap: Bitmap?,
        wallpaperColorScheme: MonetColorExtractor.WallpaperColorScheme?,
        appColorSchemes: Map<String, Pair<String, String>>,
        onStorePreviewGenerated: (Bitmap) -> Unit
    ): Bitmap? = withContext(Dispatchers.Default) {
        // 获取当前资源，优先使用 preview mapper
        val provider = assetsFacade.getProvider()
        val fullMap = withContext(Dispatchers.IO) {
            val previewStream = try {
                provider.openIconMapper(PreviewConstants.PREVIEW_MAPPER_FILE)
            } catch (_: Exception) {
                null
            }
            previewStream?.use { IconMapperProcessor.parseIconMapper(it) }
                ?: provider.openIconMapper(PreviewConstants.FULL_MAPPER_FILE)
                    .use { IconMapperProcessor.parseIconMapper(it) }
        }
        val svgDir = withContext(Dispatchers.IO) {
            provider.getSvgDir()
        }

        if (svgDir == null) return@withContext null

        val masks = config.selectedMasks
        val maskBitmaps = withContext(Dispatchers.IO) {
            masks.mapNotNull { name -> MaskAssetLoader.loadBitmap(context, name) }
        }

        // 下层背景独立 mask（双层启用时加载）
        val maskBitmaps2 = if (config.dualLayerEnabled) {
            withContext(Dispatchers.IO) {
                config.bgLayer2.selectedMasks.mapNotNull { name ->
                    MaskAssetLoader.loadBitmap(context, name)
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
                    packageName,
                    context = context
                )
                val currentBg = ConfigColorResolver.resolveConfigColors(
                    false,
                    config,
                    wallpaperColorScheme,
                    appColorSchemes,
                    packageName,
                    context = context
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

                        val upperBgResult = BackgroundLayerRenderer.renderUpperBackground(
                            context = context,
                            bgStyle = config.bgStyle,
                            iconSize = finalIconSize,
                            fallbackColorHex = finalBg,
                            maskBitmap = maskBmp,
                            imgStaticRef = config.selectedStaticImages.randomOrNull(),
                            imgFillingRef = config.selectedFillingImages.randomOrNull(),
                            fillingRandomRotation = config.imageFilling.randomRotation,
                            fillingScaleMode = config.imageFilling.scaleMode,
                            retroShapeName = config.selectedMasks.firstOrNull()
                        )

                        // 先合成上层背景 + 装饰层 + 内阴影 + 图标 → upperComposite（finalIconSize×finalIconSize）
                        // 内阴影仅在单层背景生效：双层背景时 innerShadowBitmap 应为 null
                        val upperComposite = LayerMerger.merge(
                            background = upperBgResult.background,
                            icon = processedIcon,
                            decorLayers = upperBgResult.decorLayers,
                            innerShadow = innerShadowBitmap,
                            subtractIcon = config.fgStyle == "hollow",
                            fgAlpha = if (config.fgStyle == "glass") 1.0f else try {
                                Color.alpha(finalFg.toColorInt()) / 255f
                            } catch (_: Exception) {
                                1.0f
                            }
                        )
                        upperBgResult.background.recycle()

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
                                layerIndex = 1,
                                context = context
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

                                "retro" -> {
                                    // 下层 Retro：tint 底板 + 装饰图层扁平化为单个 bitmap
                                    val shape = bgLayer2.selectedMasks.firstOrNull() ?: "ios27"
                                    RetroStyleRenderer.render(
                                        context = context,
                                        shapeName = shape,
                                        iconSize = finalIconSize,
                                        tintColorHex = finalBg2,
                                        maskBitmap = maskBmp2
                                    ).toFlattenedBitmap()
                                }

                                else -> BackgroundGenerator.createBackground(
                                    finalIconSize, finalBg2, maskBmp2
                                )
                            }
                            // 缩放上层 + 计算 alpha + 合并 + 回收，统一交给 DualLayerComposer
                            DualLayerComposer.compose(
                                upperComposite = upperComposite,
                                lowerBg = lowerBg,
                                iconSize = finalIconSize,
                                upperRenderSize = upperRenderSize,
                                bgStyle2 = bgLayer2.style,
                                configuredAlpha = bgLayer2.alpha
                            )
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

    // mapper 文件名常量
    private object PreviewConstants {
        const val PREVIEW_MAPPER_FILE = "icon_mapper_preview.xml"
        const val FULL_MAPPER_FILE = "icon_mapper.xml"
    }
}
