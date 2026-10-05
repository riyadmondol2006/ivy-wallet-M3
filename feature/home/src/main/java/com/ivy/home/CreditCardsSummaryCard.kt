package com.ivy.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.component.CreditCurrencySummary
import com.ivy.ui.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CreditCardsSummaryCard(
    summary: CreditCardsSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.credit_cards), style = MaterialTheme.typography.titleMedium)
            CreditCurrencySummary(summary.currencies)
            summary.nearestDueDate?.let { dueDate ->
                val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                Text(
                    stringResource(R.string.credit_nearest_due, dueDate.format(formatter)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (summary.currencies.any { it.estimated }) {
                Text(
                    stringResource(R.string.credit_shared_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
