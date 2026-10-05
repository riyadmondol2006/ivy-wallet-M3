package com.ivy.legacy.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.ivy.design.system.IvySpacing
import com.ivy.ui.component.IvySearchField

/**
 * Legacy entry point kept for its call sites; renders the shared Material 3 [IvySearchField] so
 * every search box in the app looks and behaves the same.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("UNUSED_PARAMETER")
@Composable
fun SearchInput(
    searchQueryTextFieldValue: TextFieldValue,
    hint: String,
    focus: Boolean = true,
    showClearIcon: Boolean = true,
    onSetSearchQueryTextField: (TextFieldValue) -> Unit
) {
    IvySearchField(
        value = searchQueryTextFieldValue,
        onValueChange = onSetSearchQueryTextField,
        modifier = Modifier.padding(horizontal = IvySpacing.screenGutter),
        placeholder = hint,
        requestFocus = focus,
    )
}
