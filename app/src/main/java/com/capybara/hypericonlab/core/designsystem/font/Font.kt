package com.capybara.hypericonlab.core.designsystem.font

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.capybara.hypericonlab.R

// Google Sans Flex Variable
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexFontFamily: FontFamily = FontFamily(
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(100f),
            FontVariation.slant(0f)
        )
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(100f),
            FontVariation.slant(0f)
        )
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(100f),
            FontVariation.slant(0f)
        )
    ),
    Font(
        R.font.googlesansflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(100f),
            FontVariation.slant(0f)
        )
    )
)

// Google Sans Code Variable
@OptIn(ExperimentalTextApi::class)
val GoogleSansCodeFontFamily: FontFamily = FontFamily(
    Font(
        R.font.googlesanscode_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400)
        )
    ),
    Font(
        R.font.googlesanscode_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700)
        )
    )
)

val LocalUseGoogleSansFlex = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isGoogleSansFlexEnabled(): Boolean = LocalUseGoogleSansFlex.current
