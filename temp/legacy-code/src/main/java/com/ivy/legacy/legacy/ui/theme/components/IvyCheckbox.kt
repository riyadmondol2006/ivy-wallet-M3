package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.rememberInteractionSource

/**
 * Native Material 3 checkbox. M3 [Checkbox] provides the smooth-fill box + animated check tick
 * out of the box and pulls its colors from the active (dynamic) [MaterialTheme.colorScheme].
 */
@Composable
fun IvyCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit
) {
    Checkbox(
        modifier = modifier,
        checked = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
fun IvyCheckboxWithText(
    modifier: Modifier = Modifier,
    text: String,
    checked: Boolean,
    onCheckedChange: (checked: Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .clickableNoIndication(rememberInteractionSource()) {
                onCheckedChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview
@Composable
private fun PreviewIvyCheckboxWithText() {
    IvyWalletComponentPreview {
        IvyCheckboxWithText(
            text = "Default category",
            checked = false,
        ) {
        }
    }
}
