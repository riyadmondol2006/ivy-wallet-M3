package com.ivy.legacy.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.navigation.navigation
import com.ivy.ui.R

/**
 * Material 3 top bar for the account / category / loan detail screens: close on the left, a
 * tonal Edit button and an error-tonal delete icon button on the right.
 */
@SuppressLint("ComposeModifierMissing")
@Composable
fun ItemStatisticToolbar(
    onEdit: () -> Unit,
    showEditButton: Boolean = true,
    showDeleteButton: Boolean = true,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val nav = navigation()
        IconButton(
            modifier = Modifier.testTag("toolbar_close"),
            onClick = { nav.back() },
        ) {
            Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
        }

        Spacer(Modifier.weight(1f))

        if (showEditButton) {
            FilledTonalButton(
                onClick = onEdit,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.edit))
            }
        }

        if (showDeleteButton) {
            Spacer(Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.delete))
            }
        }

        Spacer(Modifier.width(8.dp))
    }
}
