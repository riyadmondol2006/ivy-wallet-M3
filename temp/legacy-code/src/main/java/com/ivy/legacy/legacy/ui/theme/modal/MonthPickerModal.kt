package com.ivy.wallet.ui.theme.modal

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.Month.Companion.monthsList
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.onScreenStart
import com.ivy.ui.R
import java.time.LocalDate
import java.util.UUID

@SuppressLint("ComposeModifierMissing")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.MonthPickerModal(
    initialDate: LocalDate,
    visible: Boolean,
    dismiss: () -> Unit,
    onMonthSelected: (Int) -> Unit,
    id: UUID = UUID.randomUUID(),
) {
    var selectedMonth by remember {
        mutableStateOf(initialDate.monthValue)
    }

    IvyModal(
        id = id,
        visible = visible,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSave {
                onMonthSelected(selectedMonth)
                dismiss()
            }
        }
    ) {
        val view = LocalView.current
        onScreenStart {
            hideKeyboard(view)
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = stringResource(R.string.choose_month)
        )

        Spacer(Modifier.height(24.dp))

        MonthPicker(
            selectedMonth = selectedMonth,
            onMonthSelected = {
                selectedMonth = it
            }
        )

        Spacer(Modifier.height(56.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("ParameterNaming")
private fun MonthPicker(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit
) {
    val months = monthsList()

    FlowRow(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        months.forEach { month ->
            MonthButton(
                month = month,
                selected = month.monthValue == selectedMonth
            ) {
                onMonthSelected(month.monthValue)
            }
        }
    }
}

@Composable
private fun MonthButton(
    month: Month,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(month.name) }
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        MonthPickerModal(
            initialDate = dateNowUTC(),
            visible = true,
            dismiss = {},
            onMonthSelected = {}
        )
    }
}
