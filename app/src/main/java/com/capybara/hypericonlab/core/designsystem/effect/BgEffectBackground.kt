// miuix/example/shared/src/commonMain/kotlin/component/effect/BgEffectBackground.kt

package com.capybara.hypericonlab.core.designsystem.effect

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BgEffectBackground(
    isDarkTheme: Boolean,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shaderSupported = remember { isRuntimeShaderSupported() }
    Box(modifier = modifier) {
        if (shaderSupported) {
            val painter = remember { BgEffectPainter() }
            val preset = remember(isDarkTheme) { BgEffectConfig.get(isDarkTheme) }
            val colorStage = remember { Animatable(0f) }

            LaunchedEffect(preset) {
                val animatesColors = preset.colors1 !== preset.colors2 ||
                        preset.colors2 !== preset.colors3
                if (!animatesColors) return@LaunchedEffect

                var targetStage = floor(colorStage.value) + 1f
                while (isActive) {
                    delay((preset.colorInterpPeriod * 500).toLong().milliseconds)
                    colorStage.animateTo(
                        targetValue = targetStage,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f),
                    )
                    targetStage += 1f
                }
            }

            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .bgEffectDraw(
                        painter = painter,
                        preset = preset,
                        isDarkTheme = isDarkTheme,
                        surface = surfaceColor,
                        colorStage = { colorStage.value },
                    ),
            )
        } else {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .background(surfaceColor)
            )
        }
        content()
    }
}
