package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private enum class StyleChipImplementation {
    KYANT,
    LEGACY,
}

private val CurrentStyleChipImplementation = StyleChipImplementation.KYANT

@Composable
fun StyleChip(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color? = null,
) {
    when (CurrentStyleChipImplementation) {
        StyleChipImplementation.KYANT -> StyleChipKyant(
            modifier = modifier,
            label = label,
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            selectedContainerColor = selectedContainerColor,
            unselectedContainerColor = unselectedContainerColor,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
        )

        StyleChipImplementation.LEGACY -> StyleChipLegacy(
            modifier = modifier,
            label = label,
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            selectedContainerColor = selectedContainerColor,
            unselectedContainerColor = unselectedContainerColor,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
        )
    }
}
