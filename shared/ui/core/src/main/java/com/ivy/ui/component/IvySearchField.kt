package com.ivy.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.ivy.ui.R

/**
 * The one search field used across the app: an outlined M3 text field with a leading search icon,
 * a clear action while there is text, and the Search IME action.
 */
@Composable
fun IvySearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search),
    requestFocus: Boolean = false,
    onSearch: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(text = placeholder) },
        leadingIcon = {
            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
        },
        trailingIcon = {
            if (value.text.isNotEmpty()) {
                IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.clear),
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                keyboard?.hide()
                onSearch()
            },
        ),
    )

    if (requestFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

/** Convenience overload for callers that hold a plain [String]. */
@Composable
fun IvySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search),
    requestFocus: Boolean = false,
    onSearch: () -> Unit = {},
) {
    IvySearchField(
        value = TextFieldValue(value, selection = androidx.compose.ui.text.TextRange(value.length)),
        onValueChange = { onValueChange(it.text) },
        modifier = modifier,
        placeholder = placeholder,
        requestFocus = requestFocus,
        onSearch = onSearch,
    )
}
