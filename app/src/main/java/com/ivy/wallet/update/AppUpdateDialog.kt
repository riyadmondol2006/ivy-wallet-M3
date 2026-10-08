package com.ivy.wallet.update

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

/**
 * Offers a newer GitHub release. "Update" opens the download, "Skip this version" hides this
 * version for good, and tapping outside or back only hides it until the next app start.
 */
@Composable
@Suppress("FunctionNaming")
fun AppUpdateDialog(
    update: AppUpdate,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Rounded.SystemUpdate, contentDescription = null) },
        title = { Text(text = stringResource(R.string.app_update_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.app_update_desc,
                    update.version,
                    update.currentVersion,
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(text = stringResource(R.string.app_update_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(text = stringResource(R.string.app_update_skip))
            }
        },
    )
}
