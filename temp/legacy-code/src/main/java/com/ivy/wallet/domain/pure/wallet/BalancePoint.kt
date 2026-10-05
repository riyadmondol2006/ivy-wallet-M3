package com.ivy.wallet.domain.pure.wallet

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/** Total balance (in the base currency) at the end of [date]. */
data class BalancePoint(
    val date: LocalDate,
    val amount: Double,
)

/**
 * Walks backwards from today's per-account balances to reconstruct the daily total balance.
 *
 * @param currentBalances today's balance per account, in each account's own currency.
 * @param deltasByDay net change per account per calendar day (income positive, expense negative,
 *  transfers on both sides). Days without transactions may be absent.
 * @param rates base-currency units per one unit of each account's currency (today's rates).
 * @param from first day to report; [today] is the last.
 * @return one point per day from [from] to [today], ascending.
 */
fun balanceHistory(
    currentBalances: Map<UUID, BigDecimal>,
    deltasByDay: Map<LocalDate, Map<UUID, BigDecimal>>,
    rates: Map<UUID, BigDecimal>,
    from: LocalDate,
    today: LocalDate,
): List<BalancePoint> {
    if (from.isAfter(today)) return emptyList()
    val balances = currentBalances.toMutableMap()
    val points = ArrayDeque<BalancePoint>()
    var day = today
    while (!day.isBefore(from)) {
        points.addFirst(BalancePoint(date = day, amount = total(balances, rates)))
        // Undo this day's movements to get the balance at the end of the previous day.
        deltasByDay[day]?.forEach { (accountId, delta) ->
            balances[accountId] = (balances[accountId] ?: BigDecimal.ZERO) - delta
        }
        day = day.minusDays(1)
    }
    return points.toList()
}

private fun total(balances: Map<UUID, BigDecimal>, rates: Map<UUID, BigDecimal>): Double =
    balances.entries.sumOf { (accountId, balance) ->
        balance * (rates[accountId] ?: BigDecimal.ONE)
    }.toDouble()
