package com.capybara.hypericonlab.core.designsystem.liquidglass.kyant

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.DefaultCameraDistance
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.unit.Density

internal class InverseLayerScope : GraphicsLayerScope {

    override var size: Size = Size.Unspecified
    override var density: Float = 1f
    override var fontScale: Float = 1f

    override var scaleX: Float = 1f
    override var scaleY: Float = 1f
    override var alpha: Float = 0f
    override var translationX: Float = 0f
    override var translationY: Float = 0f
    override var shadowElevation: Float = 0f
    override var ambientShadowColor: Color = DefaultShadowColor
    override var spotShadowColor: Color = DefaultShadowColor
    override var rotationX: Float = 0f
    override var rotationY: Float = 0f
    override var rotationZ: Float = 0f
    override var cameraDistance: Float = DefaultCameraDistance
    override var transformOrigin: TransformOrigin = TransformOrigin.Center
    override var shape: Shape = RectangleShape
    override var clip: Boolean = false
    override var renderEffect: RenderEffect? = null
    override var blendMode: BlendMode = BlendMode.SrcOver
    override var colorFilter: ColorFilter? = null
    override var compositingStrategy: CompositingStrategy = CompositingStrategy.Auto

    private var matrix: Matrix? = null

    fun DrawTransform.inverseTransform(
        density: Density,
        layerBlock: GraphicsLayerScope.() -> Unit
    ) {
        this@InverseLayerScope.size = size
        this@InverseLayerScope.density = density.density
        fontScale = density.fontScale

        layerBlock()

        inverseTransformAtTopLeft(
            rotationZ = rotationZ,
            scaleX = scaleX,
            scaleY = scaleY
        )
    }

    fun reset() {
        size = Size.Unspecified
        density = 1f
        fontScale = 1f

        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        translationX = 0f
        translationY = 0f
        shadowElevation = 0f
        ambientShadowColor = DefaultShadowColor
        spotShadowColor = DefaultShadowColor
        rotationX = 0f
        rotationY = 0f
        rotationZ = 0f
        cameraDistance = DefaultCameraDistance
        transformOrigin = TransformOrigin.Center
        …9504 tokens truncated…{
            val canvas = drawContext.canvas
            canvas.save()
            canvas.clipOutline(outline, clipPath)
            canvas.drawOutline(outline, paint)
            canvas.restore()
        }
    }

    translate(-1f, -1f)
    {
        drawLayer(highlightLayer)
    }
}
}

override fun onAttach() {
    val graphicsContext = requireGraphicsContext()
    highlightLayer = graphicsContext.createGraphicsLayer()
}

override fun onDetach() {
    val graphicsContext = requireGraphicsContext()
    highlightLayer?.let { layer ->
        graphicsContext.releaseGraphicsLayer(layer)
        highlightLayer = null
    }
    clipPath = null
    runtimeShaderCache.clear()
    prevStyle = null
}

private fun DrawScope.configurePaint(highlight: Highlight) {
    paint.color = highlight.style.color
    paint.strokeWidth =
        ceil(highlight.width.toPx().fastCoerceAtMost(size.minDimension / 2f)) * 2f
    val blurRadius = highlight.blurRadius.toPx()
    paint.asFrameworkPaint().maskFilter =
        if (blurRadius > 0f) {
            BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        paint.shader = with(highlight.style) {
            createShader(
                shape = shapeProvider.shape,
                runtimeShaderCache = runtimeShaderCache
            )
        }
    }
}
}

