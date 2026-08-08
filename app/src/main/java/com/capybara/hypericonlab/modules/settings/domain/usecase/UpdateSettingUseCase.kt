package com.capybara.hypericonlab.modules.settings.domain.usecase

import com.capybara.hypericonlab.modules.settings.domain.repository.AppSettingsRepository
import com.capybara.hypericonlab.modules.settings.domain.repository.BooleanSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.FloatSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.IntSetting
import com.capybara.hypericonlab.modules.settings.domain.repository.StringSetting

class UpdateSettingUseCase(
    private val appSettingsRepo: AppSettingsRepository
) {
    suspend operator fun invoke(setting: BooleanSetting, value: Boolean) {
        appSettingsRepo.putBoolean(setting, value)
    }

    suspend operator fun invoke(setting: StringSetting, value: String) {
        appSettingsRepo.putString(setting, value)
    }

    suspend operator fun invoke(setting: IntSetting, value: Int) {
        appSettingsRepo.putInt(setting, value)
    }

    suspend operator fun invoke(setting: FloatSetting, value: Float) {
        appSettingsRepo.putFloat(setting, value)
    }
}
