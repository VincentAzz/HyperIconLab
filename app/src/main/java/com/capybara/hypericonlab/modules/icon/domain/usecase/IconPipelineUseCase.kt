package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.color.HslColorUtils
import com.capybara.hypericonlab.core.image.BackgroundGenerator
import com.capybara.hypericonlab.core.image.GlassProcessor
import com.capybara.hypericonlab.core.image.LayerMerger
import com.capybara.hypericonlab.core.image.StickerProcessor
import com.capybara.hypericonlab.core.image.SvgProcessor
import com.capybara.hypericonlab.modules.icon.domain.model.ColorMode
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
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
        maskBitmaps2: List<Bitmap> = emptyList()
    ): Flow<PipelineProgress> = flow {
        val total = iconMap.size
        var completed = 0

        // 流式模式：逐个写入ZIP
        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zipOut ->
                iconMap.forEach { (packageName, drawableName) ->
                    val svgFile = File(svgDir, "$drawableName.svg")
                    val (currentFg, currentBg) = resolveColors(config, packageName, appColorSchemes)

                    val iconBitmap = SvgProcessor.processSvgFile(
                        svgFile = svgFile,
                        strokeWidthRatio = config.strokeWidthRatio,
                        fgColorHex = if (config.fgStyle == "sticker") "#FF000000" else {
                            if (config.fgStyle == "hollow" || config.fgStyle == "glass") "#00000000" else currentFg
                        },
                        iconSize = config.iconSize,
                        iconScale = if (config.fgStyle == "sticker") 1.0f else config.iconScale
                    )

                    if (iconBitmap != null) {
                        val processedIcon: Bitmap? =
                            if (config.fgStyle == "sticker" && config.stickerConfig != null) {
                                // 自动颜色优化
                                val finalStickerConfig =
                                    if (config.colorMode == ColorMode.BLACK_WHITE) {
                                        config.stickerConfig.copy(
                                            fillStyle = "fill",
                                            lineColor = "#FF000000",
                                            fillColor = "#FFFFFFFF"
                                        )
                                    } else if (config.stickerConfig.fillColor == config.bgColorHex && config.stickerConfig.fillStyle != "none") {
                                        try {
                                            val base = currentFg.toColorInt()
                                            val alpha = 20
                                            config.stickerConfig.copy(
                                                fillColor = String.format(
                                                    "#%02X%06X",
                                                    alpha,
                                                    base and 0xFFFFFF
                                                )
                                            )
                                        } catch (_: Exception) {
                                            config.stickerConfig
                                        }
                                    } else {
                                        config.stickerConfig
                                    }

                                StickerProcessor.process(
                                    originalIcon = iconBitmap,
                                    config = finalStickerConfig,
                                    iconId = drawableName,
                                    iconScale = config.iconScale
                                ).also {
                                    iconBitmap.recycle()
                                }
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
                            val upperBg = when (config.bgStyle) {
                                "img_static" -> {
                                    val imgRef = config.selectedStaticImages.randomOrNull()
                                    if (imgRef != null) {
                                        BackgroundGenerator.createStaticImageBackground(
                                            context, imgRef, config.iconSize
                                        )
                                    } else null
                                }

                                "img_filling" -> {
                                    val imgRef = config.selectedFillingImages.randomOrNull()
                                    if (imgRef != null) {
                                        BackgroundGenerator.createImageFillingBackground(
                                            context = context,
                                            imageRef = imgRef,
                                            iconSize = config.iconSize,
                                            maskBitmap = maskBmp,
                                            randomRotation = config.imageFillingRandomRotation,
                                            scaleMode = config.imageFillingScaleMode
                                        )
                                    } else null
                                }

                                else -> null
                            } ?: BackgroundGenerator.createBackground(
                                iconSize = config.iconSize,
                                colorHex = fallbackColor,
                                maskBitmap = maskBmp
                            )

                            // 先合成上层背景 + 图标 → upperComposite（iconSize×iconSize）
                            val upperComposite = LayerMerger.merge(
                                background = upperBg,
                                icon = processedIcon,
                                subtractIcon = config.fgStyle == "hollow",
                                fgAlpha = if (config.fgStyle == "glass") 1.0f else resolveAlpha(
                                    currentFg
                                )
                            )
                            upperBg.recycle()

                            // 双层合成：上层（含图标）缩放后居中绘制到下层（铺满 iconSize）之上
                            // 下层颜色解析：app 源按 packageName 实时解析（同源时应用 HSL 亮度互补），
                            // 其余源（wallpaper/preset/ctc/custom/black_white）由 ViewModel 预解析到 config.bgColor2
                            val finalBitmap =
                                if (config.dualLayerEnabled && config.bgStyle != "none") {
                                    val maskBmp2 =
                                        if (maskBitmaps2.isNotEmpty()) maskBitmaps2.random() else null
                                    val currentBg2 = if (config.bgStyle2 == "none") {
                                        "#00000000"
                                    } else if (config.bgColorSource2 == "app") {
                                        // app 源：每个图标颜色不同，按 packageName 实时解析
                                        val appBg =
                                            appColorSchemes[packageName]?.second ?: config.bgColor2
                                        // 同源优化：上下层均为 app 时，对下层应用亮度互补
                                        if (config.bgColorSource == "app") {
                                            HslColorUtils.adjustLuminanceForContrast(appBg)
                                        } else appBg
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

                                        else -> null
                                    } ?: BackgroundGenerator.createBackground(
                                        iconSize = config.iconSize,
                                        colorHex = currentBg2,
                                        maskBitmap = maskBmp2
                                    )
                                    // 上层合成图缩放到 upperRenderSize（居中放置到下层之上）
                                    val upperScaled =
                                        upperComposite.scale(upperRenderSize, upperRenderSize)
                                    BackgroundGenerator.mergeDualLayerBackground(
                                        lowerBg = lowerBg,
                                        upperBg = upperScaled,
                                        iconSize = config.iconSize,
                                        lowerAlpha = config.bgLayer2Alpha
                                    ).also {
                                        lowerBg.recycle()
                                        upperScaled.recycle()
                                        upperComposite.recycle()
                                    }
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
        return when (config.colorMode) {
            ColorMode.COLORFUL -> {
                schemes[packageName] ?: Pair(config.fgColorHex, config.bgColorHex)
            }

            ColorMode.BLACK_WHITE -> {
                Pair("#FF000000", "#FFFFFFFF")
            }

            else -> Pair(config.fgColorHex, config.bgColorHex)
        }
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
