package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.ivy.design.l0_system.UI
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.selectEndTextFieldValue

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun IvyBasicTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    textColor: Color = UI.colors.pureInverse,
    hint: String?,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        autoCorrect = true,
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
        capitalization = KeyboardCapitalization.Sentences
    ),
    keyboardActions: KeyboardActions? = null,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val view = LocalView.current
    OutlinedTextField(
        modifier = modifier.testTag("base_input"),
        value = value,
        onValueChange = onValueChanged,
        placeholder = if (hint.isNotNullOrBlank()) {
            { Text(text = hint!!) }
        } else {
            null
        },
        singleLine = false,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions ?: KeyboardActions(
            onDone = {
                hideKeyboard(view)
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = UI.colors.pureInverse
        )
    )
}

@Preview
@Composable
private fun Preview_Hint() {
    IvyWalletComponentPreview {
        IvyBasicTextField(
            value = selectEndTextFieldValue(""),
            hint = "Search transactions",
            onValueChanged = {}
        )
    }
}

@Preview
@Composable
private fun Preview_Filled() {
    IvyWalletComponentPreview {
        IvyBasicTextField(
            value = selectEndTextFieldValue("sfds"),
            hint = "Okay",
            onValueChanged = {}
        )
    }
}
