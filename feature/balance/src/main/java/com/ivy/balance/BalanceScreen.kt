package com.ivy.balance

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.shortenAmount
import com.ivy.navigation.BalanceScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.ui.component.IvyCloseButton
import com.ivy.ui.component.chart.IvyLineChart
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.wallet.ui.theme.wallet.PeriodSelector
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val FAB_BUTTON_SIZE = 56.dp

@Composable
fun BoxWithConstraintsScope.BalanceScreen(screen: BalanceScreen) {
    val viewModel: BalanceViewModel = viewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun BoxWithConstraintsScope.UI(
    state: BalanceState,
    onEvent: (BalanceEvent) -> Unit = {}
) {
    var choosePeriodModal: ChoosePeriodModalData? by remember { mutableStateOf(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))

        PeriodSelector(
            period = state.period,
            onPreviousMonth = { onEvent(BalanceEvent.OnPreviousMonth) },
            onNextMonth = { onEvent(BalanceEvent.OnNextMonth) },
            onShowChoosePeriodModal = {
                choosePeriodModal = ChoosePeriodModalData(
                    period = state.period
                )
            }
        )

        Spacer(Modifier.height(32.dp))

        CurrentBalance(
            currency = state.baseCurrencyCode,
            currentBalance = state.currentBalance
        )

        Spacer(Modifier.height(24.dp))

        BalanceHistorySection(state = state, onEvent = onEvent)

        Spacer(Modifier.height(32.dp))

        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Spacer(Modifier.height(40.dp))

        BalanceAfterPlannedPayments(
            currency = state.baseCurrencyCode,
            currentBalance = state.currentBalance,
            plannedPaymentsAmount = state.plannedPaymentsAmount,
            balanceAfterPlannedPayments = state.balanceAfterPlannedPayments
        )

        Spacer(Modifier.height(32.dp))

        CloseButton()

        Spacer(Modifier.height(48.dp))
    }

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            choosePeriodModal = null
        }
    ) {
        onEvent(BalanceEvent.OnSetPeriod(it))
    }
}

@Composable
private fun ColumnScope.CurrentBalance(
    currency: String,
    currentBalance: Double
) {
    Text(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        text = stringResource(R.string.current_balance),
        style = UI.typo.b2.style(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(4.dp))

    BalanceRow(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        currency = currency,
        balance = currentBalance
    )
}

@Composable
private fun ColumnScope.BalanceAfterPlannedPayments(
    currency: String,
    currentBalance: Double,
    plannedPaymentsAmount: Double,
    balanceAfterPlannedPayments: Double
) {
    Text(
        modifier = Modifier
            .padding(horizontal = 32.dp),
        text = stringResource(R.string.balance_after_payments),
        style = UI.typo.b2.style(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
    )

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(32.dp))

        BalanceRow(
            currency = currency,
            balance = balanceAfterPlannedPayments,

            balanceFontSize = 30.sp,
            currencyFontSize = 18.sp,

            currencyUpfront = false
        )

        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "${currentBalance.format(2)} $currency",
                style = UI.typo.nC.style(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(Modifier.height(2.dp))

            val plusSign = if (plannedPaymentsAmount >= 0) "+" else ""
            Text(
                text = "${plusSign}${plannedPaymentsAmount.format(2)} $currency",
                style = UI.typo.nC.style(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun ColumnScope.CloseButton() {
    val nav = navigation()
    IvyCloseButton(
        onClick = { nav.back() },
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(FAB_BUTTON_SIZE),
        tonal = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceHistorySection(
    state: BalanceState,
    onEvent: (BalanceEvent) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.balance_over_time),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            BalanceHistoryRange.entries.forEachIndexed { index, range ->
                SegmentedButton(
                    selected = state.historyRange == range,
                    onClick = { onEvent(BalanceEvent.OnSetHistoryRange(range)) },
                    shape = SegmentedButtonDefaults.itemShape(index, BalanceHistoryRange.entries.size),
                ) {
                    Text(text = stringResource(range.labelRes))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
        val values = remember(state.history) {
            state.history.map { it.amount.toFloat() }.toImmutableList()
        }
        IvyLineChart(
            values = values,
            selectedIndex = state.selectedHistoryIndex,
            formatValue = { shortenAmount(it.toDouble()) },
            formatIndex = { index -> state.history.getOrNull(index)?.date?.format(dateFormatter).orEmpty() },
            onSelect = { onEvent(BalanceEvent.OnSelectHistoryPoint(it)) },
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.balance_history_rates_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun Preview(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme) {
        UI(
            state = BalanceState(
                period = TimePeriod(month = Month.monthsList().first()),
                baseCurrencyCode = "BGN",
                currentBalance = 9326.55,
                balanceAfterPlannedPayments = 8426.0,
                plannedPaymentsAmount = -900.55,
                history = persistentListOf(),
                historyRange = BalanceHistoryRange.M3,
                selectedHistoryIndex = null,
            )
        )
    }
}

/** For screenshot testing */
@Composable
fun BalanceScreenUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    Preview(theme)
}