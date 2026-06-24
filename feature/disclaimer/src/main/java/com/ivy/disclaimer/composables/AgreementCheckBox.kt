package com.ivy.disclaimer.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ivy.disclaimer.CheckboxViewState

@Composable
fun AgreementCheckBox(
    viewState: CheckboxViewState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (viewState.checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        label = "agreementContainerColor",
    )
    val textColor by animateColorAsState(
        targetValue = if (viewState.checked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "agreementTextColor",
    )
    Surface(
        modifier = modifier
            .toggleable(
                value = viewState.checked,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            ),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = viewState.checked,
                onCheckedChange = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = viewState.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}
