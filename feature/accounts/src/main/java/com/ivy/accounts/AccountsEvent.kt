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

    data class SaveCreditCard(val input: com.ivy.legacy.data.model.CreditCardInput) : AccountsEvent
    data class PayCreditCard(val input: com.ivy.legacy.data.model.CreditCardPaymentInput) : AccountsEvent

    @Suppress("DataClassTypedIDs")
    data class ResetCreditCard(val accountId: AccountId, val expectedOwed: Double) : AccountsEvent
    data object ClearCreditError : AccountsEvent
}
