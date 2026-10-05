package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the optional statement-closing day and payment-due day to credit-card accounts. */
@Suppress("ClassNaming", "MagicNumber")
class Migration132to133_CreditCardDueDates : Migration(132, 133) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditStatementDay INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditDueDay INTEGER DEFAULT NULL")
    }
}
