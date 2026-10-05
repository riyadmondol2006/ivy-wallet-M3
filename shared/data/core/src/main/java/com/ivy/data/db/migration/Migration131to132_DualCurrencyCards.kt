package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassNaming", "MagicNumber")
class Migration131to132_DualCurrencyCards : Migration(131, 132) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditCardGroupId TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimitShared INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditExchangeRate REAL DEFAULT NULL")
    }
}
