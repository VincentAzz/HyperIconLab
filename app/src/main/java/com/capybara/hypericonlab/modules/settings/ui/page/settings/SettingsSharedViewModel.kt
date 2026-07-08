package com.capybara.hypericonlab.modules.settings.ui.page.settings

import androidx.lifecycle.ViewModel
import com.capybara.hypericonlab.modules.settings.domain.model.SettingsSharedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsSharedViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsSharedState())
    val state: StateFlow<SettingsSharedState> = _state.asStateFlow()

    fun updateLastMainPageIndex(index: Int) {
        _state.update { it.copy(lastMainPageIndex = index) }
    }
}
