package com.capybara.hypericonlab.modules.render.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.capybara.hypericonlab.modules.icon.domain.model.GlassConfig
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

// 玻璃前景图标处理器
object GlassProcessor {

    fun process(
        svgFile: File,
        baseStrokeWidthRatio: Float,
        iconSize: Int,
        iconScale: Float,
        themeColorHex: String,
        isFgWhite: Boolean,
        glassConfig: GlassConfig
    ): Bitmap? {
        // 生成 baseWhite
        val baseWhite = SvgProcessor.processSvgFile(
            svgFile = svgFile,
            strokeWidthRatio = baseStrokeWidthRatio,
            fgColorHex = "#FFFFFFFF",
            iconSize = iconSize,
            iconScale = iconScale
        ) ?: return null

        // 生成 gradientLayer 较细粗细
        val thinnerRatio = (baseStrokeWidthRatio + glassConfig.strokeDiff).coerceAtLeast(0.1f)
        val gradientLayer = SvgProcessor.processSvgFile(
            svgFile = svgFile,
            strokeWidthRatio = thinnerRatio,
            fgColorHex = "#FFFFFFFF",
            iconSize = iconSize,
            iconScale = iconScale
        ) ?: return baseWhite

        // 应用渐变到 gradientLayer
        val processedGradient =
            applyGradient(gradientLayer, themeColorHex, isFgWhite, glassConfig.angle)

        // 合并图层
        val result = baseWhite.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // gradientLayer 在上
        canvas.drawBitmap(processedGradient, 0f, 0f, paint)

        // 添加阴影 (可选)
        val finalResult = if (glassConfig.shadowEnabled) {
            applyShadow(result, iconSize)
        } else {
            result
        }

        if (processedGradient !== gradientLayer) gradientLayer.recycle()
        processedGradient.recycle()
        if (finalResult !== result) result.recycle()

        return finalResult
    }

    private fun applyGradient(
        src: Bitmap,
        themeColorHex: String,
        isFgWhite: Boolean,
        angle: Float
    ): Bitmap {
        val width = src.width.toFloat()
        val height = src.height.toFloat()
        val out = createBitmap(src.width, src.height)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val themeColor = try {
            themeColorHex.toColorInt()
        } catch (e: Exception) {
            Color.BLUE
        }

        val colorStart = Color.argb(0, 255, 255, 255) // 透明白

        // 渐变中点配色为白色到主题色的渐变中点
        val midR = (255 + Color.red(themeColor)) / 2
        val midG = (255 + Color.green(themeColor)) / 2
        val midB = (255 + Color.blue(themeColor)) / 2
        val colorMid = Color.argb(128, midR, midG, midB) // 50% 不透明

        val colorEnd = Color.argb(
            230,
            Color.red(themeColor),
            Color.green(themeColor),
            Color.blue(themeColor)
        ) // 90% 不透明主题色

        val angleRad = Math.toRadians(angle.toDouble())
        val x0: Float
        val y0: Float
        val x1: Float
        val y1: Float

        when (angle) {
            -45f -> {
                x0 = 0f; y0 = 0f; x1 = width; y1 = height
            }

            0f -> {
                x0 = width / 2; y0 = 0f; x1 = width / 2; y1 = height
            }

            45f -> {
                x0 = 0f; y0 = height; x1 = width; y1 = 0f
            }

            else -> {
                val dist = width / 2
                x0 = (width / 2 - dist * cos(angleRad)).toFloat()
                y0 = (height / 2 - dist * sin(angleRad)).toFloat()
                x1 = (width / 2 + dist * cos(angleRad)).toFloat()
                y1 = (height / 2 + dist * sin(angleRad)).toFloat()
            }
        }

        val gradient = LinearGradient(
            x0, y0, x1, y1,
            intArrayOf(colorStart, colorMid, colorEnd),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )

        paint.shader = gradient
        canvas.drawRect(0f, 0f, width, height, paint)

        paint.shader = null
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)

        return out
    }

    private fun applyShadow(src: Bitmap, iconSize: Int): Bitmap {
        val out = createBitmap(iconSize, iconSize)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 64 // 25%
            setShadowLayer(16f, 8f, 8f, Color.argb(64, 0, 0, 0))
        }

        canvas.drawBitmap(src, 0f, 0f, shadowPaint)

        canvas.drawBitmap(src, 0f, 0f, paint)

        return out
    }
}
