package com.capybara.hypericonlab.modules.icon.domain.render

import android.content.Context
import android.graphics.Bitmap
import com.capybara.hypericonlab.core.image.BackgroundGenerator

// 背景图层渲染器
object BackgroundLayerRenderer {

    // 渲染上层背景，按 bgStyle 分支生成
    fun renderUpperBackground(
        context: Context,
        bgStyle: String,
        iconSize: Int,
        fallbackColorHex: String,
        maskBitmap: Bitmap?,
        imgStaticRef: String?,
        imgFillingRef: String?,
        fillingRandomRotation: Boolean,
        fillingScaleMode: String
    ): Bitmap {
        return when (bgStyle) {
            "img_static" -> {
                if (imgStaticRef != null) {
                    BackgroundGenerator.createStaticImageBackground(context, imgStaticRef, iconSize)
                } else {
                    BackgroundGenerator.createBackground(iconSize, fallbackColorHex, maskBitmap)
                }
            }

            "img_filling" -> {
                if (imgFillingRef != null) {
                    BackgroundGenerator.createImageFillingBackground(
                        context = context,
                        imageRef = imgFillingRef,
                        iconSize = iconSize,
                        maskBitmap = maskBitmap,
                        randomRotation = fillingRandomRotation,
                        scaleMode = fillingScaleMode
                    )
                } else {
                    BackgroundGenerator.createBackground(iconSize, fallbackColorHex, maskBitmap)
                }
            }

            else -> BackgroundGenerator.createBackground(iconSize, fallbackColorHex, maskBitmap)
        } ?: BackgroundGenerator.createBackground(iconSize, fallbackColorHex, maskBitmap)
    }
}
