package com.ivy.wallet.domain.pure.transaction

import com.ivy.data.db.entity.TransactionEntity
import java.time.Instant
import java.time.LocalDate

/**
 * What the planned-payment reminder should say: how many unpaid planned payments are due today,
 * how many are already overdue, and the total they add up to.
 */
data class PlannedDueSummary(
    val dueToday: Int,
    val overdue: Int,
    val totalAmount: Double,
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
        .mapNotNull { transaction -> transaction.dueDate?.let { toLocalDate(it) to transaction.amount } }
        .filter { (dueDate, _) -> !dueDate.isAfter(today) }

    return PlannedDueSummary(
        dueToday = unpaidByDueDate.count { (dueDate, _) -> dueDate.isEqual(today) },
        overdue = unpaidByDueDate.count { (dueDate, _) -> dueDate.isBefore(today) },
        totalAmount = unpaidByDueDate.sumOf { (_, amount) -> amount },
    )
}
