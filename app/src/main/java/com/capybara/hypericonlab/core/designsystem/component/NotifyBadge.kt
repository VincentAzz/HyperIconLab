package com.capybara.hypericonlab.core.designsystem.component

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object NotifyBadgeDefaults {
    val Size = 6.dp
    val BadgeStartOffset = 8.dp
}

@Composable
fun NotifyBadge(
    showBadge: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BadgedBox(
        modifier = modifier,
        badge = {
            if (showBadge) {
                Badge(
                    modifier = Modifier
                        .offset(x = NotifyBadgeDefaults.BadgeStartOffset)
                        .size(NotifyBadgeDefaults.Size),
                    containerColor = MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
        content()
    }
}
