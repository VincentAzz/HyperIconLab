package com.capybara.hypericonlab.iconpack.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.capybara.hypericonlab.iconpack.ui.symbol.search
import com.capybara.hypericonlab.iconpack.ui.theme.AppMaterialSymbols
import com.capybara.hypericonlab.iconpack.ui.theme.rememberKyantRoundedRectangleShape
import com.capybara.hypericonlab.iconpack.ui.theme.CornerRadius as AppCornerRadius

@Composable
fun PreviewSearchField(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val fieldShape = rememberKyantRoundedRectangleShape(AppCornerRadius)

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SearchUiConfig.FIELD_HORIZONTAL_PADDING,
                top = SearchUiConfig.FIELD_TOP_PADDING,
                end = SearchUiConfig.FIELD_HORIZONTAL_PADDING,
                bottom = SearchUiConfig.FIELD_BOTTOM_PADDING
            )
            .focusRequester(focusRequester),
        label = { Text("搜索名称、包名或 Drawable") },
        leadingIcon = {
            Icon(
                imageVector = AppMaterialSymbols.search,
                contentDescription = null
            )
        },
        singleLine = true,
        shape = fieldShape
    )
}
