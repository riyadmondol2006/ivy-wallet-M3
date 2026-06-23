package com.ivy.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassNaming", "MagicNumber")
class Migration130to131_AccountCreditLimit : Migration(130, 131) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN creditLimit REAL DEFAULT NULL")
    }
}
