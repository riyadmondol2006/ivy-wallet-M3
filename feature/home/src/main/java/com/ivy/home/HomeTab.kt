package com.ivy.home

import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.Transaction
import com.ivy.base.legacy.TransactionHistoryItem
import com.ivy.base.legacy.stringRes
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.system.IvySpacing
import com.ivy.frp.forward
import com.ivy.frp.then2
import com.ivy.home.Constants.SWIPE_HORIZONTAL_THRESHOLD
import com.ivy.home.customerjourney.CustomerJourney
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.AppBaseData
import com.ivy.legacy.data.BufferInfo
import com.ivy.legacy.data.LegacyDueSection
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.data.model.Month
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.transaction.transactions
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.CloudSyncScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.ui.sync.text
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.pure.data.IncomeExpensePair
import com.ivy.wallet.ui.theme.modal.BufferModal
import com.ivy.wallet.ui.theme.modal.BufferModalData
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModal
import com.ivy.wallet.ui.theme.modal.ChoosePeriodModalData
import com.ivy.wallet.ui.theme.modal.CurrencyModal
import com.ivy.wallet.ui.theme.modal.DeleteModal
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.HomeTab() {
    val viewModel: HomeViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    HomeUi(uiState, viewModel::onEvent)
}

@Suppress("LongMethod")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun BoxWithConstraintsScope.HomeUi(
    uiState: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()

    var bufferModalData: BufferModalData? by remember { mutableStateOf(null) }
    var currencyModalVisible by remember { mutableStateOf(false) }
    var choosePeriodModal: ChoosePeriodModalData? by remember {
        mutableStateOf(null)
    }
    var moreMenuExpanded by remember { mutableStateOf(ivyContext.moreMenuExpanded) }
    var skipAllModalVisible by remember { mutableStateOf(false) }
    val setMoreMenuExpanded = { expanded: Boolean ->
        moreMenuExpanded = expanded
        ivyContext.setMoreMenuExpanded(expanded)
    }

    val baseCurrency = uiState.baseData.baseCurrency

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalSwipeListener(
                sensitivity = Constants.SWIPE_DOWN_THRESHOLD_OPEN_MORE_MENU,
                state = rememberSwipeListenerState(),
                onSwipeDown = {
                    setMoreMenuExpanded(true)
                }
            )
            .horizontalSwipeListener(
                sensitivity = SWIPE_HORIZONTAL_THRESHOLD,
                state = rememberSwipeListenerState(),
                // Accounts is the next tab, so only a swipe towards it (left) changes tabs; this
                // matches the forward slide transition in MainScreen.
                onSwipeLeft = {
                    ivyContext.selectMainTab(MainTab.ACCOUNTS)
                },
            )
    ) {
        val listState = rememberScrollPositionListState(
            key = "home_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.transactionsListState
                ?.firstVisibleItemIndex ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.transactionsListState
                ?.firstVisibleItemScrollOffset ?: 0
        )

        HomeHeader(
            expanded = uiState.expanded,
            name = uiState.name,
            period = uiState.period,
            currency = baseCurrency,
            balance = uiState.balance.toDouble(),
            hideBalance = uiState.hideBalance,

            onShowMonthModal = {
                choosePeriodModal = ChoosePeriodModalData(
                    period = uiState.period
                )
            },
            onBalanceClick = {
                onEvent(HomeEvent.BalanceClick)
            },
            onHiddenBalanceClick = {
                onEvent(HomeEvent.HiddenBalanceClick)
            },
            onSelectNextMonth = {
                onEvent(HomeEvent.SelectNextMonth)
            },
            onSelectPreviousMonth = {
                onEvent(HomeEvent.SelectPreviousMonth)
            },
            manualSyncVisible = uiState.manualSyncVisible,
            syncing = uiState.syncing,
            onManualSync = {
                onEvent(HomeEvent.ManualSync)
            },
            onOpenMoreMenu = {
                setMoreMenuExpanded(true)
            }
        )

        HomeLazyColumn(
            hideBalance = uiState.hideBalance,
            syncError = uiState.syncError,
            onDismissSyncError = { onEvent(HomeEvent.DismissSyncError) },
            onOpenCloudSync = { onEvent(HomeEvent.OpenCloudSync) },
            hideIncome = uiState.hideIncome,
            onSetExpand = {
                onEvent(HomeEvent.SetExpanded(it))
            },
            balance = uiState.balance,
            onBalanceClick = {
                onEvent(HomeEvent.BalanceClick)
            },
            onHiddenBalanceClick = {
                onEvent(HomeEvent.HiddenBalanceClick)
            },
            onHiddenIncomeClick = {
                onEvent(HomeEvent.HiddenIncomeClick)
            },

            period = uiState.period,
            listState = listState,

            baseData = uiState.baseData,

            upcoming = uiState.upcoming,
            overdue = uiState.overdue,

            stats = uiState.stats,
            history = uiState.history,

            customerJourneyCards = uiState.customerJourneyCards,
            shouldShowAccountSpecificColorInTransactions = uiState.shouldShowAccountSpecificColorInTransactions,

            creditCardsEnabled = uiState.creditCardsEnabled,
            creditSummary = uiState.creditSummary,
            onCreditClick = {
                ivyContext.selectMainTab(MainTab.ACCOUNTS)
            },

            onPayOrGet = forward<Transaction>() then2 {
                HomeEvent.PayOrGetPlanned(it)
            } then2 onEvent,
            onDismiss = forward<CustomerJourneyCardModel>() then2 {
                HomeEvent.DismissCustomerJourneyCard(it)
            } then2 onEvent,
            onSkipTransaction = forward<Transaction>() then2 {
                HomeEvent.SkipPlanned(it)
            } then2 onEvent,
            setUpcomingExpanded = forward<Boolean>() then2 {
                HomeEvent.SetUpcomingExpanded(it)
            } then2 onEvent,
            setOverdueExpanded = forward<Boolean>() then2 {
                HomeEvent.SetOverdueExpanded(it)
            } then2 onEvent,
            onSkipAllTransactions = {
                skipAllModalVisible = true
            }
        )
    }

    MoreMenu(
        expanded = moreMenuExpanded,
        theme = uiState.theme,
        balance = uiState.balance.toDouble(),
        currency = baseCurrency,
        buffer = uiState.buffer.amount.toDouble(),
        manualSyncVisible = uiState.manualSyncVisible,
        syncing = uiState.syncing,
        onSwitchTheme = {
            onEvent(HomeEvent.SwitchTheme)
        },
        setExpanded = setMoreMenuExpanded,
        onBufferClick = {
            bufferModalData = BufferModalData(
                balance = uiState.balance.toDouble(),
                currency = baseCurrency,
                buffer = uiState.buffer.amount.toDouble()
            )
        },
        onManualSync = {
            onEvent(HomeEvent.ManualSync)
        },
        onSetUpSync = {
            nav.navigateTo(CloudSyncScreen())
        }
    )

    BufferModal(
        modal = bufferModalData,
        dismiss = {
            bufferModalData = null
        },
        onBufferChanged = forward<Double>() then2 {
            HomeEvent.SetBuffer(it)
        } then2 onEvent
    )

    CurrencyModal(
        title = stringResource(R.string.set_currency),
        initialCurrency = IvyCurrency.fromCode(baseCurrency),
        visible = currencyModalVisible,
        dismiss = {
            currencyModalVisible = false
        },
        onSetCurrency = forward<String>() then2 {
            HomeEvent.SetCurrency(it)
        } then2 onEvent
    )

    ChoosePeriodModal(
        modal = choosePeriodModal,
        dismiss = {
            choosePeriodModal = null
        },
        onPeriodSelected = forward<TimePeriod>() then2 {
            HomeEvent.SetPeriod(it)
        } then2 onEvent
    )

    DeleteModal(
        visible = skipAllModalVisible,
        title = stringResource(R.string.confirm_skip_all),
        description = stringResource(R.string.confirm_skip_all_description),
        dismiss = {
            skipAllModalVisible = false
        }
    ) {
        onEvent(HomeEvent.SkipAllPlanned(uiState.overdue.trns))
        skipAllModalVisible = false
    }

    val context = LocalContext.current
    val syncMessageText = uiState.syncMessage?.text()
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(uiState.syncMessage) {
        if (syncMessageText != null) {
            Toast.makeText(context, syncMessageText, Toast.LENGTH_SHORT).show()
            currentOnEvent(HomeEvent.DismissSyncMessage)
        }
    }

    if (uiState.remoteSyncPromptAtMillis > 0L) {
        val updatedAt = remember(uiState.remoteSyncPromptAtMillis) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(uiState.remoteSyncPromptAtMillis))
        }
        AlertDialog(
            // Tapping outside or "Not now" hides the prompt; it returns on the next Home start
            // until the change is pulled or overwritten.
            onDismissRequest = { onEvent(HomeEvent.HideRemoteSync) },
            title = { Text(stringResource(R.string.cloud_sync_pull_prompt_title)) },
            text = {
                Text(
                    stringResource(
                        if (uiState.remoteSyncConflict) {
                            R.string.cloud_sync_conflict_desc
                        } else {
                            R.string.cloud_sync_pull_prompt_desc
                        },
                        updatedAt,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(HomeEvent.ConfirmRemoteSync) }) {
                    Text(
                        stringResource(
                            if (uiState.remoteSyncConflict) {
                                R.string.cloud_sync_merge
                            } else {
                                R.string.cloud_sync_pull
                            }
                        )
                    )
                }
            },
            dismissButton = {
                Row {
                    if (uiState.remoteSyncConflict) {
                        TextButton(onClick = { onEvent(HomeEvent.ForceSync) }) {
                            Text(
                                text = stringResource(R.string.cloud_sync_overwrite),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    TextButton(onClick = { onEvent(HomeEvent.DismissRemoteSync) }) {
                        Text(stringResource(R.string.cloud_sync_not_now))
                    }
                }
            },
        )
    }
}

/** Shown while the configured cloud database can't be reached, so sync never fails silently. */
@Composable
private fun CloudSyncErrorCard(
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.cloud_sync_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
            Text(
                text = stringResource(R.string.cloud_sync_error_desc, message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 12.dp),
            )
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.cloud_sync_error_open_settings))
            }
        }
    }
}

@Suppress("LongParameterList")
@ExperimentalAnimationApi
@Composable
fun HomeLazyColumn(
    hideBalance: Boolean,
    hideIncome: Boolean,
    onSetExpand: (Boolean) -> Unit,
    listState: LazyListState,
    period: TimePeriod,

    baseData: AppBaseData,
    shouldShowAccountSpecificColorInTransactions: Boolean,

    creditCardsEnabled: Boolean,
    creditSummary: CreditCardsSummary,
    onCreditClick: () -> Unit,

    upcoming: LegacyDueSection,
    overdue: LegacyDueSection,
    balance: BigDecimal,
    stats: IncomeExpensePair,
    history: ImmutableList<TransactionHistoryItem>,

    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,

    setUpcomingExpanded: (Boolean) -> Unit,
    setOverdueExpanded: (Boolean) -> Unit,

    onBalanceClick: () -> Unit,

    onPayOrGet: (Transaction) -> Unit,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
    onHiddenBalanceClick: () -> Unit,
    onHiddenIncomeClick: () -> Unit,
    onSkipTransaction: (Transaction) -> Unit,
    onSkipAllTransactions: (List<Transaction>) -> Unit,
    modifier: Modifier = Modifier,
    syncError: String? = null,
    onDismissSyncError: () -> Unit = {},
    onOpenCloudSync: () -> Unit = {},
) {
    val ivyContext = ivyWalletCtx()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                onSetExpand(
                    listState.firstVisibleItemIndex == 0 &&
                        listState.firstVisibleItemScrollOffset == 0
                )
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    val timeProvider = LocalTimeProvider.current
    val timeConverter = LocalTimeConverter.current
    val timeFormatter = LocalTimeFormatter.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .testTag("home_lazy_column"),
        state = listState,
        // Bottom inset clears the floating navigation bar and the FAB above it.
        contentPadding = PaddingValues(top = 8.dp, bottom = HomeListBottomInset),
    ) {
        item {
            CashFlowInfo(
                currency = baseData.baseCurrency,
                balance = balance.toDouble(),

                hideBalance = hideBalance,

                monthlyIncome = stats.income.toDouble(),
                monthlyExpenses = stats.expense.toDouble(),

                onBalanceClick = onBalanceClick,
                onHiddenBalanceClick = onHiddenBalanceClick,
                percentExpanded = 1f,
                hideIncome = hideIncome,
                onHiddenIncomeClick = onHiddenIncomeClick
            )
        }
        if (syncError != null) {
            item {
                Spacer(Modifier.height(16.dp))

                CloudSyncErrorCard(
                    message = syncError,
                    onOpenSettings = onOpenCloudSync,
                    onDismiss = onDismissSyncError,
                    modifier = Modifier
                        .padding(horizontal = IvySpacing.screenGutter)
                        .animateItem(),
                )
            }
        }
        if (creditCardsEnabled && creditSummary.cardCount > 0) {
            item {
                Spacer(Modifier.height(16.dp))

                CreditCardsSummaryCard(
                    summary = creditSummary,
                    onClick = onCreditClick,
                    modifier = Modifier.animateItem()
                )
            }
        }
        item {
            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = IvySpacing.screenGutter),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        item {
            CustomerJourney(
                customerJourneyCards = customerJourneyCards,
                onDismiss = onDismiss
            )
        }

        transactions(
            baseData = baseData,
            upcoming = upcoming,
            setUpcomingExpanded = setUpcomingExpanded,
            overdue = overdue,
            setOverdueExpanded = setOverdueExpanded,
            history = history,
            onPayOrGet = onPayOrGet,
            emptyStateTitle = stringRes(R.string.no_transactions),
            emptyStateText = stringRes(
                R.string.no_transactions_description,
                period.toDisplayLong(
                    startDateOfMonth = ivyContext.startDayOfMonth,
                    timeProvider = timeProvider,
                    timeConverter = timeConverter,
                    timeFormatter = timeFormatter,
                )
            ),
            shouldShowAccountSpecificColorInTransactions = shouldShowAccountSpecificColorInTransactions,
            dateDividerMarginTop = 24.dp,
            lastItemSpacer = 0.dp,
            onSkipTransaction = onSkipTransaction,
            onSkipAllTransactions = onSkipAllTransactions
        )
    }
}

/** 80dp navigation bar + 56dp FAB + a 24dp gap. */
private val HomeListBottomInset = 160.dp

@Suppress("MagicNumber")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Preview
@Composable
private fun BoxWithConstraintsScope.PreviewHomeTab(isDark: Boolean = false) {
    IvyPreview(isDark) {
        HomeUi(
            uiState = HomeState(
                theme = Theme.AUTO,
                name = "",
                baseData = AppBaseData(
                    baseCurrency = "BDT",
                    accounts = persistentListOf(),
                    categories = persistentListOf()
                ),
                balance = BigDecimal("14347.50"),
                buffer = BufferInfo(
                    amount = BigDecimal.ZERO,
                    bufferDiff = BigDecimal.ZERO,
                ),
                customerJourneyCards = persistentListOf(),
                history = persistentListOf(),
                stats = IncomeExpensePair(
                    income = BigDecimal("52000"),
                    expense = BigDecimal("37652.50"),
                ),
                upcoming = LegacyDueSection(
                    trns = persistentListOf(),
                    stats = IncomeExpensePair.zero(),
                    expanded = false,
                ),
                overdue = LegacyDueSection(
                    trns = persistentListOf(),
                    stats = IncomeExpensePair.zero(),
                    expanded = false,
                ),
                period = TimePeriod(month = Month.monthsList().first(), year = 2023),
                hideBalance = false,
                hideIncome = false,
                expanded = true,
                shouldShowAccountSpecificColorInTransactions = false
            ),
            onEvent = {}
        )
    }
}

/** For screenshot testing */
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeUiTest(isDark: Boolean) {
    val theme = when (isDark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    IvyWalletPreview(theme) {
        PreviewHomeTab(isDark)
    }
}
