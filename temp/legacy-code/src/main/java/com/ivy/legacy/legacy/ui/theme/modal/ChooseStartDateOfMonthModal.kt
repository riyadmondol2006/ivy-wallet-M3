package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.ChooseStartDateOfMonthModal(
    id: UUID = UUID.randomUUID(),
    visible: Boolean,
    selectedStartDateOfMonth: Int,

    dismiss: () -> Unit,
    onStartDateOfMonthSelected: (Int) -> Unit,
) {
    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = { }
    ) {
        Spacer(Modifier.height(32.dp))

        ModalTitle(text = stringResource(R.string.choose_start_date_of_month))

        Spacer(Modifier.height(32.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 1,
            toInclusive = 5
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 6,
            toInclusive = 10
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 11,
            toInclusive = 15
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 16,
            toInclusive = 20
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 21,
            toInclusive = 25
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 26,
            toInclusive = 30
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(16.dp))

        NumberRow(
            selectedNumber = selectedStartDateOfMonth,
            fromInclusive = 31,
            toInclusive = 31,
        ) {
            save(
                number = it,
                onStartDateOfMonthSelected = onStartDateOfMonthSelected,
                dismiss = dismiss
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun save(
    number: Int,

    onStartDateOfMonthSelected: (Int) -> Unit,
    dismiss: () -> Unit
) {
    onStartDateOfMonthSelected(number)
    dismiss()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.NumberRow(
    selectedNumber: Int,
    fromInclusive: Int,
    toInclusive: Int,
    onClick: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (number in fromInclusive..toInclusive) {
            NumberView(
                number = number,
                selected = number == selectedNumber
            ) {
                onClick(it)
            }
        }
    }
}

@Composable
private fun NumberView(
    number: Int,
    selected: Boolean,
    onClick: (Int) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onClick(number) },
        label = { Text(number.toString()) }
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        ChooseStartDateOfMonthModal(
            visible = true,
            selectedStartDateOfMonth = 1,
            dismiss = {}
        ) {
        }
    }
}
