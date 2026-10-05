package com.ivy.home

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.ui.sync.SyncMessage
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Immutable
@Suppress("DataClassDefaultValues")
data class HomeState(
    val theme: Theme,
    val name: String,

    val period: TimePeriod,
    val baseData: AppBaseData,

    val history: ImmutableList<TransactionHistoryItem>,
    val stats: IncomeExpensePair,

    val balance: BigDecimal,

    val buffer: BufferInfo,

    val upcoming: LegacyDueSection,
    val overdue: LegacyDueSection,

    val customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    val hideBalance: Boolean,
    val hideIncome: Boolean,
    val expanded: Boolean,
    val shouldShowAccountSpecificColorInTransactions: Boolean,
    val creditCardsEnabled: Boolean = false,
    val creditSummary: CreditCardsSummary = CreditCardsSummary.None,
    /** Show the minimal manual-sync button in the header (MANUAL mode + configured). */
    val manualSyncVisible: Boolean = false,
    /** A cloud sync (push or pull) is currently running. */
    val syncing: Boolean = false,
    /** When > 0, prompt to pull newer cloud changes from another device (the remote updatedAt). */
    val remoteSyncPromptAtMillis: Long = 0L,
    /**
     * The prompt was raised because a manual "Sync now" was refused: besides pulling, the user may
     * choose to overwrite the cloud with this device's data.
     */
    val remoteSyncConflict: Boolean = false,
    /** One-shot result of the last sync action, shown as a toast and then dismissed. */
    val syncMessage: SyncMessage? = null,
)

@Immutable
data class CreditCardsSummary(
    val cardCount: Int,
    val currencies: ImmutableList<com.ivy.legacy.data.model.CreditCurrencyStats>,
    /** Earliest card payment date among cards that still owe money, if a billing cycle is set. */
    val nearestDueDate: java.time.LocalDate?,
) {
    companion object {
        val None = CreditCardsSummary(
            cardCount = 0,
            currencies = persistentListOf(),
            nearestDueDate = null,
        )
    }
}
