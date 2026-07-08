package com.capybara.hypericonlab.core.designsystem.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

@Composable
fun MainScreenBackHandler(
    mainPagerState: MainPagerState,
) {
    val isEnabled by remember {
        derivedStateOf {
            mainPagerState.selectedPage != 0
        }
    }

    BackHandler(enabled = isEnabled) {
        mainPagerState.animateToPage(0)
    }
}
