package com.capybara.hypericonlab.modules.iconrender

import android.graphics.Bitmap
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.core.image.StickerProcessor
import com.capybara.hypericonlab.core.image.SvgProcessor
import com.capybara.hypericonlab.modules.icon.domain.model.ColorMode
import com.capybara.hypericonlab.modules.icon.domain.model.IconBuildConfig
import com.capybara.hypericonlab.modules.icon.domain.model.IconConfigState
import com.capybara.hypericonlab.modules.icon.domain.model.StickerConfig
import java.io.File

// 图标前景渲染器，统一 SVG 基础渲染与前景色决策
object IconForegroundRenderer {

    // 渲染 SVG 基础位图，按 fgStyle 决定 fgColorHex 与 iconScale
    fun renderBaseSvg(
        svgFile: File,
        fgStyle: String,
        strokeWidthRatio: Float,
        currentFg: String,
        iconSize: Int,
        iconScale: Float
    ): Bitmap? {
        val fgColorHex = if (fgStyle == "sticker") "#FF000000" else {
            if (fgStyle == "hollow" || fgStyle == "glass") "#00000000" else currentFg
        }
        val scale = if (fgStyle == "sticker") 1.0f else iconScale
        return SvgProcessor.processSvgFile(
            svgFile = svgFile,
            strokeWidthRatio = strokeWidthRatio,
            fgColorHex = fgColorHex,
            iconSize = iconSize,
            iconScale = scale
        )
    }

    // 渲染 sticker 前景
    fun renderStickerFromUiState(
        iconBitmap: Bitmap,
        config: IconConfigState,
        currentFg: String,
        currentBg: String,
        drawableName: String
    ): Bitmap {
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
        return StickerProcessor.process(iconBitmap, stickerConfig, drawableName, config.iconScale)
    }

    // 渲染 sticker 前景，返回新位图并回收原图
    // 调用前需确保 config.stickerConfig 非空
    fun renderStickerFromBuildConfig(
        iconBitmap: Bitmap,
        config: IconBuildConfig,
        currentFg: String,
        drawableName: String
    ): Bitmap {
        val stickerConfig = config.stickerConfig!!
        val finalStickerConfig =
            if (config.colorMode == ColorMode.BLACK_WHITE) {
                stickerConfig.copy(
                    fillStyle = "fill",
                    lineColor = "#FF000000",
                    fillColor = "#FFFFFFFF"
                )
            } else if (stickerConfig.fillColor == config.bgColorHex && stickerConfig.fillStyle != "none") {
                try {
                    val base = currentFg.toColorInt()
                    val alpha = 20
                    stickerConfig.copy(
                        fillColor = String.format("#%02X%06X", alpha, base and 0xFFFFFF)
                    )
                } catch (_: Exception) {
                    stickerConfig
                }
            } else {
                stickerConfig
            }
        return StickerProcessor.process(
            originalIcon = iconBitmap,
            config = finalStickerConfig,
            iconId = drawableName,
            iconScale = config.iconScale
        ).also {
            iconBitmap.recycle()
        }
    }
}
