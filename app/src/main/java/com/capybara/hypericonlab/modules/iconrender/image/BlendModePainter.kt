package com.capybara.hypericonlab.modules.iconrender.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import com.capybara.hypericonlab.modules.iconrender.image.BlendModePainter.drawWithBlendMode

/** 装饰图层描述：bitmap + 混合模式 + 可选着色 + 整体透明度。 */
data class BlendLayer(
    val bitmap: Bitmap,
    val mode: PorterDuff.Mode,
    val tint: Int? = null,
    val alpha: Float = 1f
)

/**
 * 混合模式绘制工具。
 *
 * 作为图标管线中"装饰图层叠加"的低层基础设施工具，封装 PorterDuff 绘制与重新着色逻辑，
 * 供 LayerMerger 与后续 StyleLayerRenderer 复用，避免在多处重复实现。
 *
 * 着色策略：用 [PorterDuffColorFilter] 配合 SRC_IN 将 bitmap 颜色替换为 tint 色，
 * 保留原 alpha 蒙版，无需创建临时位图，性能优于先复制再染色。
 */
object BlendModePainter {

    /**
     * 在 [canvas] 上以指定混合模式绘制 [bitmap]。
     *
     * @param canvas 目标画布（其上已有底层内容作为 dst）
     * @param bitmap 待绘制的源 bitmap（应与 canvas 同尺寸）
     * @param mode PorterDuff 混合模式，常用：
     *   - [PorterDuff.Mode.SCREEN]：高光提亮且保留底色色相
     *   - [PorterDuff.Mode.MULTIPLY]：阴影压暗且保留底色色相
     *   - [PorterDuff.Mode.SRC_IN]：用源 alpha 蒙版填充目标色
     * @param tint 非 null 时，先用 SRC_IN 将 bitmap 重新着色为该颜色（保留 alpha 蒙版），
     *   再以 [mode] 与 canvas 合成；常用于把灰度资产染成主题色
     * @param alpha 该层整体透明度 0~1，1 表示完全不透明
     */
    fun drawWithBlendMode(
        canvas: Canvas,
        bitmap: Bitmap,
        mode: PorterDuff.Mode,
        tint: Int? = null,
        alpha: Float = 1f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            if (alpha < 1f) this.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
            // 着色：用 SRC_IN ColorFilter 把 bitmap 颜色替换为 tint，保留原 alpha 蒙版
            if (tint != null) colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
            xfermode = PorterDuffXfermode(mode)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    /** 便捷重载：直接接收 [BlendLayer] 描述，转调 [drawWithBlendMode]。 */
    fun drawBlendLayer(canvas: Canvas, layer: BlendLayer) {
        drawWithBlendMode(canvas, layer.bitmap, layer.mode, layer.tint, layer.alpha)
    }
}
