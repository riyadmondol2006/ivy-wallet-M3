package com.ivy.wallet.domain.action.wallet

import arrow.core.Some
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.legacy.domain.pure.transaction.AccountValueFunctions
import com.ivy.legacy.utils.atEndOfDay
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.action.exchange.ExchangeAct
import com.ivy.wallet.domain.pure.exchange.ExchangeData
import com.ivy.wallet.domain.pure.wallet.BalancePoint
import com.ivy.wallet.domain.pure.wallet.balanceHistory
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Daily total-balance history for every account that counts towards the balance, converted to the
 * base currency at today's exchange rates (the app stores no historical rates).
 */
class BalanceHistoryAct @Inject constructor(
    private val accountRepository: AccountRepository,
    private val calcAccBalanceAct: CalcAccBalanceAct,
    private val transactionRepository: TransactionRepository,
    private val exchangeAct: ExchangeAct,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) {
    suspend operator fun invoke(baseCurrency: String, from: LocalDate): List<BalancePoint> {
        val accounts = accountRepository.findAll().filter { it.includeInBalance }
        if (accounts.isEmpty()) return emptyList()
        val today = timeProvider.localDateNow()

        val currentBalances = accounts.associate { account ->
            account.id.value to calcAccBalanceAct(CalcAccBalanceAct.Input(account)).balance
        }
        val rates = accounts.associate { account -> account.id.value to rateToBase(account, baseCurrency) }
        val deltasByDay = with(timeConverter) {
            transactionRepository.findAllBetween(
                startDate = from.atStartOfDay().toUTC(),
                endDate = today.atEndOfDay().toUTC(),
            ).groupBy { it.time.toLocalDate() }
        }.mapValues { (_, transactions) ->
            accounts.associate { account ->
                account.id.value to transactions.sumOf { trn ->
                    AccountValueFunctions.balance(trn, account.id.value)
                }
            }
        }

        return balanceHistory(
            currentBalances = currentBalances,
            deltasByDay = deltasByDay,
            rates = rates,
            from = from,
            today = today,
        )
    }

    private suspend fun rateToBase(account: Account, baseCurrency: String): BigDecimal =
        exchangeAct(
            ExchangeAct.Input(
                data = ExchangeData(baseCurrency = baseCurrency, fromCurrency = Some(account.asset.code)),
                amount = BigDecimal.ONE,
            ),
        ).getOrNull() ?: BigDecimal.ONE
}

/** Convenience for callers that only have UUIDs. */
fun Map<UUID, BigDecimal>.sumOfBalances(): BigDecimal = values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
