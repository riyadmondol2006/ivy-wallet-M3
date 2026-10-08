package com.ivy.data.db

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs a block of database writes atomically: all of them are applied, or none. */
interface DbTransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

// Adapts Room's `withTransaction` extension to an interface that JVM tests can replace.
@Suppress("UnnecessaryPassThroughClass")
class RoomDbTransactionRunner @Inject constructor(
    private val db: IvyRoomDatabase,
) : DbTransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction(block)
}
