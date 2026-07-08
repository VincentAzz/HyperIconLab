package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt

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
}