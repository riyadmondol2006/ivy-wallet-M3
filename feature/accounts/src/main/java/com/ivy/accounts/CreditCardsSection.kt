package com.ivy.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.CreditCardData
import com.ivy.legacy.data.model.CreditDueStatus
import com.ivy.legacy.data.model.dueStatus
import com.ivy.legacy.ui.component.CreditCurrencySummary
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CreditCardsSection(
    cards: ImmutableList<CreditCardData>,
    today: LocalDate,
    onCardClick: (CreditCardData) -> Unit,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.credit_cards),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            TextButton(onClick = onAddCard) { Text(stringResource(R.string.add)) }
        }
        cards.forEach { card ->
            ElevatedCard(onClick = { onCardClick(card) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ItemIconSDefaultIcon(
                            iconName = card.primary.account.icon?.id,
                            defaultIcon = R.drawable.ic_custom_account_s,
                            tint = Color(card.primary.account.color.value)
                        )
                        Text(card.primary.account.name.value, style = MaterialTheme.typography.titleMedium)
                    }
                    CreditDueStatusText(card = card, today = today)
                    CreditCurrencySummary(card.stats().toImmutableList())
                    if (card.secondary != null) {
                        Text(
                            stringResource(
                                if (card.shared) R.string.credit_shared_note else R.string.credit_separate_note
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** "Payment due in N days", "Overdue since", or the next statement date; nothing without a cycle. */
@Composable
internal fun CreditDueStatusText(
    card: CreditCardData,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val status = remember(card, today) { card.dueStatus(today) } ?: return
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val (text, color) = when (status) {
        is CreditDueStatus.DueIn -> if (status.days == 0L) {
            stringResource(R.string.credit_due_today) to MaterialTheme.colorScheme.primary
        } else {
            stringResource(
                R.string.credit_due_in_days,
                status.days.toInt(),
                status.dueDate.format(formatter),
            ) to MaterialTheme.colorScheme.onSurfaceVariant
        }

        is CreditDueStatus.Overdue ->
            stringResource(R.string.credit_overdue_since, status.dueDate.format(formatter)) to
                MaterialTheme.colorScheme.error

        is CreditDueStatus.NextStatement ->
            stringResource(R.string.credit_next_statement, status.statementDate.format(formatter)) to
                MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}
