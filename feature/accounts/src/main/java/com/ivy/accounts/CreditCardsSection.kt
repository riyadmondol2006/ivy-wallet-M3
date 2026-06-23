package com.ivy.accounts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMotion
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList

private fun AccountData.owed(): Double = -minOf(0.0, balance)
private fun AccountData.limit(): Double = account.creditLimit ?: 0.0
private fun AccountData.limitLeft(): Double = (limit() - owed()).coerceAtLeast(0.0)

/**
 * Credit-cards group shown at the top of the Accounts tab. Header carries the aggregated
 * "Limit left" + "To pay" duo across all cards; each card shows its own duo + progress.
 */
@Composable
fun CreditCardsSection(
    baseCurrency: String,
    cards: ImmutableList<AccountData>,
    onCardClick: (AccountData) -> Unit,
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalOwed = cards.sumOf { it.owed() }
    val totalLimit = cards.sumOf { it.limit() }
    val totalLimitLeft = (totalLimit - totalOwed).coerceAtLeast(0.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = IvyMotion.contentSizeSpring())
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.credit_cards),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAddCard) {
                Text(text = stringResource(R.string.add_credit_card))
            }
        }

        if (cards.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            // Aggregated duo across all cards
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(vertical = 16.dp),
            ) {
                DuoStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.limit_left),
                    value = totalLimitLeft.format(baseCurrency),
                    currency = baseCurrency,
                    valueColor = MaterialTheme.colorScheme.primary
                )
                DuoStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.amount_to_pay),
                    value = totalOwed.format(baseCurrency),
                    currency = baseCurrency,
                    valueColor = if (totalOwed > 0.0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        cards.forEach { card ->
            Spacer(Modifier.height(12.dp))
            CreditCardCard(
                card = card,
                onClick = { onCardClick(card) }
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DuoStat(
    label: String,
    value: String,
    currency: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$value $currency",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun CreditCardCard(
    card: AccountData,
    onClick: () -> Unit
) {
    val accentColor = card.account.color.value.toComposeColor()
    val limit = card.limit()
    val limitLeft = card.limitLeft()
    val owed = card.owed()
    val progress = if (limit > 0.0) (limitLeft / limit).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = IvyMotion.spatialSpring(),
        label = "credit-card-progress"
    )
    val currency = card.account.asset.code

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .animateContentSize(animationSpec = IvyMotion.contentSizeSpring())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center,
                ) {
                    ItemIconSDefaultIcon(
                        iconName = card.account.icon?.id,
                        defaultIcon = R.drawable.ic_custom_account_s,
                        tint = com.ivy.wallet.ui.theme.findContrastTextColor(accentColor)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = card.account.name.value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                DuoStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.amount_to_pay),
                    value = owed.format(currency),
                    currency = currency,
                    valueColor = if (owed > 0.0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                DuoStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.limit_left),
                    value = limitLeft.format(currency),
                    currency = currency,
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${stringResource(R.string.limit_label)}: ${limit.format(currency)} $currency",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Bottom sheet shown when a credit card is tapped: pay the owed amount from a real account
 * (creates a transfer), or just reset the card's tracking. Either way the limit is restored.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkPaidSheet(
    card: AccountData,
    payableAccounts: ImmutableList<AccountData>,
    onPayFromAccount: (AccountData) -> Unit,
    onReset: () -> Unit,
    onEdit: () -> Unit,
    onViewTransactions: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currency = card.account.asset.code
    val owed = card.owed()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = card.account.name.value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.amount_to_pay)}: ${owed.format(currency)} $currency",
                style = MaterialTheme.typography.bodyLarge,
                color = if (owed > 0.0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (owed > 0.0) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.pay_from_account),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                payableAccounts.forEach { acc ->
                    PayFromAccountRow(
                        account = acc,
                        onClick = { onPayFromAccount(acc) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewTransactions) {
                    Text(stringResource(R.string.view_transactions))
                }
                Row {
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.edit))
                    }
                    if (owed > 0.0) {
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = onReset) {
                            Text(stringResource(R.string.reset_card))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PayFromAccountRow(
    account: AccountData,
    onClick: () -> Unit
) {
    val accentColor = account.account.color.value.toComposeColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            ItemIconSDefaultIcon(
                iconName = account.account.icon?.id,
                defaultIcon = R.drawable.ic_custom_account_s,
                tint = com.ivy.wallet.ui.theme.findContrastTextColor(accentColor)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = account.account.name.value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${account.balance.format(account.account.asset.code)} ${account.account.asset.code}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
