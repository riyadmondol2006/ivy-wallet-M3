package com.ivy.data.db

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs a block of database writes atomically: all of them are applied, or none. */
interface DbTransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

class RoomDbTransactionRunner @Inject constructor(
    private val db: IvyRoomDatabase,
) : DbTransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)
}
