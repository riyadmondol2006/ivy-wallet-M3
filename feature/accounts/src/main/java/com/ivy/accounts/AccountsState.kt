package com.ivy.accounts

import androidx.compose.runtime.Immutable
import com.ivy.legacy.data.model.AccountData
import kotlinx.collections.immutable.ImmutableList

@Immutable
@Suppress("DataClassDefaultValues")
data class AccountsState(
    val baseCurrency: String,
    val accountsData: ImmutableList<AccountData>,
    val totalBalanceWithExcluded: String,
    val totalBalanceWithExcludedText: String,
    val totalBalanceWithoutExcluded: String,
    val totalBalanceWithoutExcludedText: String,
    val reorderVisible: Boolean,
    val compactAccountsModeEnabled: Boolean,
    val hideTotalBalance: Boolean,
    val creditCardsEnabled: Boolean = false,
)
