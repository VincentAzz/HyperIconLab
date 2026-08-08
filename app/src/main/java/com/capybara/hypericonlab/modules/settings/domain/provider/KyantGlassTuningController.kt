package com.capybara.hypericonlab.modules.settings.domain.provider

import com.capybara.hypericonlab.core.designsystem.liquidglass.kyant.config.KyantGlassTuning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class KyantGlassTuningController {
    private val _preview = MutableStateFlow<KyantGlassTuning?>(null)
    val preview: StateFlow<KyantGlassTuning?> = _preview.asStateFlow()

    fun updatePreview(tuning: KyantGlassTuning) {
        _preview.value = tuning.normalized()
    }

    fun currentOr(fallback: KyantGlassTuning): KyantGlassTuning =
        _preview.value ?: fallback
}
