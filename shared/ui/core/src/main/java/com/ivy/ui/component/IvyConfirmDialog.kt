package com.ivy.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

/**
 * The one confirmation dialog used across the app: a Material 3 [AlertDialog] with a confirm and a
 * cancel action. Set [destructive] for irreversible actions so the confirm button uses the error
 * colour.
 */
@Composable
fun IvyConfirmDialog(
    visible: Boolean,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    icon: ImageVector? = null,
    dismissText: String = stringResource(R.string.cancel),
) {
    if (!visible) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
    )
}
