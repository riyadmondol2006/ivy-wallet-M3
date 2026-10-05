package com.ivy.legacy.data.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.AccountId
import com.ivy.data.model.isSecondaryCreditCurrency
import com.ivy.legacy.utils.withDayOfMonthSafe
import com.ivy.wallet.domain.data.IvyCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val RateDivisionScale = 12

@Immutable
@Suppress("DataClassDefaultValues")
data class CreditCurrencyStats(
    val currency: String,
    val toPay: Double,
    val limit: Double,
    val available: Double?,
    val estimated: Boolean = false,
    val limitIsCap: Boolean = false,
)

@Immutable
@Suppress("DataClassDefaultValues", "DataClassFunctions")
data class CreditCardData(
    val primary: AccountData,
    val secondary: AccountData? = null,
) {
    val accounts: List<AccountData> get() = listOfNotNull(primary, secondary)
    val shared: Boolean get() = secondary != null && primary.account.creditLimitShared

    @Suppress("ReturnCount")
    fun stats(): List<CreditCurrencyStats> {
        val first = primary.standaloneStats()
        val second = secondary?.standaloneStats()?.copy(limitIsCap = shared) ?: return listOf(first)
        if (!shared) return listOf(first, second)
        val rate = primary.account.creditExchangeRate
            ?.takeIf { it.isFinite() && it > 0.0 }?.toBigDecimal()
            ?: return listOf(first.copy(available = null), second.copy(available = null))
        val remaining = (
            first.limit.toBigDecimal() - first.toPay.toBigDecimal() -
                second.toPay.toBigDecimal() * rate
            ).max(BigDecimal.ZERO)
        val secondRemaining = (second.limit.toBigDecimal() - second.toPay.toBigDecimal())
            .max(BigDecimal.ZERO)
            .min(remaining.divide(rate, RateDivisionScale, RoundingMode.DOWN))
        return listOf(
            first.copy(
                available = remaining.moneyDown(first.currency),
                estimated = true,
            ),
            second.copy(
                available = secondRemaining.moneyDown(second.currency),
                estimated = true,
            ),
        )
    }
}

/**
 * Everything a card (or all cards sharing a main currency) owes, expressed in [currency].
 * Secondary-currency debt is converted at the bank rate the user entered, so it is an estimate.
 */
@Immutable
data class CreditOwedTotal(
    val currency: String,
    val amount: Double,
    /** Bank rates used for the conversion: second currency -> units of [currency] per one unit. */
    val rates: Map<String, Double>,
    /** Second currencies whose debt is not included because their card has no bank rate. */
    val missingRateCurrencies: List<String>,
) {
    val estimated: Boolean get() = rates.isNotEmpty()
}

/** Owed in the main currency: primary debt plus the second currency's debt at the bank rate. */
fun CreditCardData.totalOwed(): CreditOwedTotal {
    val currency = primary.account.asset.code
    val primaryOwed = (-primary.balance.toBigDecimal()).max(BigDecimal.ZERO)
    val secondCurrency = secondary?.account?.asset?.code
    val secondOwed = secondary?.let { (-it.balance.toBigDecimal()).max(BigDecimal.ZERO) } ?: BigDecimal.ZERO
    val rate = primary.account.creditExchangeRate?.takeIf { it.isFinite() && it > 0.0 }
    return when {
        secondCurrency == null -> CreditOwedTotal(currency, primaryOwed.moneyDown(currency), emptyMap(), emptyList())
        rate == null -> CreditOwedTotal(currency, primaryOwed.moneyDown(currency), emptyMap(), listOf(secondCurrency))
        else -> CreditOwedTotal(
            currency = currency,
            amount = (primaryOwed + secondOwed * rate.toBigDecimal()).moneyDown(currency),
            rates = if (secondOwed.signum() > 0) mapOf(secondCurrency to rate) else emptyMap(),
            missingRateCurrencies = emptyList(),
        )
    }
}

/** One total per main currency across cards; second-currency debt is converted per card. */
fun creditOwedTotals(cards: List<CreditCardData>): List<CreditOwedTotal> =
    cards.map { it.totalOwed() }.groupBy { it.currency }.map { (currency, totals) ->
        CreditOwedTotal(
            currency = currency,
            amount = totals.sumOf { it.amount.toBigDecimal() }.moneyDown(currency),
            rates = totals.flatMap { it.rates.entries }.associate { it.key to it.value },
            missingRateCurrencies = totals.flatMap { it.missingRateCurrencies }.distinct(),
        )
    }

private fun BigDecimal.moneyDown(currency: String): Double =
    setScale(IvyCurrency.getDecimalPlaces(currency), RoundingMode.DOWN).toDouble()

private fun AccountData.standaloneStats(): CreditCurrencyStats {
    val owed = (-balance.toBigDecimal()).max(BigDecimal.ZERO)
    val limit = (account.creditLimit ?: 0.0).toBigDecimal()
    return CreditCurrencyStats(
        currency = account.asset.code,
        toPay = owed.toDouble(),
        limit = limit.toDouble(),
        available = (limit - owed).max(BigDecimal.ZERO).moneyDown(account.asset.code),
    )
}

/** Group the two currency ledgers into one physical card, keeping account order. */
fun groupCreditCards(accounts: List<AccountData>): List<CreditCardData> {
    val cards = accounts.filter { it.account.creditLimit != null }
    val roots = cards.filter { !it.account.isSecondaryCreditCurrency }
    val usedIds = mutableSetOf<AccountId>()
    val grouped = roots.map { primary ->
        val secondary = cards.firstOrNull {
            it.account.isSecondaryCreditCurrency &&
                it.account.creditCardGroupId == primary.account.id
        }
        usedIds += primary.account.id
        secondary?.let { usedIds += it.account.id }
        CreditCardData(primary, secondary)
    }
    // Keep imported/orphaned currency accounts visible rather than dropping their debt.
    return grouped + cards.filter { it.account.id !in usedIds }.map { CreditCardData(it) }
}

/** Totals stay in their native currencies: USD and BDT are never added together. */
fun creditCurrencyTotals(cards: List<CreditCardData>): List<CreditCurrencyStats> =
    cards.flatMap { it.stats() }.groupBy { it.currency }.map { (currency, values) ->
        CreditCurrencyStats(
            currency = currency,
            toPay = values.sumOf { it.toPay.toBigDecimal() }.toDouble(),
            limit = values.sumOf { it.limit.toBigDecimal() }.toDouble(),
            available = if (values.any { it.available == null }) {
                null
            } else {
                values.sumOf { it.available!!.toBigDecimal() }.toDouble()
            },
            estimated = values.any { it.estimated },
            limitIsCap = values.any { it.limitIsCap },
        )
    }

@Suppress("DataClassDefaultValues", "DataClassTypedIDs")
data class CreditCardInput(
    val primaryId: AccountId?,
    val name: String,
    val currency: String,
    val limit: Double,
    val color: Int,
    val icon: String?,
    val secondaryCurrency: String?,
    val secondaryLimit: Double?,
    val sharedLimit: Boolean,
    val exchangeRate: Double?,
    /**
     * Whether the card's debt counts against the total balance and its purchases show up in the
     * income/expense statistics. Defaults to true so card spending is visible like any other account.
     */
    val includeInBalance: Boolean = true,
    /** Statement closing day of month (1..31); set together with [dueDay] or not at all. */
    val statementDay: Int? = null,
    /** Payment due day of month (1..31). */
    val dueDay: Int? = null,
)

@Suppress("DataClassTypedIDs")
data class CreditCardPaymentInput(
    val cardAccountId: AccountId,
    val fromAccountId: AccountId,
    val paidAmount: Double,
    val debitedAmount: Double,
)

/** One billing cycle: the statement that closed on [statementDate] must be paid by [dueDate]. */
@Immutable
data class CreditCardCycle(
    val statementDate: LocalDate,
    val dueDate: LocalDate,
)

/** What the card UI should say about the next payment. */
@Immutable
sealed interface CreditDueStatus {
    /** Payment for the latest statement is due in [days] (0 = today) on [dueDate]. */
    data class DueIn(val days: Long, val dueDate: LocalDate) : CreditDueStatus

    /** The latest statement's due date has passed and the card still carries a balance. */
    data class Overdue(val dueDate: LocalDate) : CreditDueStatus

    /** Nothing is owed; the next statement closes on [statementDate]. */
    data class NextStatement(val statementDate: LocalDate) : CreditDueStatus
}

/** The most recent statement on or before [today] and the payment due date that follows it. */
fun CreditCardData.currentCycle(today: LocalDate): CreditCardCycle? {
    val statementDay = primary.account.creditStatementDay
    val dueDay = primary.account.creditDueDay
    if (statementDay == null || dueDay == null) return null
    var statement = today.withDayOfMonthSafe(statementDay)
    if (statement.isAfter(today)) statement = today.minusMonths(1).withDayOfMonthSafe(statementDay)
    return CreditCardCycle(statementDate = statement, dueDate = dueDateAfter(statement, dueDay))
}

/** The first statement closing date strictly after [today], or null when no cycle is set. */
fun CreditCardData.nextStatementDate(today: LocalDate): LocalDate? {
    val statementDay = primary.account.creditStatementDay ?: return null
    val sameMonth = today.withDayOfMonthSafe(statementDay)
    return if (sameMonth.isAfter(today)) sameMonth else today.plusMonths(1).withDayOfMonthSafe(statementDay)
}

/** Days from [today] to the current cycle's due date (negative when overdue); null without a cycle. */
fun CreditCardData.daysUntilDue(today: LocalDate): Long? =
    currentCycle(today)?.let { ChronoUnit.DAYS.between(today, it.dueDate) }

/** True when any ledger of the card carries debt. */
val CreditCardData.owesMoney: Boolean
    get() = accounts.any { it.balance < 0.0 }

fun CreditCardData.dueStatus(today: LocalDate): CreditDueStatus? {
    val cycle = currentCycle(today) ?: return null
    val days = ChronoUnit.DAYS.between(today, cycle.dueDate)
    return when {
        !owesMoney -> nextStatementDate(today)?.let(CreditDueStatus::NextStatement)
        days < 0 -> CreditDueStatus.Overdue(cycle.dueDate)
        else -> CreditDueStatus.DueIn(days, cycle.dueDate)
    }
}

/** The due date on or after the statement date; a due day before the statement day rolls over a month. */
private fun dueDateAfter(statement: LocalDate, dueDay: Int): LocalDate {
    val sameMonth = statement.withDayOfMonthSafe(dueDay)
    return if (sameMonth.isAfter(statement)) sameMonth else statement.plusMonths(1).withDayOfMonthSafe(dueDay)
}

/** Earliest upcoming (or overdue) payment date across cards that still owe money. */
fun List<CreditCardData>.nearestDueDate(today: LocalDate): LocalDate? = mapNotNull { card ->
    when (val status = card.dueStatus(today)) {
        is CreditDueStatus.DueIn -> status.dueDate
        is CreditDueStatus.Overdue -> status.dueDate
        is CreditDueStatus.NextStatement, null -> null
    }
}.minOrNull()
