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
import androidx.compose.ui.text.font.FontWeight
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

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun IvyNumberTextField(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    value: TextFieldValue,
    hint: String?,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    textColor: Color = UI.colors.pureInverse,
    hintColor: Color = Color.Gray,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions? = null,
    keyboardActions: KeyboardActions? = null,
    onValueChanged: (TextFieldValue) -> Unit
) {
    val view = LocalView.current
    OutlinedTextField(
        modifier = modifier.then(textModifier).testTag("base_number_input"),
        value = value,
        onValueChange = onValueChanged,
        placeholder = if (hint.isNotNullOrBlank()) {
            { Text(text = hint!!, fontWeight = fontWeight) }
        } else {
            null
        },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions ?: KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number,
            autoCorrect = false
        ),
        keyboardActions = keyboardActions ?: KeyboardActions(
            onDone = {
                hideKeyboard(view)
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = UI.colors.pureInverse,
            focusedPlaceholderColor = hintColor,
            unfocusedPlaceholderColor = hintColor
        )
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IvyNumberTextField(
            value = TextFieldValue(),
            hint = "0"
        ) {
        }
    }
}
