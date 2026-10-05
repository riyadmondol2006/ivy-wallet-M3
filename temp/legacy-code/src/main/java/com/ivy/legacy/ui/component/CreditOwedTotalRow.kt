package com.ivy.legacy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.CreditOwedTotal
import com.ivy.legacy.utils.format
import com.ivy.ui.R

/**
 * What a card owes in total, in its main currency. Shows the bank rate behind the conversion, or
 * which currency is left out when the card has no rate yet.
 */
@Composable
fun CreditOwedTotalRow(total: CreditOwedTotal, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.credit_total_owed),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val prefix = if (total.estimated) "≈ " else ""
                Text(
                    prefix + total.amount.format(total.currency) + " " + total.currency,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (total.amount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            total.rates.forEach { (currency, rate) ->
                Text(
                    stringResource(
                        R.string.credit_total_owed_rate,
                        currency,
                        rate.format(total.currency),
                        total.currency,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (total.missingRateCurrencies.isNotEmpty()) {
                Text(
                    stringResource(R.string.credit_total_owed_missing_rate, total.missingRateCurrencies.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
