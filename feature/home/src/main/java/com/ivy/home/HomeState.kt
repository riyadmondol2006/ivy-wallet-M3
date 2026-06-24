package com.ivy.home

import androidx.compose.runtime.Immutable
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import kotlinx.collections.immutable.ImmutableList
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
)

@Immutable
data class CreditCardsSummary(
    val cardCount: Int,
    val totalOwed: Double,
    val totalLimit: Double,
    val totalLimitLeft: Double,
) {
    companion object {
        val None = CreditCardsSummary(
            cardCount = 0,
            totalOwed = 0.0,
            totalLimit = 0.0,
            totalLimitLeft = 0.0,
        )
    }
}
