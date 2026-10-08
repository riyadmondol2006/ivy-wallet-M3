package com.ivy.home

import com.ivy.base.legacy.Transaction
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.data.model.TimePeriod

sealed interface HomeEvent {
    data class SetUpcomingExpanded(val expanded: Boolean) : HomeEvent
    data class SetOverdueExpanded(val expanded: Boolean) : HomeEvent

    data object BalanceClick : HomeEvent
    data object HiddenBalanceClick : HomeEvent
    data object HiddenIncomeClick : HomeEvent
    data class SetExpanded(val expanded: Boolean) : HomeEvent

    data object SwitchTheme : HomeEvent

    data class SetBuffer(val buffer: Double) : HomeEvent

    data class SetCurrency(val currency: String) : HomeEvent

    data class SetPeriod(val period: TimePeriod) : HomeEvent

    data class PayOrGetPlanned(val transaction: Transaction) : HomeEvent
    data class SkipPlanned(val transaction: Transaction) : HomeEvent
    data class SkipAllPlanned(val transactions: List<Transaction>) : HomeEvent

    data class DismissCustomerJourneyCard(val card: CustomerJourneyCardModel) : HomeEvent

    data object SelectNextMonth : HomeEvent
    data object SelectPreviousMonth : HomeEvent

    data object ManualSync : HomeEvent

    /** Overwrite the cloud with this device's data after a refused manual sync. */
    data object ForceSync : HomeEvent

    /** Pull the newer cloud revision into this device. */
    data object ConfirmRemoteSync : HomeEvent

    /** "Not now": remember the remote revision so the prompt does not nag again for it. */
    data object DismissRemoteSync : HomeEvent

    /** Close the prompt without a decision; it comes back on the next Home start. */
    data object HideRemoteSync : HomeEvent

    data object DismissSyncMessage : HomeEvent

    /** Hide the "database unreachable" card until the next check. */
    data object DismissSyncError : HomeEvent

    /** Open the cloud sync settings from the "database unreachable" card. */
    data object OpenCloudSync : HomeEvent
}
