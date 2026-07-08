package com.capybara.hypericonlab.core.designsystem.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.ui.page.settings.SettingsSharedViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(uiState: ThemeState) {
    val sharedViewModel: SettingsSharedViewModel = koinViewModel()
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        MainScreen(uiState, sharedViewModel)
    }
}
