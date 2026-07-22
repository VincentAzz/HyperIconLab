package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

// 图标图层合成器
object LayerMerger {

    /**
     * 合成背景 + 图标两层（无内阴影）。
     */
    fun merge(
        background: Bitmap,
        icon: Bitmap,
        subtractIcon: Boolean = false,
        fgAlpha: Float = 1.0f
    ): Bitmap = merge(background, icon, innerShadow = null, subtractIcon, fgAlpha)

    /**
     * 合成背景 + 内阴影 + 图标三层。
     *
     * 图层顺序：背景（已在 result 中）→ 内阴影层（如有）→ 前景图标
     *
     * @param innerShadow 已叠加好的内阴影位图（与 background 同尺寸），null 表示无内阴影
     */
    fun merge(
        background: Bitmap,
        icon: Bitmap,
        innerShadow: Bitmap?,
        subtractIcon: Boolean = false,
        fgAlpha: Float = 1.0f
    ): Bitmap {
        val result = background.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        if (innerShadow != null) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            canvas.drawBitmap(innerShadow, 0f, 0f, shadowPaint)
        }

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