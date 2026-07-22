package com.capybara.hypericonlab.core.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.graphics.createBitmap

/**
 * 内阴影处理器。
 *
 * 阴影来源于 assets/shadow_baking/ 下烘焙好的 PNG（四周内阴影，中心透明）。
 * 强度采用多层叠加方式：同一阴影图层重复绘制 N 次，alpha 自然累加获得弱/中/强效果。
 * 叠加位置：上层背景与前景图标之间。
 *
 * 为提升管线效率，采用"预合并"策略：在管线入口将多层阴影合并为单张阴影层 bitmap，
 * 管线内每个图标只需绘制一次该阴影层，避免重复叠加。
 */
object InnerShadowProcessor {

    // 强度层数上下限
    private const val MIN_LAYERS = 1
    private const val MAX_LAYERS = 3

    fun mergeShadowLayers(
        shadowBitmap: Bitmap,
        layers: Int
    ): Bitmap {
        val result = createBitmap(shadowBitmap.width, shadowBitmap.height)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
        val effectiveLayers = layers.coerceIn(MIN_LAYERS, MAX_LAYERS)
        repeat(effectiveLayers) {
            canvas.drawBitmap(shadowBitmap, 0f, 0f, paint)
        }
        return result
    }
}
