package com.ivy.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

/**
 * The one close button used across the app. Plain by default; [tonal] gives it a tonal container
 * for use on sheets and overlays where it must stand out from the content.
 */
@Composable
fun IvyCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
) {
    val icon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.close),
        )
    }
    if (tonal) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier, content = icon)
    } else {
        IconButton(onClick = onClick, modifier = modifier, content = icon)
    }
}
