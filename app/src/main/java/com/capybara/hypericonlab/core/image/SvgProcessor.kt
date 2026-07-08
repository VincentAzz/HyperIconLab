package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.caverock.androidsvg.SVG
import java.io.File

/**
 * Handles SVG to Bitmap conversion with color and stroke width support.
 */
object SvgProcessor {

    /**
     * Processes SVG from a file and returns a Bitmap.
     */
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

    /**
     * Processes SVG content and returns a Bitmap.
     */
    fun processSvg(
        svgContent: String,
        strokeWidthRatio: Float = 1.0f,
        fgColorHex: String? = null,
        iconSize: Int = 512,
        iconScale: Float = 1.0f
    ): Bitmap? {
        try {
            var processedSvg = svgContent

            // 1. Handle stroke-width adjustment
            if (strokeWidthRatio != 1.0f) {
                val regex = Regex("""stroke-width="([\d.]+)"""")
                processedSvg = regex.replace(processedSvg) { matchResult ->
                    val originalValue = matchResult.groupValues[1].toFloatOrNull() ?: 1.0f
                    val newValue = originalValue * strokeWidthRatio
                    """stroke-width="$newValue""""
                }
            }

            // 2. Handle color replacement
            if (fgColorHex != null) {
                val colorForSvg = if (fgColorHex.startsWith("#") && fgColorHex.length == 9) {
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

            val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
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