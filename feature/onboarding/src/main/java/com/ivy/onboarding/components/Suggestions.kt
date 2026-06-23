package com.ivy.onboarding.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.Purple
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.domain.deprecated.logic.model.CreateCategoryData
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.components.WrapContentRow

@Composable
fun Suggestions(
    suggestions: List<Any>,

    onAddSuggestion: (Any) -> Unit,
    onAddNew: () -> Unit
) {
    val items = suggestions.plus(AddNew())

    WrapContentRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        items = items,
        horizontalMarginBetweenItems = 8.dp,
        verticalMarginBetweenRows = 12.dp
    ) {
        when (it) {
            is CreateAccountData -> {
                Suggestion(name = it.name) {
                    onAddSuggestion(it)
                }
            }

            is CreateCategoryData -> {
                Suggestion(name = it.name) {
                    onAddSuggestion(it)
                }
            }

            is AddNew -> {
                AddNewButton {
                    onAddNew()
                }
            }
        }
    }
}

@Composable
private fun Suggestion(
    name: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = name) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

@Composable
private fun AddNewButton(
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = stringResource(R.string.add_new)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

private class AddNew

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        Suggestions(
            suggestions = listOf(
                Account("Cash", Green.toArgb()),
                Account("Bank", Red.toArgb()),
                Account("Revolut", Purple.toArgb())
            ),
            onAddSuggestion = { }
        ) {
        }
    }
}
