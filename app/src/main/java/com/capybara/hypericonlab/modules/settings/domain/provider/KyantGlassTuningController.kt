package com.capybara.hypericonlab.modules.settings.domain.provider

import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.config.KyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.config.KyantGlassTuningParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KyantGlassTuningController {
    private val _preview = MutableStateFlow<KyantGlassTuning?>(null)
    val preview: StateFlow<KyantGlassTuning?> = _preview.asStateFlow()

    fun updatePreview(tuning: KyantGlassTuning) {
        _preview.value = tuning.normalized()
    }

    fun updatePreview(
        parameter: KyantGlassTuningParameter,
        value: Float,
        fallback: KyantGlassTuning
    ) {
        val current = currentOr(fallback)
        updatePreview(
            when (parameter) {
                KyantGlassTuningParameter.BLUR_SCALE -> current.copy(blurScale = value)
                KyantGlassTuningParameter.REFRACTION_HEIGHT_SCALE ->
                    current.copy(refractionHeightScale = value)

                KyantGlassTuningParameter.REFRACTION_AMOUNT_SCALE ->
                    current.copy(refractionAmountScale = value)

                KyantGlassTuningParameter.CHROMATIC_ABERRATION ->
                    current.copy(chromaticAberration = value)
            }
        )
    }

    fun currentOr(fallback: KyantGlassTuning): KyantGlassTuning =
        _preview.value ?: fallback
}
