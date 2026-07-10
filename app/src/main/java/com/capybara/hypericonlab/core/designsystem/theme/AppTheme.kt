package com.capybara.hypericonlab.core.designsystem.theme

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.capybara.hypericonlab.core.designsystem.theme.material.PaletteStyle
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeColorSpec
import com.capybara.hypericonlab.core.designsystem.theme.material.ThemeMode
import com.capybara.hypericonlab.core.designsystem.theme.material.animateAsState
import com.capybara.hypericonlab.core.designsystem.theme.material.dynamicColorScheme

private val LocalIsDark = staticCompositionLocalOf { false }
private val LocalPaletteStyle = staticCompositionLocalOf { PaletteStyle.Expressive }
private val LocalThemeColorSpec = staticCompositionLocalOf { ThemeColorSpec.SPEC_2025 }
private val LocalSeedColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
private val LocalUseDynamicColor = staticCompositionLocalOf { false }

val LocalAppColorScheme =
    staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

object AppTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current
    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current
    val seedColor: Color
        @Composable @ReadOnlyComposable get() = LocalSeedColor.current
    val paletteStyle: PaletteStyle
        @Composable @ReadOnlyComposable get() = LocalPaletteStyle.current
    val colorSpec: ThemeColorSpec
        @Composable @ReadOnlyComposable get() = LocalThemeColorSpec.current
    val themeMode: ThemeMode
        @Composable @ReadOnlyComposable get() = LocalThemeMode.current
    val useDynamicColor: Boolean
        @Composable @ReadOnlyComposable get() = LocalUseDynamicColor.current
}

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    paletteStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    useDynamicColor: Boolean,
    seedColor: Color,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val keyColor = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        colorResource(id = android.R.color.system_accent1_500)
    else seedColor

    val baseColorScheme = remember(keyColor, isDark, paletteStyle, colorSpec) {
        dynamicColorScheme(
            keyColor = keyColor,
            isDark = isDark,
            style = paletteStyle,
            colorSpec = colorSpec
        )
    }
    val animatedColorScheme = baseColorScheme.animateAsState()

    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalPaletteStyle provides paletteStyle,
        LocalSeedColor provides seedColor,
        LocalAppColorScheme provides animatedColorScheme,
        LocalThemeMode provides themeMode,
        LocalUseDynamicColor provides useDynamicColor,
        LocalThemeColorSpec provides colorSpec
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            NavigationBarContrastHandler()

        AppMaterialExpressiveTheme(
            darkTheme = isDark,
            colorScheme = animatedColorScheme
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppMaterialExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme,
    compatStatusBarColor: Boolean = true,
    content: @Composable () -> Unit
) {
    if (compatStatusBarColor) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as ComponentActivity).window
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    !darkTheme
            }
        }
    }

    val useGoogleSansFlex = isGoogleSansFlexEnabled()
    val baseTypography = Typography()
    val typography = if (useGoogleSansFlex) {
        baseTypography.run {
            copy(
                displayLarge = displayLarge.copy(fontFamily = GoogleSansFlexFontFamily),
                displayMedium = displayMedium.copy(fontFamily = GoogleSansFlexFontFamily),
                displaySmall = displaySmall.copy(fontFamily = GoogleSansFlexFontFamily),
                headlineLarge = headlineLarge.copy(fontFamily = GoogleSansFlexFontFamily),
                headlineMedium = headlineMedium.copy(fontFamily = GoogleSansFlexFontFamily),
                headlineSmall = headlineSmall.copy(fontFamily = GoogleSansFlexFontFamily),
                titleLarge = titleLarge.copy(fontFamily = GoogleSansFlexFontFamily),
                titleMedium = titleMedium.copy(fontFamily = GoogleSansFlexFontFamily),
                titleSmall = titleSmall.copy(fontFamily = GoogleSansFlexFontFamily),
                bodyLarge = bodyLarge.copy(fontFamily = GoogleSansFlexFontFamily),
                bodyMedium = bodyMedium.copy(fontFamily = GoogleSansFlexFontFamily),
                bodySmall = bodySmall.copy(fontFamily = GoogleSansFlexFontFamily),
                labelLarge = labelLarge.copy(fontFamily = GoogleSansFlexFontFamily),
                labelMedium = labelMedium.copy(fontFamily = GoogleSansFlexFontFamily),
                labelSmall = labelSmall.copy(fontFamily = GoogleSansFlexFontFamily)
            )
        }
    } else {
        Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            )
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun NavigationBarContrastHandler() {
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    DisposableEffect(configuration) {
        val window = (view.context as? ComponentActivity)?.window
        window?.isNavigationBarContrastEnforced = false
        onDispose {}
    }
}
