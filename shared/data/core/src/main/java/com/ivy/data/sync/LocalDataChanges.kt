package com.ivy.data.sync

import com.ivy.data.db.IvyRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Emits whenever a table that cloud backups contain is written, by any code path.
 *
 * Watches the database itself rather than repository events, because many screens (transactions,
 * budgets, loans, planned payments) write through DAOs that post no event.
 */
class LocalDataChanges @Inject constructor(
    private val db: IvyRoomDatabase,
) {
    val changes: Flow<Unit>
        get() = db.invalidationTracker
            .createFlow(
                // Every table that [com.ivy.data.backup.IvyWalletCompleteData] holds.
                "accounts",
                "transactions",
                "categories",
                "budgets",
                "loans",
                "loan_records",
                "planned_payment_rules",
                "settings",
                "tags",
                "tags_association",
                emitInitialState = false,
            )
            .map { }
}
