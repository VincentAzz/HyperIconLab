package com.capybara.hypericonlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.navigation.AppRoot
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.util.LocalWindowLayoutInfo
import com.capybara.hypericonlab.core.designsystem.util.rememberWindowLayoutInfo
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled

class MainActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        var isThemeLoaded = false
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)

        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(
                initialValue = ThemeState()
            )
            isThemeLoaded = uiState.isLoaded

            if (!isThemeLoaded) return@setContent

            val layoutInfo = rememberWindowLayoutInfo()

            CompositionLocalProvider(
                LocalWindowLayoutInfo provides layoutInfo,
                LocalSquircleEnabled provides uiState.useMiuixSquircle
            ) {
                AppTheme(
                    themeMode = uiState.themeMode,
                    paletteStyle = uiState.paletteStyle,
                    colorSpec = uiState.colorSpec,
                    useDynamicColor = uiState.useDynamicColor,
                    seedColor = uiState.seedColor
                ) {
                    AppRoot(uiState)
                }
            }
        }
    }
}
