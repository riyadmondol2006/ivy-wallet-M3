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
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.domain.AppStarter
import com.ivy.legacy.data.model.CreditDueStatus
import com.ivy.legacy.data.model.dueStatus
import com.ivy.legacy.data.model.groupCreditCards
import com.ivy.legacy.utils.atEndOfDay
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.ivyMinTime
import com.ivy.ui.R
import com.ivy.wallet.android.notification.IvyNotificationChannel
import com.ivy.wallet.android.notification.NotificationService
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.transaction.plannedDueSummary
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

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
    private val accountDataAct: AccountDataAct,
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

        val parts = buildList {
            if (summary.dueToday > 0) {
                add(stringRes(R.string.planned_due_today, summary.dueToday.toString()))
            }
            if (summary.overdue > 0) {
                add(stringRes(R.string.planned_overdue_count, summary.overdue.toString()))
            }
            dueExpensesText(summary.expensesByAccount)?.let(::add)
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
        val cardAccounts = accountRepository.findAll().filter { it.creditLimit != null }
        if (cardAccounts.isEmpty()) return
        val now = timeProvider.utcNow()
        // Grouping keeps a dual-currency card's second ledger with its main card, so both currencies
        // decide whether a payment is due and both appear in the amount.
        val cards = groupCreditCards(
            accountDataAct(
                AccountDataAct.Input(
                    accounts = cardAccounts.toImmutableList(),
                    baseCurrency = currencyRepository.getBaseCurrency().code,
                    range = ClosedTimeRange(from = now, to = now),
                )
            )
        )
        val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        for (card in cards) {
            val status = (card.dueStatus(today) as? CreditDueStatus.DueIn)
                ?.takeIf { it.days <= CARD_DUE_WARNING_DAYS }
                ?: continue
            val owedText = card.accounts
                .mapNotNull { ledger ->
                    val owed = ledger.statementOwed ?: (-ledger.balance).coerceAtLeast(0.0)
                    val currency = ledger.account.asset.code
                    owed.takeIf { it > 0.0 }?.let { "${it.format(currency)} $currency" }
                }
                .joinToString(separator = " + ")
            val account = card.primary.account
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
                        owedText,
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

    /** "120.00 USD + 5,000.00 BDT": due expenses per currency, never added across currencies. */
    private suspend fun dueExpensesText(expensesByAccount: Map<UUID, Double>): String? {
        if (expensesByAccount.isEmpty()) return null
        val baseCurrency = currencyRepository.getBaseCurrency().code
        val currencyByAccount = accountRepository.findAll().associate { it.id.value to it.asset.code }
        return expensesByAccount.entries
            .groupBy { (accountId, _) -> currencyByAccount[accountId] ?: baseCurrency }
            .map { (currency, entries) -> currency to entries.sumOf { it.value } }
            .filter { (_, amount) -> amount > 0.0 }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " + ") { (currency, amount) -> "${amount.format(currency)} $currency" }
    }

    private fun remindersEnabled(): Boolean =
        sharedPrefs.getBoolean(SharedPrefs.SHOW_NOTIFICATIONS, true) &&
            sharedPrefs.getBoolean(SharedPrefs.PLANNED_PAYMENT_REMINDERS, true)

    companion object {
        const val NOTIFICATION_ID = 2
        private const val CARD_DUE_WARNING_DAYS = 3L
    }
}
