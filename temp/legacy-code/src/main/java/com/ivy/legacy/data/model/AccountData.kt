package com.ivy.legacy.data.model

import androidx.compose.runtime.Immutable
import com.ivy.data.model.Account
import com.ivy.wallet.domain.data.Reorderable

@Immutable
@Suppress("DataClassDefaultValues")
data class AccountData(
    val account: Account,
    val balance: Double,
    val balanceBaseCurrency: Double?,
    val monthlyExpenses: Double,
    val monthlyIncome: Double,
    /**
     * Credit cards only: what is still unpaid from the latest closed statement (0 when it's been
     * paid off). Null when unknown or the card has no billing cycle.
     */
    val statementOwed: Double? = null,
) : Reorderable {
    override fun getItemOrderNum() = account.orderNum

    override fun withNewOrderNum(newOrderNum: Double) = this.copy(
        account = account.copy(
            orderNum = newOrderNum
        )
    )
}
