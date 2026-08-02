package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.capybara.hypericonlab.iconpack.R

@Composable
fun PreviewTopAppBar(
    visibleEntryCount: Int,
    totalEntryCount: Int,
    isSearchActive: Boolean
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "HyperIconLab 图标包",
                    style = MaterialTheme.typography.titleLarge
                )
                val iconSetName = stringResource(R.string.icon_set_name)
                val countText = if (isSearchActive && visibleEntryCount != totalEntryCount) {
                    stringResource(
                        R.string.filtered_icon_count_format,
                        visibleEntryCount,
                        totalEntryCount
                    )
                } else {
                    stringResource(R.string.icon_count_format, totalEntryCount)
                }
                Text(
                    text = "$countText ($iconSetName)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
