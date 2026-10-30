package com.ivy.wallet.domain.pure.transaction

import com.ivy.base.model.TransactionType
import com.ivy.data.db.entity.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * What the planned-payment reminder should say: how many unpaid planned payments are due today,
 * how many are already overdue, and how much the due expenses add up to per account. Amounts stay
 * per account because accounts can hold different currencies; income is not counted.
 */
data class PlannedDueSummary(
    val dueToday: Int,
    val overdue: Int,
    val expensesByAccount: Map<UUID, Double>,
) {
    val isEmpty: Boolean get() = dueToday == 0 && overdue == 0
}

/**
 * Buckets unpaid planned transactions (rows with a `dueDate` and no `dateTime`) relative to
 * [today]. Future due dates are ignored. [toLocalDate] converts a stored instant to the user's
 * calendar date, so the caller decides the time zone.
 */
fun plannedDueSummary(
    dueTransactions: List<TransactionEntity>,
    today: LocalDate,
    toLocalDate: (Instant) -> LocalDate,
): PlannedDueSummary {
    val unpaidByDueDate = dueTransactions
        .filter { it.dateTime == null }
        .mapNotNull { transaction -> transaction.dueDate?.let { toLocalDate(it) to transaction } }
        .filter { (dueDate, _) -> !dueDate.isAfter(today) }

    return PlannedDueSummary(
        dueToday = unpaidByDueDate.count { (dueDate, _) -> dueDate.isEqual(today) },
        overdue = unpaidByDueDate.count { (dueDate, _) -> dueDate.isBefore(today) },
        expensesByAccount = unpaidByDueDate
            .map { (_, transaction) -> transaction }
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.accountId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } },
    )
}
