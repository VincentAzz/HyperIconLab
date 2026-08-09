package com.capybara.hypericonlab.modules.settings.ui.page.settings.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.capybara.hypericonlab.R
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuningDefaults
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.KyantGlassTuningParameter
import com.capybara.hypericonlab.core.designsystem.component.SliderWidget
import com.capybara.hypericonlab.modules.icon.ui.page.custom.component.StyleChip
import kotlin.math.abs
import kotlin.math.roundToInt

private object KyantGlassTuningControlsConfig {
    val PresetHorizontalPadding = 12.dp
    val PresetBottomPadding = 12.dp
    val PresetSpacing = 8.dp
    val TransparentPreset = KyantGlassTuning(
        blurScale = 0.25f,
        refractionHeightScale = 1f,
        refractionAmountScale = 1f,
        chromaticAberration = 0f
    )
    val DefaultPreset = KyantGlassTuning(
        blurScale = 1f,
        refractionHeightScale = 1f,
        refractionAmountScale = 1f,
        chromaticAberration = 0f
    )
    val BlurredPreset = KyantGlassTuning(
        blurScale = 2f,
        refractionHeightScale = 1f,
        refractionAmountScale = 1f,
        chromaticAberration = 0f
    )
    const val PresetSelectionTolerance = 0.001f
    const val ScaleSteps = 39
    const val ChromaticAberrationSteps = 19
}

@Composable
fun KyantGlassTuningControls(
    tuning: KyantGlassTuning,
    onTuningChange: (KyantGlassTuningParameter, Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onPresetSelected: (KyantGlassTuning) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SliderWidget(
            title = stringResource(R.string.theme_settings_kyant_glass_blur_scale),
            value = tuning.blurScale,
            onValueChange = {
                onTuningChange(KyantGlassTuningParameter.BLUR_SCALE, it)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = KyantGlassTuningDefaults.ScaleRange,
            steps = KyantGlassTuningControlsConfig.ScaleSteps,
            valueDisplay = tuning.blurScale.toPercentage(),
            shape = RectangleShape
        )
        SliderWidget(
            title = stringResource(R.string.theme_settings_kyant_glass_refraction_height_scale),
            value = tuning.refractionHeightScale,
            onValueChange = {
                onTuningChange(KyantGlassTuningParameter.REFRACTION_HEIGHT_SCALE, it)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = KyantGlassTuningDefaults.ScaleRange,
            steps = KyantGlassTuningControlsConfig.ScaleSteps,
            valueDisplay = tuning.refractionHeightScale.toPercentage(),
            shape = RectangleShape
        )
        SliderWidget(
            title = stringResource(R.string.theme_settings_kyant_glass_refraction_amount_scale),
            value = tuning.refractionAmountScale,
            onValueChange = {
                onTuningChange(KyantGlassTuningParameter.REFRACTION_AMOUNT_SCALE, it)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = KyantGlassTuningDefaults.ScaleRange,
            steps = KyantGlassTuningControlsConfig.ScaleSteps,
            valueDisplay = tuning.refractionAmountScale.toPercentage(),
            shape = RectangleShape
        )
        SliderWidget(
            title = stringResource(R.string.theme_settings_kyant_glass_chromatic_aberration),
            value = tuning.chromaticAberration,
            onValueChange = {
                onTuningChange(KyantGlassTuningParameter.CHROMATIC_ABERRATION, it)
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = KyantGlassTuningDefaults.ChromaticAberrationRange,
            steps = KyantGlassTuningControlsConfig.ChromaticAberrationSteps,
            valueDisplay = tuning.chromaticAberration.toPercentage(),
            shape = RectangleShape
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = KyantGlassTuningControlsConfig.PresetHorizontalPadding,
                    end = KyantGlassTuningControlsConfig.PresetHorizontalPadding,
                    bottom = KyantGlassTuningControlsConfig.PresetBottomPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(
                KyantGlassTuningControlsConfig.PresetSpacing
            )
        ) {
            listOf(
                KyantGlassPreset(
                    label = stringResource(R.string.theme_settings_kyant_glass_preset_transparent),
                    tuning = KyantGlassTuningControlsConfig.TransparentPreset
                ),
                KyantGlassPreset(
                    label = stringResource(R.string.theme_settings_kyant_glass_preset_default),
                    tuning = KyantGlassTuningControlsConfig.DefaultPreset
                ),
                KyantGlassPreset(
                    label = stringResource(R.string.theme_settings_kyant_glass_preset_blurred),
                    tuning = KyantGlassTuningControlsConfig.BlurredPreset
                )
            ).forEach { preset ->
                StyleChip(
                    label = preset.label,
                    selected = tuning.matchesPreset(preset.tuning),
                    onClick = { onPresetSelected(preset.tuning) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private data class KyantGlassPreset(
    val label: String,
    val tuning: KyantGlassTuning
)

private fun KyantGlassTuning.matchesPreset(preset: KyantGlassTuning): Boolean =
    blurScale.matchesPresetValue(preset.blurScale) &&
            refractionHeightScale.matchesPresetValue(preset.refractionHeightScale) &&
            refractionAmountScale.matchesPresetValue(preset.refractionAmountScale) &&
            chromaticAberration.matchesPresetValue(preset.chromaticAberration)

private fun Float.matchesPresetValue(preset: Float): Boolean =
    abs(this - preset) <= KyantGlassTuningControlsConfig.PresetSelectionTolerance

private fun Float.toPercentage(): String = "${(this * 100f).roundToInt()}%"
