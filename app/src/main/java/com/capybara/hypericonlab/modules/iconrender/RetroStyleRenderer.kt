package com.capybara.hypericonlab.modules.iconrender

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import com.capybara.hypericonlab.core.image.BackgroundGenerator
import com.capybara.hypericonlab.core.image.BlendLayer
import com.capybara.hypericonlab.core.image.BlendModePainter

object RetroAssets {
    const val DIR = "style_baked/retro"

    // <shapeName>_retro_<typeName>_<order>.png
    const val FILE_SUFFIX = ".png"
    const val TYPE_TOKEN = "retro"
}

// Retro 复古样式渲染器
// 图层顺序：背景 tint 层（带 mask）→ retro 装饰层（按文件名末尾编号排序）→ 前景图标
// 当前阶段装饰层不着色，直接以 SRC_OVER 叠加（PNG 为白色+透明度导出）
object RetroStyleRenderer {

    // Retro 背景渲染结果：背景 tint 层 + 装饰图层列表
    data class RetroResult(
        val background: Bitmap,
        val decorLayers: List<BlendLayer>
    ) {
        fun toFlattenedBitmap(): Bitmap {
            if (decorLayers.isEmpty()) return background
            val result = background.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(result)
            decorLayers.forEach { BlendModePainter.drawBlendLayer(canvas, it) }
            return result
        }
    }

    // 渲染 Retro 背景组合：生成 tint 底板并加载该形状对应的所有 retro 装饰层
    fun render(
        context: Context,
        shapeName: String,
        iconSize: Int,
        tintColorHex: String,
        maskBitmap: Bitmap?
    ): RetroResult {
        val background = BackgroundGenerator.createBackground(
            iconSize = iconSize,
            colorHex = tintColorHex,
            maskBitmap = maskBitmap
        )

        val decorLayers = loadRetroLayers(context, shapeName, iconSize)

        return RetroResult(background, decorLayers)
    }

    private fun loadRetroLayers(
        context: Context,
        shapeName: String,
        iconSize: Int
    ): List<BlendLayer> {
        val files = try {
            context.assets.list(RetroAssets.DIR) ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        // 解析 <shapeName>_retro_<typeName>_<order>.png
        return files
            .asSequence()
            .filter { it.endsWith(RetroAssets.FILE_SUFFIX) }
            .mapNotNull { filename ->
                val core = filename.removeSuffix(RetroAssets.FILE_SUFFIX)
                val parts = core.split("_")
                if (parts.size == 4 && parts[0] == shapeName && parts[1] == RetroAssets.TYPE_TOKEN) {
                    val order = parts[3].toIntOrNull() ?: return@mapNotNull null
                    order to filename
                } else null
            }
            .sortedBy { it.first }
            .mapNotNull { (_, filename) ->
                loadBitmapFromAssets(context, "${RetroAssets.DIR}/$filename")?.let { bitmap ->
                    // 当前阶段不着色，直接 SRC_OVER 叠加
                    BlendLayer(
                        bitmap = bitmap,
                        mode = PorterDuff.Mode.SRC_OVER,
                        tint = null,
                        alpha = 1f
                    )
                }
            }
            .toList()
    }

    private fun loadBitmapFromAssets(context: Context, path: String): Bitmap? {
        return try {
            context.assets.open(path).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}
