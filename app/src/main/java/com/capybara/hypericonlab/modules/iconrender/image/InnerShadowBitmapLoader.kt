package com.capybara.hypericonlab.modules.iconrender.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale

// 内阴影烘焙 PNG 加载器
object InnerShadowBitmapLoader {

    // 加载内阴影 PNG 并缩放到目标尺寸，不合并多层强度
    fun load(
        shapeName: String,
        styleName: String,
        targetSize: Int,
        context: Context
    ): Bitmap? {
        return try {
            val path =
                "${InnerShadowAssets.DIR}/${shapeName}_${styleName}${InnerShadowAssets.FILE_SUFFIX}"
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }?.let { raw ->
                if (raw.width != targetSize) {
                    val scaled = raw.scale(targetSize, targetSize)
                    raw.recycle()
                    scaled
                } else raw
            }
        } catch (_: Exception) {
            null
        }
    }

    // 加载+缩放+预合并多层强度为单张阴影层，管线内每个图标只绘制一次
    fun loadAndMerge(
        shapeName: String,
        styleName: String,
        targetSize: Int,
        intensityLayers: Int,
        context: Context
    ): Bitmap? {
        return load(shapeName, styleName, targetSize, context)?.let { scaled ->
            val merged = InnerShadowProcessor.mergeShadowLayers(scaled, intensityLayers)
            scaled.recycle()
            merged
        }
    }
}
