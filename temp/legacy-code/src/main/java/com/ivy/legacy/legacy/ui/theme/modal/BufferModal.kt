package com.ivy.wallet.ui.theme.modal

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.util.UUID
import kotlin.math.abs

private val SheetInset = 24.dp
private val CardInset = 16.dp
private val ProgressHeight = 8.dp

@Deprecated("Old design system. Use `:ivy-design` and Material3")
data class BufferModalData(
    val balance: Double,
    val buffer: Double,
    val currency: String,
    val id: UUID = UUID.randomUUID()
)

/**
 * Savings goal ("buffer") editor: explains the goal, previews what is left to spend with a
 * progress bar, and opens the amount keypad from a tappable goal field.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("ParameterNaming")
@Composable
fun BoxWithConstraintsScope.BufferModal(
    modal: BufferModalData?,
    dismiss: () -> Unit,
    onBufferChanged: (Double) -> Unit
) {
    var newBufferAmount by remember(modal) {
        mutableStateOf(modal?.buffer ?: 0.0)
    }

    var amountModalVisible by remember { mutableStateOf(false) }
    val currency = modal?.currency ?: ""

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        dismiss = dismiss,
        PrimaryAction = {
            ModalSave {
                onBufferChanged(newBufferAmount)
                dismiss()
            }
        }
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            modifier = Modifier.padding(horizontal = SheetInset),
            text = stringResource(R.string.savings_goal),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = SheetInset),
            text = stringResource(R.string.savings_goal_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        GoalAmountField(
            amount = newBufferAmount,
            currency = currency,
            onClick = { amountModalVisible = true },
        )

        Spacer(Modifier.height(12.dp))

        LeftToSpendCard(
            balance = modal?.balance ?: 0.0,
            buffer = newBufferAmount,
            currency = currency,
        )

        Spacer(Modifier.height(24.dp))
    }

    val amountModalId = remember(modal, newBufferAmount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currency,
        initialAmount = newBufferAmount,
        dismiss = { amountModalVisible = false }
    ) {
        newBufferAmount = it
    }
}

/** The goal amount as a tappable field; tapping opens the amount keypad. */
@Composable
private fun GoalAmountField(
    amount: Double,
    currency: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardInset)
            .testTag("amount_balance"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.savings_goal_amount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                BalanceRow(
                    currency = currency,
                    balance = amount,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    spacerCurrency = 8.dp,
                    balanceFontSize = 32.sp,
                    currencyFontSize = 22.sp,
                    currencyUpfront = false,
                )
            }

            Spacer(Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.edit_savings_goal),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Live preview of the balance left after the goal, with how much of the balance it reserves. */
@Composable
private fun LeftToSpendCard(
    balance: Double,
    buffer: Double,
    currency: String,
) {
    val leftToSpend = balance - buffer
    val exceeded = balance < buffer
    val fraction = if (balance > 0.0) (buffer / balance).coerceIn(0.0, 1.0).toFloat() else 1f
    val accent = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardInset),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        if (exceeded) R.string.buffer_exceeded_by else R.string.left_to_spend
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (exceeded) accent else MaterialTheme.colorScheme.onSurface,
                )
                AmountCurrencyB1(
                    amount = abs(leftToSpend),
                    currency = currency,
                    amountFontWeight = FontWeight.Bold,
                    textColor = if (exceeded) accent else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProgressHeight)
                    .clip(MaterialTheme.shapes.small),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${stringResource(R.string.savings_goal)} ${buffer.format(currency)} / " +
                    balance.format(currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
