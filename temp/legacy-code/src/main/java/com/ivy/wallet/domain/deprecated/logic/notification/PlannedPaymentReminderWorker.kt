package com.ivy.wallet.domain.deprecated.logic.notification

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.stringRes
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.TransactionDao
import com.ivy.data.model.isSecondaryCreditCurrency
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.AppStarter
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.CreditCardData
import com.ivy.legacy.data.model.CreditDueStatus
import com.ivy.legacy.data.model.dueStatus
import com.ivy.legacy.utils.atEndOfDay
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.ivyMinTime
import com.ivy.ui.R
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import com.ivy.wallet.domain.action.account.CalcAccBalanceAct
import com.ivy.wallet.domain.pure.transaction.plannedDueSummary
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Once a day (and right after a planned payment is saved) tells the user how many planned
 * payments are due today or overdue. Tapping the notification opens the Planned payments screen.
 */
@HiltWorker
class PlannedPaymentReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val notificationService: NotificationService,
    private val sharedPrefs: SharedPrefs,
    private val appStarter: AppStarter,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val currencyRepository: CurrencyRepository,
    private val accountRepository: AccountRepository,
    private val accountBalance: CalcAccBalanceAct,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!remindersEnabled()) return@withContext Result.success()

        val today = timeProvider.localDateNow()
        notifyCardPaymentsDue(today)
        notifyPlannedPaymentsDue(today)
        Result.success()
    }

    private suspend fun notifyPlannedPaymentsDue(today: LocalDate) {
        val dueTransactions = with(timeConverter) {
            transactionDao.findAllDueToBetween(
                startDate = ivyMinTime(),
                endDate = today.atEndOfDay().toUTC(),
            )
        }
        val summary = plannedDueSummary(
            dueTransactions = dueTransactions,
            today = today,
            toLocalDate = { instant -> with(timeConverter) { instant.toLocalDate() } },
        )
        if (summary.isEmpty) return

        val currency = currencyRepository.getBaseCurrency().code
        val parts = buildList {
            if (summary.dueToday > 0) {
                add(stringRes(R.string.planned_due_today, summary.dueToday.toString()))
            }
            if (summary.overdue > 0) {
                add(stringRes(R.string.planned_overdue_count, summary.overdue.toString()))
            }
            add("${summary.totalAmount.format(currency)} $currency")
        }

        val notification = notificationService
            .defaultIvyNotification(
                channel = IvyNotificationChannel.PLANNED_PAYMENT_DUE,
                priority = NotificationCompat.PRIORITY_DEFAULT,
            )
            .setContentTitle(stringRes(R.string.planned_due_notification_title))
            .setContentText(parts.joinToString(separator = " · "))
            .setContentIntent(
                PendingIntent.getActivity(
                    applicationContext,
                    NOTIFICATION_ID,
                    appStarter.openPlannedPaymentsIntent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

        notificationService.showNotification(notification, NOTIFICATION_ID)
    }

    /** One notification per credit card whose statement payment is due within the next few days. */
    private suspend fun notifyCardPaymentsDue(today: LocalDate) {
        val cards = accountRepository.findAll()
            .filter { it.creditLimit != null && it.creditDueDay != null && !it.isSecondaryCreditCurrency }
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        for (account in cards) {
            val balance = accountBalance(CalcAccBalanceAct.Input(account)).balance.toDouble()
            val card = CreditCardData(
                primary = AccountData(
                    account = account,
                    balance = balance,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 0.0,
                    monthlyIncome = 0.0,
                ),
            )
            val status = (card.dueStatus(today) as? CreditDueStatus.DueIn)
                ?.takeIf { it.days <= CARD_DUE_WARNING_DAYS }
                ?: continue
            val owed = (-balance).coerceAtLeast(0.0)
            val currency = account.asset.code
            val notification = notificationService
                .defaultIvyNotification(
                    channel = IvyNotificationChannel.PLANNED_PAYMENT_DUE,
                    priority = NotificationCompat.PRIORITY_DEFAULT,
                )
                .setContentTitle(stringRes(R.string.credit_due_notification_title))
                .setContentText(
                    stringRes(
                        R.string.credit_due_notification_text,
                        account.name.value,
                        "${owed.format(currency)} $currency",
                        status.dueDate.format(formatter),
                    ),
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        account.id.value.hashCode(),
                        appStarter.getRootIntent(),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            notificationService.showNotification(notification, account.id.value.hashCode())
        }
    }

    private fun remindersEnabled(): Boolean =
        sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true) &&
            sharedPrefs.getBoolean(SharedPrefs.PLANNED_PAYMENT_REMINDERS, true)

    companion object {
        const val NOTIFICATION_ID = 2
        private const val CARD_DUE_WARNING_DAYS = 3L
    }
}
