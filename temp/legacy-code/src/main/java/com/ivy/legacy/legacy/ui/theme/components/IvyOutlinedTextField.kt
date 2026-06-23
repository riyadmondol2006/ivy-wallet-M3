package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.isNotNullOrBlank

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming", "UnusedParameter")
@Composable
fun IvyOutlinedTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    hint: String?,
    hintColor: Color = UI.colors.gray,
    backgroundColor: Color = UI.colors.primary,
    emptyBorderColor: Color = UI.colors.gray,
    textColor: Color = UI.colors.pureInverse,
    cursorColor: Color = UI.colors.pureInverse,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    validateInput: (TextFieldValue) -> Boolean = { it.text.isNotNullOrBlank() },
    onValueChanged: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChanged,
        placeholder = if (hint.isNotNullOrBlank()) {
            { Text(text = hint!!) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = cursorColor,
            focusedBorderColor = backgroundColor,
            unfocusedBorderColor = emptyBorderColor,
            focusedPlaceholderColor = hintColor,
            unfocusedPlaceholderColor = hintColor
        )
    )
}

@Preview
@Composable
private fun PreviewOutlineTextField() {
    IvyWalletComponentPreview {
        IvyOutlinedTextField(
            modifier = Modifier.padding(horizontal = 24.dp),
            value = TextFieldValue(),
            hint = "Hint",
            onValueChanged = {}
        )
    }
}
