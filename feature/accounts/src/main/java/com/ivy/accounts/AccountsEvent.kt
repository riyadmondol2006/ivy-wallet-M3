package com.ivy.accounts

import com.ivy.data.model.AccountId
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent
    data class OnCreateAccount(val data: CreateAccountData) : AccountsEvent
    data class OnEditAccount(val account: Account, val newBalance: Double) : AccountsEvent

    @Suppress("DataClassTypedIDs")
    data class MarkPaidFromAccount(val card: AccountData, val fromAccountId: AccountId) :
        AccountsEvent
    data class MarkPaidReset(val card: AccountData) : AccountsEvent
}
