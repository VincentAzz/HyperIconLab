package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.caverock.androidsvg.SVG
import java.io.File

// svg 处理器
object SvgProcessor {


    fun processSvgFile(
        svgFile: File,
        strokeWidthRatio: Float = 1.0f,
        fgColorHex: String? = null,
        iconSize: Int = 512,
        iconScale: Float = 1.0f
    ): Bitmap? {
        if (!svgFile.exists()) return null
        return processSvg(svgFile.readText(), strokeWidthRatio, fgColorHex, iconSize, iconScale)
    }

    fun processSvg(
        svgContent: String,
        strokeWidthRatio: Float = 1.0f,
        fgColorHex: String? = null,
        iconSize: Int = 512,
        iconScale: Float = 1.0f
    ): Bitmap? {
        try {
            var processedSvg = svgContent

            if (strokeWidthRatio != 1.0f) {
                val regex = Regex("""stroke-width="([\d.]+)"""")
                processedSvg = regex.replace(processedSvg) { matchResult ->
                    val originalValue = matchResult.groupValues[1].toFloatOrNull() ?: 1.0f
                    val newValue = originalValue * strokeWidthRatio
                    """stroke-width="$newValue""""
                }
            }

            if (fgColorHex != null) {
                val hasAlpha = fgColorHex.startsWith("#") && fgColorHex.length == 9
                val colorForSvg = if (hasAlpha) {
                    "#" + fgColorHex.substring(3)
                } else {
                    fgColorHex
                }

                processedSvg = processedSvg.replace(
                    Regex("#000(?:000)?", RegexOption.IGNORE_CASE),
                    colorForSvg
                )
                processedSvg = processedSvg.replace(
                    Regex("""fill="black"""", RegexOption.IGNORE_CASE),
                    """fill="$colorForSvg""""
                )
                processedSvg = processedSvg.replace(
                    Regex("""stroke="black"""", RegexOption.IGNORE_CASE),
                    """stroke="$colorForSvg""""
                )

                if (hasAlpha) {
                    val alphaHex = fgColorHex.substring(1, 3)
                    val alpha = alphaHex.toInt(16) / 255f
                    if (alpha < 0.99f) {
                        processedSvg = processedSvg.replace(
                            """fill="$colorForSvg"""",
                            """fill="$colorForSvg" fill-opacity="$alpha""""
                        )
                        processedSvg = processedSvg.replace(
                            """stroke="$colorForSvg"""",
                            """stroke="$colorForSvg" stroke-opacity="$alpha""""
                        )
                    }
                }
            }

            // 3. Render SVG
            val svg = SVG.getFromString(processedSvg)

            if (svg.documentViewBox == null) {
                val width = svg.documentWidth
                val height = svg.documentHeight
                if (width > 0 && height > 0) {
                    svg.setDocumentViewBox(0f, 0f, width, height)
                }
            }

            try {
                svg.setDocumentWidth("100%")
                svg.setDocumentHeight("100%")
            } catch (e: Exception) {
                try {
                    svg.documentWidth = iconSize.toFloat()
                    svg.documentHeight = iconSize.toFloat()
                } catch (inner: Exception) {
                    // Ignore if width/height are also read-only
                }
            }

            val iconActualSize = (iconSize * iconScale).toInt()

            val bitmap = createBitmap(iconSize, iconSize)
            val canvas = Canvas(bitmap)

            val offset = (iconSize - iconActualSize) / 2f
            val destRect = RectF(offset, offset, offset + iconActualSize, offset + iconActualSize)

            svg.renderToCanvas(canvas, destRect)

            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}