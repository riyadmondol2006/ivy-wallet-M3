package com.ivy.legacy.domain.pure.wallet

import com.ivy.wallet.domain.pure.wallet.balanceHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class BalanceHistoryTest {
    private val cash = UUID(0, 1)
    private val euros = UUID(0, 2)
    private val today = LocalDate.of(2026, 10, 5)

    @Test
    fun `walks balances back one day at a time`() {
        val points = balanceHistory(
            currentBalances = mapOf(cash to BigDecimal("100")),
            deltasByDay = mapOf(
                today to mapOf(cash to BigDecimal("-20")), // spent 20 today
                today.minusDays(2) to mapOf(cash to BigDecimal("50")), // earned 50 two days ago
            ),
            rates = mapOf(cash to BigDecimal.ONE),
            from = today.minusDays(3),
            today = today,
        )

        assertEquals(listOf(70.0, 120.0, 120.0, 100.0), points.map { it.amount })
        assertEquals(today.minusDays(3), points.first().date)
        assertEquals(today, points.last().date)
    }

    @Test
    fun `converts each account at its own rate`() {
        val points = balanceHistory(
            currentBalances = mapOf(cash to BigDecimal("100"), euros to BigDecimal("10")),
            deltasByDay = emptyMap(),
            rates = mapOf(cash to BigDecimal.ONE, euros to BigDecimal("2")),
            from = today,
            today = today,
        )

        assertEquals(1, points.size)
        assertEquals(120.0, points.single().amount, 0.0001)
    }

    @Test
    fun `empty when the range starts after today`() {
        assertTrue(
            balanceHistory(
                currentBalances = mapOf(cash to BigDecimal.TEN),
                deltasByDay = emptyMap(),
                rates = emptyMap(),
                from = today.plusDays(1),
                today = today,
            ).isEmpty()
        )
    }
}
