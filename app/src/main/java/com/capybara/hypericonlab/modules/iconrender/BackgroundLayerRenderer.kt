package com.capybara.hypericonlab.modules.iconrender

import android.content.Context
import android.graphics.Bitmap
import com.capybara.hypericonlab.modules.iconrender.image.BackgroundGenerator
import com.capybara.hypericonlab.modules.iconrender.image.BlendLayer

// 背景图层渲染器：渲染上层复杂背景
object BackgroundLayerRenderer {
    data class UpperBackgroundResult(
        val background: Bitmap,
        val decorLayers: List<BlendLayer> = emptyList()
    )

    // 渲染上层复杂背景
    fun renderUpperBackground(
        context: Context,
        bgStyle: String,
        iconSize: Int,
        fallbackColorHex: String,
        maskBitmap: Bitmap?,
        imgStaticRef: String?,
        imgFillingRef: String?,
        fillingRandomRotation: Boolean,
        fillingScaleMode: String,
        retroShapeName: String? = null
    ): UpperBackgroundResult {
        return when (bgStyle) {
            "img_static" -> {
                val bg = if (imgStaticRef != null) {
                    BackgroundGenerator.createStaticImageBackground(context, imgStaticRef, iconSize)
                } else {
                    BackgroundGenerator.createBackground(iconSize, fallbackColorHex, maskBitmap)
                }
                UpperBackgroundResult(
                    bg ?: BackgroundGenerator.createBackground(
                        iconSize,
                        fallbackColorHex,
                        maskBitmap
                    )
                )
            }

            "img_filling" -> {
                val bg = if (imgFillingRef != null) {
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
                UpperBackgroundResult(
                    bg ?: BackgroundGenerator.createBackground(
                        iconSize,
                        fallbackColorHex,
                        maskBitmap
                    )
                )
            }

            "retro" -> {
                // Retro 复古：tint 底板 + RetroStyleRenderer 加载装饰图层
                val shape = retroShapeName ?: "ios27"
                val result = RetroStyleRenderer.render(
                    context = context,
                    shapeName = shape,
                    iconSize = iconSize,
                    tintColorHex = fallbackColorHex,
                    maskBitmap = maskBitmap
                )
                UpperBackgroundResult(
                    background = result.background,
                    decorLayers = result.decorLayers
                )
            }

            else -> UpperBackgroundResult(
                BackgroundGenerator.createBackground(iconSize, fallbackColorHex, maskBitmap)
            )
        }
    }
}
