package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

// 图标图层合成器
object LayerMerger {
    fun merge(
        background: Bitmap,
        icon: Bitmap,
        subtractIcon: Boolean = false,
        fgAlpha: Float = 1.0f
    ): Bitmap {
        val result = background.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (subtractIcon || fgAlpha <= 0.05f) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            canvas.drawBitmap(icon, 0f, 0f, paint)
        } else {
            canvas.drawBitmap(icon, 0f, 0f, paint)
        }

        return result
    }
}