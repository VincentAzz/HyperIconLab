package com.capybara.hypericonlab.modules.icon.domain.render

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.capybara.hypericonlab.core.image.BackgroundGenerator

// 双层背景合成器
object DualLayerComposer {

    // 图片类下层背景强制不透明
    private const val FORCED_ALPHA = 255

    // 合成双层背景：lowerBg 为 null 时直接返回 upperComposite
    fun compose(
        upperComposite: Bitmap,
        lowerBg: Bitmap?,
        iconSize: Int,
        upperRenderSize: Int,
        bgStyle2: String,
        configuredAlpha: Int
    ): Bitmap {
        if (lowerBg == null) return upperComposite
        val upperScaled = upperComposite.scale(upperRenderSize, upperRenderSize)
        val effectiveLowerAlpha = resolveLowerAlpha(bgStyle2, configuredAlpha)
        return BackgroundGenerator.mergeDualLayerBackground(
            lowerBg = lowerBg,
            upperBg = upperScaled,
            iconSize = iconSize,
            lowerAlpha = effectiveLowerAlpha
        ).also {
            lowerBg.recycle()
            upperScaled.recycle()
            upperComposite.recycle()
        }
    }

    private fun resolveLowerAlpha(bgStyle2: String, configuredAlpha: Int): Int {
        return when (bgStyle2) {
            "img_static", "img_filling" -> FORCED_ALPHA
            else -> configuredAlpha
        }
    }
}
