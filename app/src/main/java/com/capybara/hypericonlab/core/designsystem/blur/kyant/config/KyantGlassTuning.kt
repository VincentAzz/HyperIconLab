package com.capybara.hypericonlab.core.designsystem.blur.kyant.config

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

object KyantGlassTuningDefaults {
    const val BlurScale = 1f
    const val RefractionHeightScale = 1f
    const val RefractionAmountScale = 1f
    const val ChromaticAberration = 0f

    val ScaleRange = 0f..2f
    val ChromaticAberrationRange = 0f..1f
}

enum class KyantGlassTuningParameter {
    BLUR_SCALE,
    REFRACTION_HEIGHT_SCALE,
    REFRACTION_AMOUNT_SCALE,
    CHROMATIC_ABERRATION
}

@Immutable
data class KyantGlassTuning(
    val blurScale: Float = KyantGlassTuningDefaults.BlurScale,
    val refractionHeightScale: Float = KyantGlassTuningDefaults.RefractionHeightScale,
    val refractionAmountScale: Float = KyantGlassTuningDefaults.RefractionAmountScale,
    val chromaticAberration: Float = KyantGlassTuningDefaults.ChromaticAberration
) {
    fun normalized(): KyantGlassTuning = copy(
        blurScale = blurScale.coerceIn(KyantGlassTuningDefaults.ScaleRange),
        refractionHeightScale = refractionHeightScale.coerceIn(
            KyantGlassTuningDefaults.ScaleRange
        ),
        refractionAmountScale = refractionAmountScale.coerceIn(
            KyantGlassTuningDefaults.ScaleRange
        ),
        chromaticAberration = chromaticAberration.coerceIn(
            KyantGlassTuningDefaults.ChromaticAberrationRange
        )
    )
}

val LocalKyantGlassTuning = staticCompositionLocalOf { KyantGlassTuning() }
