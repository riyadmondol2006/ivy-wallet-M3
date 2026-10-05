package com.ivy.legacy.data.model

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.domain.validMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class CreditCardDataTest {
    private val root = AccountId(UUID(0, 1))
    private fun ledger(currency: String, limit: Double, owed: Double, secondary: Boolean = false) = AccountData(
        account = Account(
            id = if (secondary) AccountId(UUID(0, 2)) else root,
            name = NotBlankTrimmedString.unsafe("Visa"),
            asset = AssetCode.unsafe(currency), color = ColorInt(0), icon = null,
            includeInBalance = false, orderNum = 0.0, creditLimit = limit, creditCardGroupId = root,
        ),
        balance = -owed,
        balanceBaseCurrency = null,
        monthlyExpenses = 0.0,
        monthlyIncome = 0.0,
    )
    private fun card(
        shared: Boolean = true,
        rate: Double? = 120.0,
        mainOwed: Double = 20000.0,
        foreignOwed: Double = 100.0
    ) =
        CreditCardData(
            ledger(
                "BDT",
                100000.0,
                mainOwed
            ).let { it.copy(account = it.account.copy(creditLimitShared = shared, creditExchangeRate = rate)) },
            ledger("USD", 1000.0, foreignOwed, secondary = true),
        )

    @Test fun sharedLimitConsumesBothBalances() {
        val stats = card().stats()
        assertEquals(68000.0, stats[0].available!!, 0.0)
        assertEquals(566.66, stats[1].available!!, 0.0)
        assertEquals(20000.0, stats[0].toPay, 0.0)
        assertEquals(100.0, stats[1].toPay, 0.0)
        assertTrue(stats.all { it.estimated })
    }

    @Test fun independentLimitsDoNotConvertDebt() {
        val stats = card(shared = false).stats()
        assertEquals(80000.0, stats[0].available!!, 0.0)
        assertEquals(900.0, stats[1].available!!, 0.0)
        assertFalse(stats.any { it.estimated })
    }

    @Test fun foreignCapRestrictsSpendingEvenWithOverallRoom() {
        val stats = card(rate = 10.0).stats()
        assertEquals(900.0, stats[1].available!!, 0.0)
    }

    @Test fun exceedingOverallLimitClampsBothAvailableAmounts() {
        assertTrue(card(mainOwed = 100000.0).stats().all { it.available == 0.0 })
    }

    @Test fun exceedingForeignCapDoesNotEraseRemainingMainCredit() {
        val stats = card(rate = 10.0, foreignOwed = 1100.0).stats()
        assertEquals(69000.0, stats[0].available!!, 0.0)
        assertEquals(0.0, stats[1].available!!, 0.0)
    }

    @Test fun missingOrInvalidRateDoesNotInventAvailableCredit() {
        listOf(null, 0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { rate ->
            assertTrue(card(rate = rate).stats().all { it.available == null })
        }
    }

    @Test fun paymentsRestoreSharedCreditInBothViews() {
        val before = card().stats()
        val after = card(foreignOwed = 0.0).stats()
        assertEquals(before[0].available!! + 12000.0, after[0].available!!, 0.0)
        assertEquals(0.0, after[1].toPay, 0.0)
    }

    @Test fun positiveBalancesAreNotDebtOrExtraCredit() {
        val stats = card(mainOwed = -500.0, foreignOwed = 0.0).stats()
        assertEquals(0.0, stats[0].toPay, 0.0)
        assertEquals(100000.0, stats[0].available!!, 0.0)
    }

    @Test fun groupingCountsPhysicalCardsAndKeepsOrphanDebtVisible() {
        val dual = card()
        assertEquals(listOf(dual), groupCreditCards(dual.accounts.reversed()))
        assertEquals(1, groupCreditCards(listOf(dual.secondary!!)).size)
    }

    @Test fun totalsNeverMixCurrencies() {
        val totals = creditCurrencyTotals(listOf(card(), card(shared = false)))
        assertEquals(listOf("BDT", "USD"), totals.map { it.currency })
        assertEquals(40000.0, totals[0].toPay, 0.0)
        assertEquals(200.0, totals[1].toPay, 0.0)
    }

    @Test fun totalOwedConvertsSecondCurrencyDebtAtTheBankRate() {
        val total = card().totalOwed()
        assertEquals("BDT", total.currency)
        assertEquals(32000.0, total.amount, 0.0)
        assertEquals(mapOf("USD" to 120.0), total.rates)
        assertTrue(total.missingRateCurrencies.isEmpty())
        assertTrue(total.estimated)
    }

    @Test fun totalOwedWorksWithSeparateLimitsWhenARateIsEntered() {
        assertEquals(32000.0, card(shared = false).totalOwed().amount, 0.0)
    }

    @Test fun totalOwedWithoutARateCountsOnlyTheMainCurrencyAndSaysWhatIsMissing() {
        listOf(null, 0.0, Double.NaN).forEach { rate ->
            val total = card(rate = rate).totalOwed()
            assertEquals(20000.0, total.amount, 0.0)
            assertEquals(listOf("USD"), total.missingRateCurrencies)
            assertFalse(total.estimated)
        }
    }

    @Test fun totalOwedIgnoresPositiveBalancesAndSingleCurrencyCardsNeedNoRate() {
        val total = card(mainOwed = -500.0, foreignOwed = 50.0).totalOwed()
        assertEquals(6000.0, total.amount, 0.0)
        val paidOff = card(foreignOwed = 0.0).totalOwed()
        assertEquals(20000.0, paidOff.amount, 0.0)
        assertTrue(paidOff.rates.isEmpty())
        val single = CreditCardData(ledger("BDT", 1000.0, 250.0)).totalOwed()
        assertEquals(250.0, single.amount, 0.0)
        assertTrue(single.missingRateCurrencies.isEmpty())
    }

    @Test fun owedTotalsGroupByMainCurrencyAndMergeRatesAndMissingOnes() {
        val usdCard = CreditCardData(
            ledger("USD", 5000.0, 300.0).let { it.copy(account = it.account.copy(id = AccountId(UUID(0, 3)))) },
            ledger("EUR", 1000.0, 100.0, secondary = true),
        )
        val totals = creditOwedTotals(listOf(card(), card(rate = null), usdCard))
        assertEquals(listOf("BDT", "USD"), totals.map { it.currency })
        assertEquals(52000.0, totals[0].amount, 0.0)
        assertEquals(mapOf("USD" to 120.0), totals[0].rates)
        assertEquals(listOf("USD"), totals[0].missingRateCurrencies)
        assertEquals(300.0, totals[1].amount, 0.0)
        assertEquals(listOf("EUR"), totals[1].missingRateCurrencies)
    }

    @Test fun paymentAmountsMustBePositiveFiniteAndCurrencyPrecision() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 1.001).forEach {
            assertFalse(validMoney(it, "BDT"))
        }
        assertTrue(validMoney(12.34, "BDT"))
    }

    @Test fun amountParsingNeverMistakesGroupingForDecimals() {
        assertNull(com.ivy.legacy.domain.parseCreditAmount("1,000", java.util.Locale.US))
        assertEquals(1000.50, com.ivy.legacy.domain.parseCreditAmount("1000.50", java.util.Locale.US)!!, 0.0)
        assertEquals(10.50, com.ivy.legacy.domain.parseCreditAmount("10,50", java.util.Locale.GERMANY)!!, 0.0)
        assertEquals(12.5, com.ivy.legacy.domain.parseCreditAmount("১২.৫", java.util.Locale.US)!!, 0.0)
        assertNull(com.ivy.legacy.domain.parseCreditAmount("NaN"))
        assertNull(com.ivy.legacy.domain.parseCreditAmount("1e3"))
    }
}

class CreditCardCycleTest {
    private fun card(statementDay: Int?, dueDay: Int?, owed: Double) = CreditCardData(
        primary = AccountData(
            account = Account(
                id = AccountId(UUID(0, 9)),
                name = NotBlankTrimmedString.unsafe("Visa"),
                asset = AssetCode.unsafe("USD"),
                color = ColorInt(0),
                icon = null,
                includeInBalance = true,
                orderNum = 0.0,
                creditLimit = 1000.0,
                creditStatementDay = statementDay,
                creditDueDay = dueDay,
            ),
            balance = -owed,
            balanceBaseCurrency = null,
            monthlyExpenses = 0.0,
            monthlyIncome = 0.0,
        ),
    )

    @Test
    fun `statement earlier this month is due later this month`() {
        val today = LocalDate.of(2026, 10, 10)
        val cycle = card(statementDay = 5, dueDay = 25, owed = 100.0).currentCycle(today)
        assertEquals(LocalDate.of(2026, 10, 5), cycle?.statementDate)
        assertEquals(LocalDate.of(2026, 10, 25), cycle?.dueDate)
        assertEquals(CreditDueStatus.DueIn(15, LocalDate.of(2026, 10, 25)), card(5, 25, 100.0).dueStatus(today))
    }

    @Test
    fun `due day before statement day rolls into the next month`() {
        val today = LocalDate.of(2026, 10, 28)
        val cycle = card(statementDay = 25, dueDay = 10, owed = 100.0).currentCycle(today)
        assertEquals(LocalDate.of(2026, 10, 25), cycle?.statementDate)
        assertEquals(LocalDate.of(2026, 11, 10), cycle?.dueDate)
    }

    @Test
    fun `statement day 31 is clamped in short months`() {
        val today = LocalDate.of(2026, 2, 20)
        val cycle = card(statementDay = 31, dueDay = 15, owed = 100.0).currentCycle(today)
        assertEquals(LocalDate.of(2026, 1, 31), cycle?.statementDate)
        assertEquals(LocalDate.of(2026, 2, 15), cycle?.dueDate)
        assertEquals(LocalDate.of(2026, 2, 28), card(31, 15, 100.0).nextStatementDate(today))
    }

    @Test
    fun `passed due date with debt is overdue and no debt shows next statement`() {
        val today = LocalDate.of(2026, 10, 28)
        assertEquals(CreditDueStatus.Overdue(LocalDate.of(2026, 10, 25)), card(5, 25, 100.0).dueStatus(today))
        assertEquals(CreditDueStatus.NextStatement(LocalDate.of(2026, 11, 5)), card(5, 25, 0.0).dueStatus(today))
        assertNull(card(null, null, 100.0).dueStatus(today))
    }

    @Test
    fun `nearest due date picks the earliest card that owes money`() {
        val today = LocalDate.of(2026, 10, 10)
        val cards = listOf(card(5, 28, 100.0), card(5, 20, 50.0), card(5, 12, 0.0))
        assertEquals(LocalDate.of(2026, 10, 20), cards.nearestDueDate(today))
        assertNull(listOf(card(5, 12, 0.0)).nearestDueDate(today))
    }
}
