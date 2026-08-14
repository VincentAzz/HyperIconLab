package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.designsystem.color.utils.HslColorUtils
import com.capybara.hypericonlab.modules.icon.domain.model.ColorMode
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
import com.capybara.hypericonlab.modules.render.BackgroundLayerRenderer
import com.capybara.hypericonlab.modules.render.ConfigColorResolver
import com.capybara.hypericonlab.modules.render.DualLayerComposer
import com.capybara.hypericonlab.modules.render.IconForegroundRenderer
import com.capybara.hypericonlab.modules.render.RetroStyleRenderer
import com.capybara.hypericonlab.modules.render.image.BackgroundGenerator
import com.capybara.hypericonlab.modules.render.image.GlassProcessor
import com.capybara.hypericonlab.modules.render.image.LayerMerger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.ceil


// 图标生成流水线
class IconPipelineUseCase(
    private val context: Context
) {

    fun executeWithFiles(
        config: IconBuildConfig,
        iconMap: Map<String, String>,
        svgDir: File,
        maskBitmaps: List<Bitmap> = emptyList(),
        outputFile: File,
        appColorSchemes: Map<String, Pair<String, String>> = emptyMap(),
        maskBitmaps2: List<Bitmap> = emptyList(),
        // 内阴影位图（已按 iconSize 缩放，仅单层背景且 innerShadow.enabled 时非空）
        innerShadowBitmap: Bitmap? = null
    ): Flow<PipelineProgress> = flow {
        val total = iconMap.size
        var completed = 0

        // 流式模式：逐个写入ZIP
        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zipOut ->
                iconMap.forEach { (packageName, drawableName) ->
                    val svgFile = File(svgDir, "$drawableName.svg")
                    val (currentFg, currentBg) = resolveColors(config, packageName, appColorSchemes)

                    val iconBitmap = IconForegroundRenderer.renderBaseSvg(
                        svgFile = svgFile,
                        fgStyle = config.fgStyle,
                        strokeWidthRatio = config.strokeWidthRatio,
                        currentFg = currentFg,
                        iconSize = config.iconSize,
                        iconScale = config.iconScale
                    )

                    if (iconBitmap != null) {
                        val processedIcon: Bitmap? =
                            if (config.fgStyle == "sticker" && config.stickerConfig != null) {
                                IconForegroundRenderer.renderStickerFromBuildConfig(
                                    iconBitmap, config, currentFg, drawableName
                                )
                            } else if (config.fgStyle == "glass" && config.glassConfig != null) {
                                val (cFg, cBg) = resolveColors(config, packageName, appColorSchemes)
                                val isFgWhite = resolveAlpha(cFg) > 0.9f
                                // 如果前景色不是白色，优先使用前景色作为渐变基础
                                val glassThemeColor = if (!isFgWhite) cFg else cBg

                                GlassProcessor.process(
                                    svgFile = svgFile,
                                    baseStrokeWidthRatio = config.strokeWidthRatio,
                                    iconSize = config.iconSize,
                                    iconScale = config.iconScale,
                                    themeColorHex = glassThemeColor,
                                    isFgWhite = isFgWhite,
                                    glassConfig = config.glassConfig
                                ).also {
                                    iconBitmap.recycle()
                                }
                            } else {
                                iconBitmap
                            }

                        if (processedIcon != null) {
                            val maskBmp =
                                if (maskBitmaps.isNotEmpty()) maskBitmaps.random() else null
                            val fallbackColor =
                                if (config.bgStyle == "none") "#00000000" else currentBg
                            // 上层背景渲染尺寸（双层启用时缩小，否则为最终尺寸）
                            val upperRenderSize =
                                if (config.dualLayerEnabled && config.bgStyle != "none") {
                                    ceil(config.iconSize * (1 - config.dualLayerSizeDiff)).toInt()
                                        .coerceAtLeast(1)
                                } else config.iconSize
                            val upperBgResult = BackgroundLayerRenderer.renderUpperBackground(
                                context = context,
                                bgStyle = config.bgStyle,
                                iconSize = config.iconSize,
                                fallbackColorHex = fallbackColor,
                                maskBitmap = maskBmp,
                                imgStaticRef = config.selectedStaticImages.randomOrNull(),
                                imgFillingRef = config.selectedFillingImages.randomOrNull(),
                                fillingRandomRotation = config.imageFillingRandomRotation,
                                fillingScaleMode = config.imageFillingScaleMode,
                                retroShapeName = config.masks.firstOrNull()
                            )

                            // 先合成上层背景 + 装饰层 + 内阴影 + 图标 → upperComposite（iconSize×iconSize）
                            // 内阴影仅在单层背景生效：双层背景时 innerShadowBitmap 应为 null
                            val upperComposite = LayerMerger.merge(
                                background = upperBgResult.background,
                                icon = processedIcon,
                                decorLayers = upperBgResult.decorLayers,
                                innerShadow = innerShadowBitmap,
                                subtractIcon = config.fgStyle == "hollow",
                                fgAlpha = if (config.fgStyle == "glass") 1.0f else resolveAlpha(
                                    currentFg
                                )
                            )
                            upperBgResult.background.recycle()

                            // 双层合成：上层（含图标）缩放后居中绘制到下层（铺满 iconSize）之上
                            // 下层颜色解析：app/app_m3 源按 packageName 实时解析（app 同源时应用 HSL 亮度互补），
                            // 其余源（wallpaper/preset/ctc/custom/black_white）由 ViewModel 预解析到 config.bgColor2
                            val finalBitmap =
                                if (config.dualLayerEnabled && config.bgStyle != "none") {
                                    val maskBmp2 =
                                        if (maskBitmaps2.isNotEmpty()) maskBitmaps2.random() else null
                                    val currentBg2 = if (config.bgStyle2 == "none") {
                                        "#00000000"
                                    } else if (config.bgColorSource2 == "app") {
                                        // app 源：每个图标颜色不同，按 packageName 实时解析
                                        val appBg = ConfigColorResolver.resolveAppColors(
                                            isFg = false,
                                            appColorSchemes = appColorSchemes,
                                            packageName = packageName,
                                            reduceWhiteBg = config.bgLayer2AppReduceWhiteBg,
                                            bgStyleNone = false,
                                            defaultColor = config.bgColor2
                                        )
                                        // 同源优化：上下层均为 app 时，对下层应用亮度互补
                                        if (config.bgColorSource == "app") {
                                            HslColorUtils.adjustLuminanceForContrast(appBg)
                                        } else appBg
                                    } else if (config.bgColorSource2 == "app_m3") {
                                        // app_m3 源：每个图标颜色不同，按 packageName 实时解析 M3 scheme
                                        ConfigColorResolver.resolveAppM3Colors(
                                            isFg = false,
                                            context = context,
                                            appColorSchemes = appColorSchemes,
                                            packageName = packageName,
                                            themeMode = config.bgPreviewThemeMode2,
                                            reduceWhiteBg = config.bgLayer2AppReduceWhiteBg,
                                            bgStyleNone = false,
                                            defaultColor = config.bgColor2
                                        )
                                    } else {
                                        config.bgColor2
                                    }
                                    val lowerBg = when (config.bgStyle2) {
                                        "img_static" -> {
                                            val imgRef2 =
                                                config.selectedStaticImages2.randomOrNull()
                                            if (imgRef2 != null) {
                                                BackgroundGenerator.createStaticImageBackground(
                                                    context, imgRef2, config.iconSize
                                                )
                                            } else null
                                        }

                                        "img_filling" -> {
                                            val imgRef2 =
                                                config.selectedFillingImages2.randomOrNull()
                                            if (imgRef2 != null) {
                                                BackgroundGenerator.createImageFillingBackground(
                                                    context = context,
                                                    imageRef = imgRef2,
                                                    iconSize = config.iconSize,
                                                    maskBitmap = maskBmp2,
                                                    randomRotation = config.imageFilling2RandomRotation,
                                                    scaleMode = config.imageFilling2ScaleMode
                                                )
                                            } else null
                                        }

                                        "retro" -> {
                                            // 下层 Retro：tint 底板 + 装饰图层扁平化为单个 bitmap
                                            // DualLayerComposer 仅接收单个 bitmap，故装饰层在此合并
                                            val shape =
                                                config.selectedMasks2.firstOrNull() ?: "ios27"
                                            RetroStyleRenderer.render(
                                                context = context,
                                                shapeName = shape,
                                                iconSize = config.iconSize,
                                                tintColorHex = currentBg2,
                                                maskBitmap = maskBmp2
                                            ).toFlattenedBitmap()
                                        }

                                        else -> null
                                    } ?: BackgroundGenerator.createBackground(
                                        iconSize = config.iconSize,
                                        colorHex = currentBg2,
                                        maskBitmap = maskBmp2
                                    )
                                    // 缩放上层 + 计算 alpha + 合并 + 回收，统一交给 DualLayerComposer
                                    DualLayerComposer.compose(
                                        upperComposite = upperComposite,
                                        lowerBg = lowerBg,
                                        iconSize = config.iconSize,
                                        upperRenderSize = upperRenderSize,
                                        bgStyle2 = config.bgStyle2,
                                        configuredAlpha = config.bgLayer2Alpha
                                    )
                                } else upperComposite

                            val entry = ZipEntry("icons/$packageName.png")
                            zipOut.putNextEntry(entry)
                            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                            zipOut.closeEntry()

                            if (processedIcon !== iconBitmap) processedIcon.recycle()
                            finalBitmap.recycle()
                        }
                    }
                    completed++
                    emit(PipelineProgress.Processing(completed, total, packageName))
                }
            }
        }

        emit(PipelineProgress.Complete(outputFile))
    }.flowOn(Dispatchers.IO)

    private fun resolveColors(
        config: IconBuildConfig,
        packageName: String,
        schemes: Map<String, Pair<String, String>>
    ): Pair<String, String> {
        // BLACK_WHITE 模式固定黑白，忽略 colorSource
        if (config.colorMode == ColorMode.BLACK_WHITE) {
            return Pair("#FF000000", "#FFFFFFFF")
        }
        // COLORFUL 模式优先用 appColorSchemes（每个图标颜色不同），回退到按 source 解析
        if (config.colorMode == ColorMode.COLORFUL) {
            schemes[packageName]?.let { return it }
        }
        // CUSTOM 与 COLORFUL 回退分支：按 colorSource 决定颜色
        // - "app" 源：用 appColorSchemes[packageName]（每个图标颜色不同），缺失则回退到预填色
        // - "app_m3" 源：用应用颜色作为 M3 种子色生成 scheme 取色（每个图标不同）
        // - 其他源（wallpaper/preset/ctc/custom/black_white）：使用 ViewModel 预解析后填入的 fgColorHex/bgColorHex
        val currentFg = when (config.fgColorSource) {
            "app" -> ConfigColorResolver.resolveAppColors(
                isFg = true,
                appColorSchemes = schemes,
                packageName = packageName,
                reduceWhiteBg = config.appReduceWhiteBg,
                bgStyleNone = config.bgStyle == "none",
                defaultColor = config.fgColorHex
            )

            "app_m3" -> ConfigColorResolver.resolveAppM3Colors(
                isFg = true,
                context = context,
                appColorSchemes = schemes,
                packageName = packageName,
                themeMode = config.previewThemeMode,
                reduceWhiteBg = config.appReduceWhiteBg,
                bgStyleNone = config.bgStyle == "none",
                defaultColor = config.fgColorHex
            )

            else -> config.fgColorHex
        }
        val currentBg = when (config.bgColorSource) {
            "app" -> ConfigColorResolver.resolveAppColors(
                isFg = false,
                appColorSchemes = schemes,
                packageName = packageName,
                reduceWhiteBg = config.appReduceWhiteBg,
                bgStyleNone = config.bgStyle == "none",
                defaultColor = config.bgColorHex
            )

            "app_m3" -> ConfigColorResolver.resolveAppM3Colors(
                isFg = false,
                context = context,
                appColorSchemes = schemes,
                packageName = packageName,
                themeMode = config.previewThemeMode,
                reduceWhiteBg = config.appReduceWhiteBg,
                bgStyleNone = config.bgStyle == "none",
                defaultColor = config.bgColorHex
            )

            else -> config.bgColorHex
        }
        return Pair(currentFg, currentBg)
    }

    private fun resolveAlpha(hex: String): Float {
        return try {
            Color.alpha(hex.toColorInt()) / 255f
        } catch (e: Exception) {
            1.0f
        }
    }

    sealed class PipelineProgress {
        data class Processing(val current: Int, val total: Int, val packageName: String) :
            PipelineProgress()

        data class Complete(val file: File) : PipelineProgress()
        data class Error(val message: String) : PipelineProgress()
    }
}
