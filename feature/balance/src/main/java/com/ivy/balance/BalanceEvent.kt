package com.ivy.balance

import com.ivy.legacy.data.model.TimePeriod

sealed interface BalanceEvent {
    data class OnSetPeriod(val timePeriod: TimePeriod) : BalanceEvent
    data object OnPreviousMonth : BalanceEvent
    data object OnNextMonth : BalanceEvent
    data class OnSetHistoryRange(val range: BalanceHistoryRange) : BalanceEvent
    data class OnSelectHistoryPoint(val index: Int?) : BalanceEvent
}
