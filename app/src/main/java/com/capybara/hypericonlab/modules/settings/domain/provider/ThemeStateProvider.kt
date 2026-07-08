package com.capybara.hypericonlab.modules.settings.domain.provider

import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import kotlinx.coroutines.flow.StateFlow

interface ThemeStateProvider {
    val themeStateFlow: StateFlow<ThemeState>
}
