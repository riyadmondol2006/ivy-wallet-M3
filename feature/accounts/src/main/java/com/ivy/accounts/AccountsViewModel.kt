package com.ivy.accounts

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Transaction
import com.ivy.base.model.TransactionType
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.model.AccountId
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.domain.features.Features
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.toCloseTimeRange
import com.ivy.legacy.datamodel.temp.toDomain
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.WalletAccountLogic
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import com.ivy.legacy.datamodel.Account as LegacyAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@SuppressLint("StaticFieldLeak")
@HiltViewModel
class AccountsViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val ivyContext: IvyWalletCtx,
    private val sharedPrefs: SharedPrefs,
    private val calcWalletBalanceAct: CalcWalletBalanceAct,
    private val baseCurrencyAct: BaseCurrencyAct,
    private val accountDataAct: AccountDataAct,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionMapper: TransactionMapper,
    private val accountCreator: AccountCreator,
    private val walletAccountLogic: WalletAccountLogic,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
) : ComposeViewModel<AccountsState, AccountsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var totalBalanceWithExcluded by mutableStateOf("")
    private var totalBalanceWithExcludedText by mutableStateOf("")
    private var totalBalanceWithoutExcluded by mutableStateOf("")
    private var totalBalanceWithoutExcludedText by mutableStateOf("")
    private var reorderVisible by mutableStateOf(false)

    init {
        viewModelScope.launch {
            dataObserver.writeEvents.collectLatest { event ->
                when (event) {
                    is DataWriteEvent.AccountChange -> {
                        onStart()
                    }

                    else -> {
                        // do nothing
                    }
                }
            }
        }
    }

    @Composable
    override fun uiState(): AccountsState {
        LaunchedEffect(Unit) {
            onStart()
        }

        return AccountsState(
            baseCurrency = getBaseCurrency(),
            accountsData = getAccountsData(),
            totalBalanceWithExcluded = getTotalBalanceWithExcluded(),
            totalBalanceWithExcludedText = getTotalBalanceWithExcludedText(),
            totalBalanceWithoutExcluded = getTotalBalanceWithoutExcluded(),
            totalBalanceWithoutExcludedText = getTotalBalanceWithoutExcludedText(),
            reorderVisible = getReorderVisible(),
            compactAccountsModeEnabled = getCompactAccountsMode(),
            hideTotalBalance = getHideTotalBalance(),
            creditCardsEnabled = getCreditCardsEnabled()
        )
    }

    @Composable
    private fun getCreditCardsEnabled(): Boolean {
        return features.creditCardsEnabled.asEnabledState()
    }

    @Composable
    private fun getHideTotalBalance(): Boolean {
        return features.hideTotalBalance.asEnabledState()
    }

    @Composable
    private fun getBaseCurrency(): String {
        return baseCurrency
    }

    @Composable
    private fun getAccountsData(): ImmutableList<AccountData> {
        return accountsData.toImmutableList()
    }

    @Composable
    private fun getTotalBalanceWithExcluded(): String {
        return totalBalanceWithExcluded
    }

    @Composable
    private fun getTotalBalanceWithExcludedText(): String {
        return totalBalanceWithExcludedText
    }

    @Composable
    private fun getTotalBalanceWithoutExcluded(): String {
        return totalBalanceWithoutExcluded
    }

    @Composable
    private fun getTotalBalanceWithoutExcludedText(): String {
        return totalBalanceWithoutExcludedText
    }

    @Composable
    private fun getReorderVisible(): Boolean {
        return reorderVisible
    }

    @Composable
    private fun getCompactAccountsMode(): Boolean {
        return features.compactAccountsMode.asEnabledState()
    }

    override fun onEvent(event: AccountsEvent) {
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is AccountsEvent.OnReorder -> reorder(event.reorderedList)
                is AccountsEvent.OnReorderModalVisible -> reorderModalVisible(event.reorderVisible)
                is AccountsEvent.OnCreateAccount -> createAccount(event.data)
                is AccountsEvent.OnEditAccount -> editAccount(event.account, event.newBalance)
                is AccountsEvent.MarkPaidFromAccount ->
                    markPaidFromAccount(event.card, event.fromAccountId)
                is AccountsEvent.MarkPaidReset -> markPaidReset(event.card)
            }
        }
    }

    private suspend fun createAccount(data: com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData) {
        accountCreator.createAccount(data) {
            startInternally()
        }
    }

    private suspend fun editAccount(account: LegacyAccount, newBalance: Double) {
        accountCreator.editAccount(account, newBalance) {
            startInternally()
        }
    }

    /**
     * "Just reset" — bring the credit card's owed balance back to 0 by booking a balancing
     * income on the card itself. No money leaves any bank account.
     */
    private suspend fun markPaidReset(card: AccountData) {
        val owed = -minOf(0.0, card.balance)
        if (owed <= 0.0) return
        walletAccountLogic.adjustBalance(
            account = card.toLegacyAccount(),
            actualBalance = card.balance,
            newBalance = 0.0,
            adjustTransactionTitle = context.getString(R.string.credit_card_payment)
        )
        startInternally()
    }

    /**
     * "Pay from account" — book a transfer of the owed amount from a real account into the card,
     * settling the card (balance back to 0, limit restored) and reducing the paying account.
     */
    private suspend fun markPaidFromAccount(card: AccountData, fromAccountId: AccountId) {
        val owed = -minOf(0.0, card.balance)
        if (owed <= 0.0) return
        ioThread {
            Transaction(
                type = TransactionType.TRANSFER,
                accountId = fromAccountId.value,
                toAccountId = card.account.id.value,
                amount = owed.toBigDecimal(),
                toAmount = owed.toBigDecimal(),
                title = context.getString(R.string.credit_card_payment),
                dateTime = timeProvider.utcNow()
            ).toDomain(transactionMapper)?.let {
                transactionRepository.save(it)
            }
        }
        startInternally()
    }

    private fun AccountData.toLegacyAccount(): LegacyAccount = LegacyAccount(
        name = account.name.value,
        currency = account.asset.code,
        color = account.color.value,
        icon = account.icon?.id,
        orderNum = account.orderNum,
        includeInBalance = account.includeInBalance,
        creditLimit = account.creditLimit,
        id = account.id.value
    )

    private suspend fun reorder(newOrder: List<AccountData>) {
        ioThread {
            newOrder.mapIndexed { index, accountData ->
                accountRepository.save(accountData.account.copy(orderNum = index.toDouble()))
            }
        }

        startInternally()
    }

    private fun onStart() {
        viewModelScope.launch(Dispatchers.Default) {
            startInternally()
        }
    }

    private suspend fun startInternally() {
        val period = com.ivy.legacy.data.model.TimePeriod.currentMonth(
            startDayOfMonth = ivyContext.startDayOfMonth
        ) // this must be monthly
        val range = period.toRange(ivyContext.startDayOfMonth, timeConverter, timeProvider)

        val baseCurrencyCode = baseCurrencyAct(Unit)
        val accounts = accountRepository.findAll().toImmutableList()

        val includeTransfersInCalc =
            sharedPrefs.getBoolean(SharedPrefs.TRANSFERS_AS_INCOME_EXPENSE, false)

        val accountsDataList = accountDataAct(
            AccountDataAct.Input(
                accounts = accounts,
                range = range.toCloseTimeRange(),
                baseCurrency = baseCurrencyCode,
                includeTransfersInCalc = includeTransfersInCalc
            )
        )

        val totalBalanceWithExcludedAccounts = calcWalletBalanceAct(
            CalcWalletBalanceAct.Input(
                baseCurrency = baseCurrencyCode,
                withExcluded = true
            )
        ).toDouble()

        val totalBalanceWithoutExcludedAccounts = calcWalletBalanceAct(
            CalcWalletBalanceAct.Input(
                baseCurrency = baseCurrencyCode
            )
        ).toDouble()

        baseCurrency = baseCurrencyCode
        accountsData = accountsDataList
        totalBalanceWithExcluded = totalBalanceWithExcludedAccounts.toString()
        totalBalanceWithExcludedText = context.getString(
            R.string.total,
            baseCurrencyCode,
            totalBalanceWithExcludedAccounts.format(
                baseCurrencyCode
            )
        )
        totalBalanceWithoutExcluded = totalBalanceWithoutExcludedAccounts.toString()
        totalBalanceWithoutExcludedText = context.getString(
            R.string.total_exclusive,
            baseCurrencyCode,
            totalBalanceWithoutExcludedAccounts.format(
                baseCurrencyCode
            )
        )
    }

    private fun reorderModalVisible(visible: Boolean) {
        reorderVisible = visible
    }
}
