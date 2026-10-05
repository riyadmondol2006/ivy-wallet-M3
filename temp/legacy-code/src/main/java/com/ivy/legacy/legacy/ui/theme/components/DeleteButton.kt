package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun DeleteButton(
    modifier: Modifier = Modifier,
    hasShadow: Boolean = true,
    onClick: () -> Unit,
) {
    IvyCircleButton(
        modifier = modifier
            .size(48.dp)
            .testTag("delete_button"),
        backgroundPadding = 6.dp,
        icon = R.drawable.ic_delete,
        // Destructive action: error container tones from the active scheme, not a fixed red.
        backgroundGradient = Gradient.solid(MaterialTheme.colorScheme.errorContainer),
        enabled = true,
        hasShadow = hasShadow,
        tint = MaterialTheme.colorScheme.onErrorContainer,
        onClick = onClick
    )
}
