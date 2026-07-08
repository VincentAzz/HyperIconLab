package com.capybara.hypericonlab.modules.icon.domain.usecase

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.toColorInt
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


// 图标生成流水线

class IconPipelineUseCase {
    fun executeWithFiles(
        config: IconBuildConfig,
        iconMap: Map<String, String>,
        svgDir: File,
        maskBitmaps: List<Bitmap> = emptyList(),
        outputFile: File,
        appColorSchemes: Map<String, Pair<String, String>> = emptyMap()
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
                            val bgBitmap = BackgroundGenerator.createBackground(
                                iconSize = config.iconSize,
                                colorHex = if (config.bgStyle == "none") "#00000000" else currentBg,
                                maskBitmap = maskBmp
                            )
                            val finalBitmap = LayerMerger.merge(
                                background = bgBitmap,
                                icon = processedIcon,
                                subtractIcon = config.fgStyle == "hollow",
                                fgAlpha = if (config.fgStyle == "glass") 1.0f else resolveAlpha(
                                    currentFg
                                )
                            )

                            val entry = ZipEntry("icons/$packageName.png")
                            zipOut.putNextEntry(entry)
                            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                            zipOut.closeEntry()

                            if (processedIcon !== iconBitmap) processedIcon.recycle()
                            bgBitmap.recycle()
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
