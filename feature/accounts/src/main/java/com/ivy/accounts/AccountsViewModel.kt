package com.ivy.accounts

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.repository.AccountRepository
import com.ivy.domain.features.Features
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.data.model.toCloseTimeRange
import com.ivy.legacy.domain.deprecated.logic.AccountCreator
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.ioThread
import com.ivy.ui.ComposeViewModel
import com.ivy.ui.R
import com.ivy.wallet.domain.action.settings.BaseCurrencyAct
import com.ivy.wallet.domain.action.viewmodel.account.AccountDataAct
import com.ivy.wallet.domain.action.wallet.CalcWalletBalanceAct
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ivy.legacy.datamodel.Account as LegacyAccount

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
    private val accountCreator: AccountCreator,
    private val dataObserver: DataObserver,
    private val features: Features,
    private val timeProvider: TimeProvider,
    private val timeConverter: TimeConverter,
    private val creditCardService: com.ivy.legacy.domain.CreditCardService,
) : ComposeViewModel<AccountsState, AccountsEvent>() {
    private var baseCurrency by mutableStateOf("")
    private var accountsData by mutableStateOf(listOf<AccountData>())
    private var totalBalanceWithExcluded by mutableStateOf("")
    private var totalBalanceWithExcludedText by mutableStateOf("")
    private var totalBalanceWithoutExcluded by mutableStateOf("")
    private var totalBalanceWithoutExcludedText by mutableStateOf("")
    private var reorderVisible by mutableStateOf(false)
    private var creditBusy by mutableStateOf(false)
    private var creditError by mutableStateOf<String?>(null)
    private var creditSuccess by mutableIntStateOf(0)

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
            creditCardsEnabled = getCreditCardsEnabled(),
            creditOperationInProgress = creditBusy,
            creditOperationError = creditError,
            creditOperationSuccess = creditSuccess,
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

    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    override fun onEvent(event: AccountsEvent) {
        if (event is AccountsEvent.ClearCreditError) {
            creditError = null
            return
        }
        val creditOperation = event is AccountsEvent.SaveCreditCard ||
            event is AccountsEvent.PayCreditCard ||
            event is AccountsEvent.ResetCreditCard
        if (creditOperation) {
            if (creditBusy) return
            creditBusy = true
            creditError = null
            viewModelScope.launch {
                var saved = false
                try {
                    ioThread {
                        when (event) {
                            is AccountsEvent.SaveCreditCard -> creditCardService.save(event.input)
                            is AccountsEvent.PayCreditCard -> creditCardService.pay(
                                event.input,
                                context.getString(R.string.credit_card_payment)
                            )
                            is AccountsEvent.ResetCreditCard -> creditCardService.reset(
                                event.accountId,
                                event.expectedOwed,
                                context.getString(R.string.credit_balance_adjustment)
                            )
                            else -> Unit
                        }
                    }
                    // Close the form once persisted, even if refreshing totals subsequently fails.
                    // Otherwise retrying a new-card form could create a duplicate card.
                    saved = true
                    creditSuccess++
                    startInternally()
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    creditError = context.getString(
                        if (saved) R.string.credit_refresh_failed else R.string.credit_operation_failed
                    )
                    if (!saved) {
                        try {
                            startInternally()
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Keep the original operation error visible and allow a later retry.
                        }
                    }
                } finally {
                    creditBusy = false
                }
            }
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            when (event) {
                is AccountsEvent.OnReorder -> reorder(event.reorderedList)
                is AccountsEvent.OnReorderModalVisible -> reorderModalVisible(event.reorderVisible)
                is AccountsEvent.OnCreateAccount -> createAccount(event.data)
                is AccountsEvent.OnEditAccount -> editAccount(event.account, event.newBalance)
                else -> Unit
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
