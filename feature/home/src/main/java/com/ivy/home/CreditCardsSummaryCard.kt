package com.ivy.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.ui.component.CreditCurrencySummary
import com.ivy.legacy.ui.component.CreditOwedTotalRow
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Every credit card with what it owes per currency and, for a dual-currency card, the total in its
 * main currency at the bank rate. With several cards an "All cards" footer adds them up.
 */
@Composable
fun CreditCardsSummaryCard(
    summary: CreditCardsSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.credit_cards), style = MaterialTheme.typography.titleMedium)
            summary.cards.forEachIndexed { index, card ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ItemIconSDefaultIcon(
                        iconName = card.icon,
                        defaultIcon = R.drawable.ic_custom_account_s,
                        tint = Color(card.color),
                    )
                    Text(card.name, style = MaterialTheme.typography.titleSmall)
                }
                CreditCurrencySummary(card.stats)
                card.total?.let { CreditOwedTotalRow(it) }
            }
            if (summary.cards.size > 1) {
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text(
                    stringResource(R.string.credit_all_cards),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CreditCurrencySummary(summary.currencies)
                summary.totals.forEach { CreditOwedTotalRow(it) }
            }
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
