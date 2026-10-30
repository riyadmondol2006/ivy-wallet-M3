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

    private val account = UUID.randomUUID()

    private fun due(
        date: LocalDate,
        amount: Double,
        paid: Boolean = false,
        type: TransactionType = TransactionType.EXPENSE,
        accountId: UUID = account,
    ) = TransactionEntity(
        accountId = accountId,
        type = type,
        amount = amount,
        dateTime = if (paid) date.atStartOfDay().toInstant(ZoneOffset.UTC) else null,
        dueDate = date.atStartOfDay().toInstant(ZoneOffset.UTC),
    )

    @Test
    fun `buckets due today and overdue and sums expenses per account`() {
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
        assertEquals(mapOf(account to 130.0), summary.expensesByAccount)
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
        assertTrue(summary.expensesByAccount.isEmpty())
    }

    @Test
    fun `income is counted as due but not added to the expense total`() {
        val other = UUID.randomUUID()
        val summary = plannedDueSummary(
            dueTransactions = listOf(
                due(today, 100.0),
                due(today, 5_000.0, type = TransactionType.INCOME, accountId = other),
            ),
            today = today,
            toLocalDate = toLocalDate,
        )

        assertEquals(2, summary.dueToday)
        assertEquals(mapOf(account to 100.0), summary.expensesByAccount)
    }
}
