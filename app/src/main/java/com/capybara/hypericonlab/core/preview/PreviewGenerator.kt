package com.capybara.hypericonlab.core.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap

// 预览生成器
object PreviewGenerator {
    fun generateMainPreview(
        context: Context,
        icons: List<Bitmap>,
        wallpaper: Bitmap? = null
    ): Bitmap {
        val targetWidth = 1080
        val targetHeight = 2400
        val result = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (wallpaper != null) {
            val bg = getCenterCroppedBitmap(wallpaper, targetWidth, targetHeight)
            canvas.drawBitmap(bg, null, Rect(0, 0, targetWidth, targetHeight), paint)
            if (bg != wallpaper) bg.recycle()
        } else {
            canvas.drawColor(Color.LTGRAY)
        }

        val rows = 8
        val cols = 4
        val iconSize = 160
        val hGap = (targetWidth - (cols * iconSize)) / (cols + 1)
        val vGap = (targetHeight - (rows * iconSize)) / (rows + 1)

        var count = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (count >= icons.size) break
                val left = hGap + c * (iconSize + hGap)
                val top = vGap + r * (iconSize + vGap)
                canvas.drawBitmap(
                    icons[count],
                    null,
                    Rect(left, top, left + iconSize, top + iconSize),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                count++
            }
        }

        return result
    }

    // 小预览图
    fun generateStorePreview(
        context: Context,
        icons: List<Bitmap>,
        background: Any? = null
    ): Bitmap {
        val width = 1080
        val height = 640
        val result = createBitmap(width, height)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (background) {
            is Bitmap -> {
                val bg = getCenterCroppedBitmap(background, width, height)
                canvas.drawBitmap(bg, null, Rect(0, 0, width, height), paint)
                if (bg != background) bg.recycle()
            }

            is Int -> {
                canvas.drawColor(background)
            }

            else -> {
                canvas.drawColor(Color.LTGRAY)
            }
        }

        val rows = 2
        val cols = 4
        val iconSize = 180
        val hGap = (width - (cols * iconSize)) / (cols + 1)
        val vGap = (height - (rows * iconSize)) / (rows + 1)

        var count = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (count >= icons.size) break
                val left = hGap + c * (iconSize + hGap)
                val top = vGap + r * (iconSize + vGap)
                canvas.drawBitmap(
                    icons[count],
                    null,
                    Rect(left, top, left + iconSize, top + iconSize),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                count++
            }
        }

        return result
    }


    fun getCenterCroppedBitmap(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = src.width
        val srcHeight = src.height

        val targetRatio = targetWidth.toFloat() / targetHeight
        val srcRatio = srcWidth.toFloat() / srcHeight

        val srcRect: Rect
        if (srcRatio > targetRatio) {
            val newWidth = (srcHeight * targetRatio).toInt()
            val left = (srcWidth - newWidth) / 2
            srcRect = Rect(left, 0, left + newWidth, srcHeight)
        } else {
            val newHeight = (srcWidth / targetRatio).toInt()
            val top = (srcHeight - newHeight) / 2
            srcRect = Rect(0, top, srcWidth, top + newHeight)
        }

        val dest = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(dest)
        canvas.drawBitmap(
            src,
            srcRect,
            Rect(0, 0, targetWidth, targetHeight),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return dest
    }
}
