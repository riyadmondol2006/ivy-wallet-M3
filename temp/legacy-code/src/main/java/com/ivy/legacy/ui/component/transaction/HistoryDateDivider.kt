package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvySpacing
import com.ivy.design.system.income
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.dateNowLocal
import com.ivy.legacy.utils.dateNowUTC
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.formatLocal
import com.ivy.ui.R
import java.time.LocalDate

/** Date header above a day's transactions with that day's net cashflow on the trailing side. */
@Composable
fun HistoryDateDivider(
    date: LocalDate,
    spacerTop: Dp,
    baseCurrency: String,
    income: Double,
    expenses: Double
) {
    Spacer(Modifier.height(spacerTop))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = IvySpacing.screenGutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val today = dateNowLocal()

        Column {
            Text(
                text = date.formatLocal(
                    if (today.year == date.year) "MMMM dd" else "MMM dd, yyyy"
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = when (date) {
                    today -> {
                        stringResource(R.string.today)
                    }
                    today.minusDays(1) -> {
                        stringResource(R.string.yesterday)
                    }
                    today.plusDays(1) -> {
                        stringResource(R.string.tomorrow)
                    }
                    else -> {
                        date.formatLocal("EEEE")
                    }
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        val cashflow = income - expenses
        Text(
            text = "${cashflow.format(baseCurrency)} $baseCurrency",
            style = MaterialTheme.typography.bodyMedium,
            color = if (cashflow > 0) {
                MaterialTheme.colorScheme.income
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    Spacer(Modifier.height(4.dp))
}

@Preview
@Composable
private fun Preview_Today() {
    IvyWalletComponentPreview {
        HistoryDateDivider(
            date = dateNowUTC(),
            spacerTop = 32.dp,
            baseCurrency = "BGN",
            income = 13.50,
            expenses = 256.13
        )
    }
}

@Preview
@Composable
private fun Preview_Yesterday() {
    IvyWalletComponentPreview {
        HistoryDateDivider(
            date = dateNowUTC().minusDays(1),
            spacerTop = 32.dp,
            baseCurrency = "BGN",
            income = 13.50,
            expenses = 256.13
        )
    }
}

@Preview
@Composable
private fun Preview_OneYear_Ago() {
    IvyWalletComponentPreview {
        HistoryDateDivider(
            date = dateNowUTC().minusYears(1),
            spacerTop = 32.dp,
            baseCurrency = "BGN",
            income = 13.50,
            expenses = 256.13
        )
    }
}
