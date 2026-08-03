package com.capybara.hypericonlab.modules.iconrender.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import kotlin.random.Random

// 图标背景生成器
object BackgroundGenerator {

    // 双层背景
    private object DualLayerDefaults {
        // 下层背景透明度
        const val LOWER_ALPHA_MAX = 255
    }

    fun createBackground(
        iconSize: Int,
        colorHex: String,
        maskBitmap: Bitmap? = null
    ): Bitmap {
        val bgBitmap = createBitmap(iconSize, iconSize)
        val canvas = Canvas(bgBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = colorHex.toColorInt()

        if (maskBitmap != null) {
            val scaledMask = if (maskBitmap.width != iconSize || maskBitmap.height != iconSize) {
                maskBitmap.scale(iconSize, iconSize)
            } else {
                maskBitmap
            }

            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
            }

            val saveCount = canvas.saveLayer(0f, 0f, iconSize.toFloat(), iconSize.toFloat(), null)
            canvas.drawRect(0f, 0f, iconSize.toFloat(), iconSize.toFloat(), paint)
            maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            canvas.drawBitmap(scaledMask, 0f, 0f, maskPaint)
            canvas.restoreToCount(saveCount)

            return bgBitmap
        }

        canvas.drawRect(0f, 0f, iconSize.toFloat(), iconSize.toFloat(), paint)
        return bgBitmap
    }


    fun createStaticImageBackground(
        context: Context,
        imageRef: String,
        iconSize: Int
    ): Bitmap? {
        val src = BgImageLoader.loadScaled(context, imageRef, iconSize) ?: return null
        return src.copy(Bitmap.Config.ARGB_8888, true).also { if (src !== it) src.recycle() }
    }


    fun createImageFillingBackground(
        context: Context,
        imageRef: String,
        iconSize: Int,
        maskBitmap: Bitmap?,
        randomRotation: Boolean,
        scaleMode: String
    ): Bitmap? {
        val src = BgImageLoader.loadScaled(context, imageRef, iconSize) ?: return null

        var current = if (scaleMode == "crop") {
            centerCropToSquare(src, iconSize)
        } else {
            src
        }

        if (randomRotation) {
            val angle = Random.nextFloat() * 360f
            val rotated = rotateExpandAndCenterCrop(current, angle, iconSize)
            if (rotated !== current) {
                if (current !== src) current.recycle()
                current = rotated
            }
        }

        val result = if (maskBitmap != null) {
            applyMask(current, maskBitmap, iconSize)
        } else {
            current.copy(Bitmap.Config.ARGB_8888, true)
        }

        if (current !== result && current !== src) current.recycle()
        if (src !== current) src.recycle()
        return result
    }


    private fun centerCropToSquare(src: Bitmap, targetSize: Int): Bitmap {
        val w = src.width
        val h = src.height
        val size = minOf(w, h)
        val left = (w - size) / 2
        val top = (h - size) / 2
        val cropped = Bitmap.createBitmap(src, left, top, size, size)
        if (size != targetSize) {
            val scaled = cropped.scale(targetSize, targetSize)
            if (scaled !== cropped) cropped.recycle()
            return scaled
        }
        return cropped
    }


    // 先放大到 iconSize*√2，再旋转并居中裁切回 iconSize，
    private fun rotateExpandAndCenterCrop(src: Bitmap, angle: Float, iconSize: Int): Bitmap {
        val scaledSize = kotlin.math.ceil(iconSize * 1.4143f).toInt()
        val upscaled = src.scale(scaledSize, scaledSize)
        if (upscaled !== src) src.recycle()

        val matrix = Matrix().apply { postRotate(angle) }
        val rotated =
            Bitmap.createBitmap(upscaled, 0, 0, upscaled.width, upscaled.height, matrix, true)
        upscaled.recycle()

        val left = ((rotated.width - iconSize) / 2f).toInt().coerceAtLeast(0)
        val top = ((rotated.height - iconSize) / 2f).toInt().coerceAtLeast(0)
        return if (rotated.width >= iconSize && rotated.height >= iconSize) {
            Bitmap.createBitmap(rotated, left, top, iconSize, iconSize).also {
                if (it !== rotated) rotated.recycle()
            }
        } else {
            val scaled = rotated.scale(iconSize, iconSize)
            if (scaled !== rotated) rotated.recycle()
            scaled
        }
    }

    private fun applyMask(src: Bitmap, maskBitmap: Bitmap, iconSize: Int): Bitmap {
        val result = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val scaledMask = if (maskBitmap.width != iconSize || maskBitmap.height != iconSize) {
            maskBitmap.scale(iconSize, iconSize)
        } else {
            maskBitmap
        }
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        canvas.drawBitmap(scaledMask, 0f, 0f, maskPaint)
        return result
    }


    fun mergeDualLayerBackground(
        lowerBg: Bitmap,
        upperBg: Bitmap,
        iconSize: Int,
        lowerAlpha: Int = DualLayerDefaults.LOWER_ALPHA_MAX
    ): Bitmap {
        val result = createBitmap(iconSize, iconSize)
        val canvas = Canvas(result)

        // 下层：铺满画布，应用图层透明度
        val lowerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = lowerAlpha.coerceIn(0, DualLayerDefaults.LOWER_ALPHA_MAX)
            isFilterBitmap = true
        }
        canvas.drawBitmap(lowerBg, 0f, 0f, lowerPaint)

        // 上层：居中绘制，不应用透明度
        val upperLeft = ((iconSize - upperBg.width) / 2f)
        val upperTop = ((iconSize - upperBg.height) / 2f)
        val upperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        canvas.drawBitmap(upperBg, upperLeft, upperTop, upperPaint)

        return result
    }
}
