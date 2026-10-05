package com.ivy.wallet.domain.deprecated.logic.notification

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ivy.base.legacy.SharedPrefs
import com.ivy.legacy.utils.timeNowLocal
import com.ivy.legacy.utils.toEpochSeconds
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Deprecated("Use FP style, look into `domain.fp` package")
class TransactionReminderLogic @Inject constructor(
    @ApplicationContext
    private val appContext: Context,
    private val sharedPrefs: SharedPrefs,
) {
    companion object {
        private const val UNIQUE_WORK_NAME_V1 = "transaction_reminder_work"
        private const val UNIQUE_WORK_NAME_V2 = "transaction_reminder_work_v2"
        private const val UNIQUE_WORK_NAME_TEST = "transaction_reminder_work_test"
        private const val PLANNED_DUE_WORK_NAME = "planned_payment_due_work"
        private const val PLANNED_DUE_NOW_WORK_NAME = "planned_payment_due_now"
        private const val TRANSACTION_REMINDER_HOUR = 20
        private const val PLANNED_DUE_REMINDER_HOUR = 9
        private const val HOURS_IN_DAY = 24L
    }

    fun testNow() {
        val workBuilder = PeriodicWorkRequestBuilder<TransactionReminderWorker>(5, TimeUnit.MINUTES)

        WorkManager
            .getInstance(appContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME_TEST,
                ExistingPeriodicWorkPolicy.REPLACE,
                workBuilder.build()
            )
    }

    /** Schedules both daily reminders (no-op while notifications are turned off). */
    fun scheduleReminder() {
        if (!fetchShowNotifications()) {
            return
        }

        enqueueDaily<TransactionReminderWorker>(UNIQUE_WORK_NAME_V2, TRANSACTION_REMINDER_HOUR)
        enqueueDaily<PlannedPaymentReminderWorker>(PLANNED_DUE_WORK_NAME, PLANNED_DUE_REMINDER_HOUR)
    }

    /**
     * Runs the planned-payment check right away, e.g. after a rule is saved, so a payment that
     * is due today is announced immediately instead of at tomorrow's scheduled run.
     */
    fun checkPlannedPaymentsNow() {
        if (!fetchShowNotifications()) {
            return
        }
        WorkManager
            .getInstance(appContext)
            .enqueueUniqueWork(
                PLANNED_DUE_NOW_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PlannedPaymentReminderWorker>().build()
            )
    }

    private inline fun <reified W : androidx.work.ListenableWorker> enqueueDaily(
        uniqueName: String,
        hourOfDay: Int,
    ) {
        val workBuilder = PeriodicWorkRequestBuilder<W>(HOURS_IN_DAY, TimeUnit.HOURS)
        val initialDelaySeconds = secondsUntil(hourOfDay)
        if (initialDelaySeconds > 0) {
            workBuilder.setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
        }

        WorkManager
            .getInstance(appContext)
            .enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.KEEP,
                workBuilder.build()
            )
    }

    /** Seconds from now until the next [hourOfDay]:00 local time (today if still ahead). */
    private fun secondsUntil(hourOfDay: Int): Long {
        val now: LocalDateTime = timeNowLocal()
        val target = now.withHour(hourOfDay).withMinute(0).withSecond(0)
        return if (target.isAfter(now)) {
            target.toEpochSeconds() - now.toEpochSeconds()
        } else {
            target.plusDays(1).toEpochSeconds() - now.toEpochSeconds()
        }
    }

    private fun fetchShowNotifications(): Boolean =
        sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true)
}
