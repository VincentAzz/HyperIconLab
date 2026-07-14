package com.capybara.hypericonlab.core.image

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

    /**
     * 静态图片背景：将图片缩放到 iconSize 直接作为背景。
     * 对应 Python 的 static_bg：resize 到 icon_size 后 paste，无 mask、无旋转。
     */
    fun createStaticImageBackground(
        context: Context,
        imageRef: String,
        iconSize: Int
    ): Bitmap? {
        val src = BgImageLoader.loadScaled(context, imageRef, iconSize) ?: return null
        // loadScaled 已缩放到 iconSize，返回可变副本供 LayerMerger 使用
        return src.copy(Bitmap.Config.ARGB_8888, true).also { if (src !== it) src.recycle() }
    }

    /**
     * 图片填充背景：缩放/裁切 + 可选随机旋转 + 可选 mask 裁切。
     * 对应 Python 的 img_bg：resize → 可选 rotate(expand=True)+居中裁切 → 可选 putalpha(mask) → paste。
     *
     * @param scaleMode "scale"=缩放填充(默认), "crop"=居中裁切
     */
    fun createImageFillingBackground(
        context: Context,
        imageRef: String,
        iconSize: Int,
        maskBitmap: Bitmap?,
        randomRotation: Boolean,
        scaleMode: String
    ): Bitmap? {
        // 1. 加载图片（缩放到 iconSize）
        val src = BgImageLoader.loadScaled(context, imageRef, iconSize) ?: return null

        // 2. 缩放或裁切
        var current = if (scaleMode == "crop") {
            centerCropToSquare(src, iconSize)
        } else {
            src // loadScaled 已是 iconSize×iconSize
        }

        // 3. 随机旋转（复刻 Python rotate(expand=True) + 居中裁切回 iconSize）
        if (randomRotation) {
            val angle = Random.nextFloat() * 360f
            val rotated = rotateExpandAndCenterCrop(current, angle, iconSize)
            if (rotated !== current) {
                if (current !== src) current.recycle()
                current = rotated
            }
        }

        // 4. mask 裁切（复刻 Python putalpha(mask)）
        val result = if (maskBitmap != null) {
            applyMask(current, maskBitmap, iconSize)
        } else {
            current.copy(Bitmap.Config.ARGB_8888, true)
        }

        if (current !== result && current !== src) current.recycle()
        if (src !== current) src.recycle()
        return result
    }

    /**
     * 居中裁切到正方形。当原图大于 targetSize 时从中心裁切。
     * 原图小于 targetSize 时不放大（保持原尺寸，后续由 Canvas 缩放）。
     */
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

    /**
     * 旋转图片（expand=True，即画布扩大以容纳旋转后的完整图片），
     * 然后居中裁切回 iconSize×iconSize。
     * 复刻 Python: layer_img.rotate(angle, expand=True) + center crop。
     */
    private fun rotateExpandAndCenterCrop(src: Bitmap, angle: Float, iconSize: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        // expand 后尺寸变大，居中裁切回 iconSize
        val left = ((rotated.width - iconSize) / 2f).toInt().coerceAtLeast(0)
        val top = ((rotated.height - iconSize) / 2f).toInt().coerceAtLeast(0)
        // 若旋转后小于 iconSize（理论上不会，因为 expand），则缩放
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

    /**
     * 用 mask 裁切图片。复刻 Python: layer_img.putalpha(mask)。
     * 使用 PorterDuff.Mode.DST_IN 保留 mask 非零区域的原图像素。
     */
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
}
