package com.ivy.wallet.domain.action.viewmodel.account

import arrow.core.toOption
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.isSecondaryCreditCurrency
import com.ivy.frp.action.FPAction
import com.ivy.frp.action.thenMap
import com.ivy.frp.then
import com.ivy.legacy.data.model.latestStatementDate
import com.ivy.legacy.utils.atEndOfDay
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.account.CalcAccIncomeExpenseAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import javax.inject.Inject

class AccountDataAct @Inject constructor(
    private val exchangeAct: ExchangeAct,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val calcAccIncomeExpenseAct: CalcAccIncomeExpenseAct,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : FPAction<AccountDataAct.Input, ImmutableList<com.ivy.legacy.data.model.AccountData>>() {

    override suspend fun Input.compose(): suspend () -> ImmutableList<com.ivy.legacy.data.model.AccountData> = suspend {
        accounts
    } thenMap { acc ->
        val balance = calcAccBalanceAct(
            CalcAccBalanceAct.Input(
                account = acc
            )
        ).balance

        val balanceBaseCurrency = if (acc.asset.code != baseCurrency) {
            exchangeAct(
                ExchangeAct.Input(
                    data = ExchangeData(
                        baseCurrency = baseCurrency,
                        fromCurrency = acc.asset.code.toOption()
                    ),
                    amount = balance
                )
            ).orNull()
        } else {
            null
        }

        val incomeExpensePair = calcAccIncomeExpenseAct(
            CalcAccIncomeExpenseAct.Input(
                account = acc,
                range = range,
                includeTransfersInCalc = includeTransfersInCalc
            )
        ).incomeExpensePair

        com.ivy.legacy.data.model.AccountData(
            account = acc,
            balance = balance.toDouble(),
            balanceBaseCurrency = balanceBaseCurrency?.toDouble(),
            monthlyIncome = incomeExpensePair.income.toDouble(),
            monthlyExpenses = incomeExpensePair.expense.toDouble(),
            statementOwed = statementOwed(acc, accounts),
        )
    } then {
        it.toImmutableList()
    }

    /**
     * What is still unpaid from the card's latest closed statement: the debt at the end of the
     * statement day minus everything paid into the card since. Null for non-card accounts and
     * cards without a billing cycle. A second-currency ledger follows its main card's cycle.
     */
    private suspend fun statementOwed(account: Account, all: List<Account>): Double? {
        val cycleOwner = if (account.isSecondaryCreditCurrency) {
            all.firstOrNull { it.id == account.creditCardGroupId } ?: account
        } else {
            account
        }
        val statementDay = cycleOwner.creditStatementDay
            ?.takeIf { account.creditLimit != null && cycleOwner.creditDueDay != null }
            ?: return null

        val now = timeProvider.utcNow()
        val statementDate = latestStatementDate(timeProvider.localDateNow(), statementDay)
        val statementEnd = with(timeConverter) { statementDate.atEndOfDay().toUTC() }.coerceAtMost(now)

        val balanceAtStatement = calcAccBalanceAct(
            CalcAccBalanceAct.Input(account = account, range = ClosedTimeRange.to(statementEnd))
        ).balance
        val paidSince = if (statementEnd < now) {
            calcAccIncomeExpenseAct(
                CalcAccIncomeExpenseAct.Input(
                    account = account,
                    range = ClosedTimeRange(from = statementEnd, to = now),
                    includeTransfersInCalc = true,
                )
            ).incomeExpensePair.income
        } else {
            BigDecimal.ZERO
        }
        return (balanceAtStatement.negate() - paidSince).max(BigDecimal.ZERO).toDouble()
    }

    data class Input(
        val accounts: ImmutableList<com.ivy.data.model.Account>,
        val baseCurrency: String,
        val range: ClosedTimeRange,
        val includeTransfersInCalc: Boolean = false
    )
}
