// miuix/example/shared/src/commonMain/kotlin/component/effect/BgEffectModifier.kt

package com.capybara.hypericonlab.core.designsystem.effect

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun Modifier.bgEffectDraw(
    painter: BgEffectPainter,
    preset: BgEffectConfig.Config,
    isDarkTheme: Boolean,
    surface: Color,
    colorStage: () -> Float,
): Modifier = this then BgEffectElement(
    painter = painter,
    preset = preset,
    isDarkTheme = isDarkTheme,
    surface = surface,
    colorStage = colorStage,
)

private data class BgEffectElement(
    val painter: BgEffectPainter,
    val preset: BgEffectConfig.Config,
    val isDarkTheme: Boolean,
    val surface: Color,
    val colorStage: () -> Float,
) : ModifierNodeElement<BgEffectNode>() {

    override fun create(): BgEffectNode = BgEffectNode(
        painter = painter,
        preset = preset,
        isDarkTheme = isDarkTheme,
        surface = surface,
        colorStage = colorStage,
    )

    override fun update(node: BgEffectNode) {
        node.update(
            painter = painter,
            preset = preset,
            isDarkTheme = isDarkTheme,
            surface = surface,
            colorStage = colorStage,
        )
    }
}

private class BgEffectNode(
    private var painter: BgEffectPainter,
    private var preset: BgEffectConfig.Config,
    private var isDarkTheme: Boolean,
    private var surface: Color,
    private var colorStage: () -> Float,
) : Modifier.Node(),
    DrawModifierNode {

    private var animationJob: Job? = null
    private var animTime: Float = 0f
    private var startOffset: Float = 0f

    override fun onAttach() {
        startAnimation()
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null
    }

    fun update(
        painter: BgEffectPainter,
        preset: BgEffectConfig.Config,
        isDarkTheme: Boolean,
        surface: Color,
        colorStage: () -> Float,
    ) {
        this.painter = painter
        this.preset = preset
        this.isDarkTheme = isDarkTheme
        this.surface = surface
        this.colorStage = colorStage
        invalidateDraw()
    }

    private fun startAnimation() {
        animationJob?.cancel()
        startOffset = animTime
        animationJob = coroutineScope.launch {
            val minDeltaNanos = 1_000_000_000L / 60L
            val origin = withFrameNanos { it }
            var lastEmit = origin
            while (isActive) {
                val now = withFrameNanos { it }
                if (now - lastEmit < minDeltaNanos) continue
                lastEmit = now
                animTime = startOffset + (now - origin) / 1_000_000_000f
                invalidateDraw()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun ContentDrawScope.draw() {
        drawRect(surface)
        painter.updateResolution(size.width, size.height)
        painter.updateBoundIfNeeded(size.height, size.height, size.width)
        painter.updatePresetIfNeeded(isDarkTheme)
        painter.updateColors(preset, colorStage())
        painter.updateAnimTime(animTime)
        painter.updatePointsAnim(animTime, preset)
        drawRect(painter.brush)
        drawContent()
    }
}
