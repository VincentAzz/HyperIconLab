package com.capybara.hypericonlab.modules.iconrender.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

// 图标图层合成器
object LayerMerger {

    /** 合成背景 + 图标两层（无内阴影）。 */
    fun merge(
        background: Bitmap,
        icon: Bitmap,
        subtractIcon: Boolean = false,
        fgAlpha: Float = 1.0f
    ): Bitmap = merge(
        background,
        icon,
        decorLayers = emptyList(),
        innerShadow = null,
        subtractIcon,
        fgAlpha
    )

    /**
     * 合成背景 + 内阴影 + 图标三层。
     * 图层顺序：背景 → 内阴影层（如有）→ 前景图标。
     *
     * @param innerShadow 已叠加好的内阴影位图（与 background 同尺寸），null 表示无内阴影
     */
    fun merge(
        background: Bitmap,
        icon: Bitmap,
        innerShadow: Bitmap?,
        subtractIcon: Boolean = false,
        fgAlpha: Float = 1.0f
    ): Bitmap =
        merge(background, icon, decorLayers = emptyList(), innerShadow, subtractIcon, fgAlpha)

    /**
     * 合成背景 + 装饰图层 + 内阴影 + 前景图标。
     *
     * 图层顺序（从下到上）：
     * 1. background（已 copy 到 result 中作为 dst）
     * 2. decorLayers（按列表顺序逐层叠加，每层用各自 [BlendLayer.mode] 混合，可选着色）
     * 3. innerShadow（如非 null，以 SRC_OVER 叠加）
     * 4. icon（前景图标，subtractIcon=true 时用 DST_OUT 镂空）
     *
     * decorLayers 用于支持 CRT 等复杂样式中的高光/阴影/扫描线等装饰层，
     * 在背景之上、内阴影之下绘制。空列表等价于无装饰层，行为与原三层合成一致。
     *
     * @param decorLayers 装饰图层列表，每层包含 bitmap、混合模式、可选着色与透明度
     * @param innerShadow 已叠加好的内阴影位图（与 background 同尺寸），null 表示无内阴影
     * @param subtractIcon true 时前景图标以 DST_OUT 镂空（用于 hollow 样式）
     * @param fgAlpha 前景图标整体透明度 0~1，<=0.05 时触发镂空
     */
    fun merge(
        background: Bitmap,
        icon: Bitmap,
        decorLayers: List<BlendLayer>,
        innerShadow: Bitmap?,
        subtractIcon: Boolean = false,
        fgAlpha: Float = 1.0f
    ): Bitmap {
        val result = background.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // 装饰图层：在背景之上、内阴影之下，按列表顺序叠加
        decorLayers.forEach { layer ->
            BlendModePainter.drawBlendLayer(canvas, layer)
        }

        // 内阴影层（SRC_OVER 叠加）
        if (innerShadow != null) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            canvas.drawBitmap(innerShadow, 0f, 0f, shadowPaint)
        }

        // 前景图标
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
