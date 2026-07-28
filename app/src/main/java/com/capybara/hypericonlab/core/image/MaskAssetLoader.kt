package com.capybara.hypericonlab.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

// Mask 资产加载器
object MaskAssetLoader {
    private const val MASK_DIR = "masks"
    private const val MASK_PREFIX = "mask_"
    private const val MASK_SUFFIX = "_512.png"

    private const val MASK_SUFFIX_COMMON = "_512_common.png"

    fun loadBitmap(context: Context, name: String): Bitmap? {
        val paths = listOf(
            "$MASK_DIR/$MASK_PREFIX$name$MASK_SUFFIX_COMMON",
            "$MASK_DIR/$MASK_PREFIX$name$MASK_SUFFIX"
        )
        for (path in paths) {
            try {
                context.assets.open(path).use { stream ->
                    return BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
            }
        }
        return null
    }
}
