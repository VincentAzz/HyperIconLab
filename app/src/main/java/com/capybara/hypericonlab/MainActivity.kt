package com.capybara.hypericonlab

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.capybara.hypericonlab.core.designsystem.blur.kyant.config.LocalKyantGlassTuning
import com.capybara.hypericonlab.core.designsystem.component.AppleStyleControls
import com.capybara.hypericonlab.core.designsystem.component.LocalAppleStyleControls
import com.capybara.hypericonlab.core.designsystem.component.LocalUseAppleStyleCard
import com.capybara.hypericonlab.core.designsystem.navigation.AppRoot
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_INSTALL_APK_URI
import com.capybara.hypericonlab.core.designsystem.navigation.EXTRA_TAB_INDEX
import com.capybara.hypericonlab.core.designsystem.navigation.LocalPendingTab
import com.capybara.hypericonlab.core.designsystem.theme.AppTheme
import com.capybara.hypericonlab.core.designsystem.theme.CornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.LocalPreferredCardCornerRadius
import com.capybara.hypericonlab.core.designsystem.theme.LocalSmootherRoundedCornersEnabled
import com.capybara.hypericonlab.core.designsystem.theme.LocalUseGoogleSansFlex
import com.capybara.hypericonlab.core.designsystem.util.LocalWindowLayoutInfo
import com.capybara.hypericonlab.core.designsystem.util.rememberWindowLayoutInfo
import com.capybara.hypericonlab.modules.build.domain.packaging.ApkInstallFacade
import com.capybara.hypericonlab.modules.build.domain.usecase.BuildTaskManager
import com.capybara.hypericonlab.modules.icon.domain.repository.InitializationStateRepository
import com.capybara.hypericonlab.modules.icon.domain.usecase.InitializationCoordinator
import com.capybara.hypericonlab.modules.settings.domain.model.ThemeState
import com.capybara.hypericonlab.modules.settings.domain.provider.ThemeStateProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {
    private val themeStateProvider by inject<ThemeStateProvider>()
    private val apkInstaller by inject<ApkInstallFacade>()
    private val buildTaskManager by inject<BuildTaskManager>()
    private val initializationCoordinator by inject<InitializationCoordinator>()
    private val initializationStateRepository by inject<InitializationStateRepository>()
    private var pendingTab by mutableStateOf<Int?>(null)
    private var pendingInstallUri: Uri? = null
    private var unknownSourcesSettingsOpened = false
    private var initializationStartupChecked = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        initializationCoordinator.startInitialization()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()

        var isThemeLoaded = false
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }

        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        observeInstallRequests()

        setContent {
            val uiState by themeStateProvider.themeStateFlow.collectAsStateWithLifecycle(
                initialValue = ThemeState()
            )
            isThemeLoaded = uiState.isLoaded

            if (!isThemeLoaded) return@setContent

            val layoutInfo = rememberWindowLayoutInfo()
            val preferredCardCornerRadius = if (uiState.useCustomCardCornerRadius) {
                uiState.cardCornerSize.cornerRadius
            } else {
                CornerRadius
            }

            CompositionLocalProvider(
                LocalWindowLayoutInfo provides layoutInfo,
                LocalSmootherRoundedCornersEnabled provides uiState.useSmootherRoundedCorners,
                LocalPreferredCardCornerRadius provides preferredCardCornerRadius,
                LocalUseAppleStyleCard provides uiState.useAppleStyleCard,
                LocalUseGoogleSansFlex provides uiState.useGoogleSansFlex,
                LocalKyantGlassTuning provides uiState.kyantGlassTuning,
                LocalAppleStyleControls provides AppleStyleControls(
                    useToggle = uiState.useAppleStyleToggle,
                    useSlider = uiState.useAppleStyleSlider
                ),
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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

    override fun onPostResume() {
        super.onPostResume()
        if (initializationStartupChecked) return
        initializationStartupChecked = true
        lifecycleScope.launch {
            startInitializationAfterPermissionCheck()
        }
    }

    private suspend fun startInitializationAfterPermissionCheck() {
        val persistedState = initializationStateRepository.state.first()
        val shouldRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !persistedState.isCompleted &&
                !isNotificationPermissionGranted() &&
                !getPreferences(MODE_PRIVATE).getBoolean(
                    StartupConfig.NOTIFICATION_PERMISSION_REQUESTED,
                    false
                )
        if (shouldRequestPermission) {
            getPreferences(MODE_PRIVATE).edit {
                putBoolean(StartupConfig.NOTIFICATION_PERMISSION_REQUESTED, true)
            }
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            initializationCoordinator.startInitialization()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isNotificationPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

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

    private fun handleNotificationIntent(intent: Intent?) {
        val tab = intent?.getIntExtra(EXTRA_TAB_INDEX, -1) ?: -1
        pendingTab = if (tab >= 0) tab else null
        pendingInstallUri = intent?.getStringExtra(EXTRA_INSTALL_APK_URI)
            ?.let(Uri::parse)
    }

    private fun observeInstallRequests() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                buildTaskManager.installRequests.collect { request ->
                    launchApkInstaller(request.artifactUri.toUri())
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

    private object StartupConfig {
        const val NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
