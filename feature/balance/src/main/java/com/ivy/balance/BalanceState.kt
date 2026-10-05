package com.ivy.balance

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.ui.R
import com.ivy.wallet.domain.pure.wallet.BalancePoint
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class BalanceState(
    val period: TimePeriod,
    val baseCurrencyCode: String,
    val currentBalance: Double,
    val plannedPaymentsAmount: Double,
    val balanceAfterPlannedPayments: Double,
    /** Daily (or weekly, for long ranges) total balance over [historyRange], oldest first. */
    val history: ImmutableList<BalancePoint>,
    val historyRange: BalanceHistoryRange,
    /** Index into [history] the user tapped on the chart, if any. */
    val selectedHistoryIndex: Int?,
)

/** How far back the balance chart looks. */
enum class BalanceHistoryRange(val months: Long, @StringRes val labelRes: Int) {
    M1(months = 1, labelRes = R.string.balance_range_1m),
    M3(months = 3, labelRes = R.string.balance_range_3m),
    M6(months = 6, labelRes = R.string.balance_range_6m),
    Y1(months = 12, labelRes = R.string.balance_range_1y),
    ALL(months = AllRangeMonths, labelRes = R.string.balance_range_all),
}

/** Five years: effectively "everything" for a personal ledger without an unbounded query. */
private const val AllRangeMonths = 60L
