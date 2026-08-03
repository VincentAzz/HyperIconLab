package com.capybara.hypericonlab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.capybara.hypericonlab.core.designsystem.navigation.AppRoot
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_INSTALL_APK_URI
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_TAB_INDEX
import com.capybara.hypericonlab.core.designsystem.navigation.LocalPendingTab
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.LocalSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.core.designsystem.theme.LocalUseGoogleSansFlex
import com.capybara.hypericonlab.core.designsystem.util.LocalWindowLayoutInfo
import com.capybara.hypericonlab.core.designsystem.util.rememberWindowLayoutInfo
import com.capybara.hypericonlab.modules.build.domain.packaging.ApkInstallFacade
import com.capybara.hypericonlab.modules.build.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()
    private val apkInstaller by inject<ApkInstallFacade>()
    private val buildTaskManager by inject<BuildTaskManager>()

    // 通知 PendingIntent 携带的目标 tab 索引（null 表示无请求）
    // 由 onCreate / onNewIntent 更新，通过 LocalPendingTab 透传到 MainScreen 触发 animateToPage
    private var pendingTab by mutableStateOf<Int?>(null)

    // 从未知来源设置页返回后待重试的 APK Uri
    private var pendingInstallUri: Uri? = null

    // 标记是否已经打开未知来源设置页，避免返回时重复拉起设置页
    private var unknownSourcesSettingsOpened = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        var isThemeLoaded = false
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)

        // 首次启动时检查 intent extras（从通知点击冷启动场景）
        handleNotificationIntent(intent)
        observeInstallRequests()

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
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            processPendingInstall()
        }
    }

    override fun onResume() {
        super.onResume()
        processPendingInstall()
    }

    private fun processPendingInstall() {
        pendingInstallUri?.let { uri ->
            if (apkInstaller.canInstallUnknownSources()) {
                pendingInstallUri = null
                unknownSourcesSettingsOpened = false
                launchApkInstaller(uri)
            } else if (!unknownSourcesSettingsOpened) {
                unknownSourcesSettingsOpened = true
                if (!apkInstaller.openUnknownSourcesSettings()) {
                    pendingInstallUri = null
                    unknownSourcesSettingsOpened = false
                }
            }
        }
    }

    // 从 intent extras 读取目标 tab 索引，更新 pendingTab 触发 MainScreen 切换
    private fun handleNotificationIntent(intent: Intent?) {
        val tab = intent?.getIntExtra(EXTRA_TAB_INDEX, -1) ?: -1
        pendingTab = if (tab >= 0) tab else null
        pendingInstallUri = intent?.getStringExtra(EXTRA_INSTALL_APK_URI)
            ?.let(Uri::parse)
    }

    // 仅在 Activity 处于前台时消费构建成功事件，避免后台直接启动 Activity。
    private fun observeInstallRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                buildTaskManager.installRequests.collect { request ->
                    launchApkInstaller(Uri.parse(request.artifactUri))
                }
            }
        }
    }

    private fun launchApkInstaller(uri: Uri) {
        when (apkInstaller.launchInstaller(uri)) {
            ApkInstallFacade.LaunchResult.Launched -> Unit
            ApkInstallFacade.LaunchResult.UnknownSourcesPermissionRequired -> {
                pendingInstallUri = uri
                if (!unknownSourcesSettingsOpened) {
                    unknownSourcesSettingsOpened = true
                    if (!apkInstaller.openUnknownSourcesSettings()) {
                        pendingInstallUri = null
                        unknownSourcesSettingsOpened = false
                    }
                }
            }

            is ApkInstallFacade.LaunchResult.Failed -> Unit
        }
    }
}
