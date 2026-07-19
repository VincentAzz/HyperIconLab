package com.capybara.hypericonlab

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.capybara.hypericonlab.core.designsystem.navigation.AppRoot
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_TAB_INDEX
import com.capybara.hypericonlab.core.designsystem.navigation.LocalPendingTab
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.LocalSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.core.designsystem.theme.LocalUseGoogleSansFlex
import com.capybara.hypericonlab.core.designsystem.util.LocalWindowLayoutInfo
import com.capybara.hypericonlab.core.designsystem.util.rememberWindowLayoutInfo
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()

    // 通知 PendingIntent 携带的目标 tab 索引（null 表示无请求）
    // 由 onCreate / onNewIntent 更新，通过 LocalPendingTab 透传到 MainScreen 触发 animateToPage
    private var pendingTab by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        var isThemeLoaded = false
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)

        // 首次启动时检查 intent extras（从通知点击冷启动场景）
        handleNotificationIntent(intent)

        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(
                initialValue = ThemeState()
            )
            isThemeLoaded = uiState.isLoaded

            if (!isThemeLoaded) return@setContent

            val layoutInfo = rememberWindowLayoutInfo()

            CompositionLocalProvider(
                LocalWindowLayoutInfo provides layoutInfo,
                LocalSmootherRoundedCornersEnabled provides uiState.useSmootherRoundedCorners,
                LocalUseGoogleSansFlex provides uiState.useGoogleSansFlex,
                LocalPendingTab provides pendingTab
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

    // singleTop 模式下，已存在实例时点击通知会回调 onNewIntent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 更新 Activity 的 intent，保持 getIntent() 一致
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    // 从 intent extras 读取目标 tab 索引，更新 pendingTab 触发 MainScreen 切换
    private fun handleNotificationIntent(intent: Intent?) {
        val tab = intent?.getIntExtra(EXTRA_TAB_INDEX, -1) ?: -1
        pendingTab = if (tab >= 0) tab else null
    }
}
