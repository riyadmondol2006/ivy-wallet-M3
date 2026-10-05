package com.ivy.legacy.domain.pure.transaction

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.wallet.domain.pure.transaction.plannedDueSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class PlannedDueSummaryTest {
    private val today = LocalDate.of(2026, 10, 5)
    private val toLocalDate: (Instant) -> LocalDate = { it.atZone(ZoneOffset.UTC).toLocalDate() }

    private fun due(date: LocalDate, amount: Double, paid: Boolean = false) = TransactionEntity(
        accountId = UUID.randomUUID(),
        type = TransactionType.EXPENSE,
        amount = amount,
        dateTime = if (paid) date.atStartOfDay().toInstant(ZoneOffset.UTC) else null,
        dueDate = date.atStartOfDay().toInstant(ZoneOffset.UTC),
    )

    @Test
    fun `buckets due today and overdue and sums both`() {
        val summary = plannedDueSummary(
            dueTransactions = listOf(
                due(today, 100.0),
                due(today.minusDays(3), 25.5),
                due(today.minusDays(1), 4.5),
            ),
            today = today,
            toLocalDate = toLocalDate,
        )

        assertEquals(1, summary.dueToday)
        assertEquals(2, summary.overdue)
        assertEquals(130.0, summary.totalAmount, 0.001)
    }

    @Test
    fun `future and already paid payments are ignored`() {
        val summary = plannedDueSummary(
            dueTransactions = listOf(
                due(today.plusDays(1), 100.0),
                due(today, 50.0, paid = true),
            ),
            today = today,
            toLocalDate = toLocalDate,
        )

        assertTrue(summary.isEmpty)
        assertEquals(0.0, summary.totalAmount, 0.001)
    }
}
