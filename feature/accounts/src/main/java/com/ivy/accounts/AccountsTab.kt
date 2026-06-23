package com.ivy.accounts

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountData
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.navigation.TransactionsScreen
import com.ivy.navigation.navigation
import com.ivy.navigation.screenScopedViewModel
import com.ivy.ui.R
import com.ivy.ui.rememberScrollPositionListState
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.ReorderButton
import com.ivy.wallet.ui.theme.components.ReorderModalSingleType
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import com.ivy.legacy.datamodel.Account as LegacyAccount

@Composable
fun BoxWithConstraintsScope.AccountsTab() {
    val viewModel: AccountsViewModel = screenScopedViewModel()
    val uiState = viewModel.uiState()

    UI(
        state = uiState,
        onEvent = viewModel::onEvent
    )
}

@Suppress("LongMethod")
@Composable
private fun BoxWithConstraintsScope.UI(
    state: AccountsState,
    onEvent: (AccountsEvent) -> Unit = {}
) {
    val nav = navigation()
    val ivyContext = com.ivy.legacy.ivyWalletCtx()

    val creditCards = remember(state.accountsData) {
        state.accountsData.filter { it.account.creditLimit != null }.toImmutableList()
    }
    val normalAccounts = remember(state.accountsData) {
        state.accountsData.filter { it.account.creditLimit == null }.toImmutableList()
    }

    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }
    var markPaidCard: AccountData? by remember { mutableStateOf(null) }

    var listState = rememberLazyListState()
    if (!state.accountsData.isEmpty()) {
        listState = rememberScrollPositionListState(
            key = "accounts_lazy_column",
            initialFirstVisibleItemIndex = ivyContext.accountsListState?.firstVisibleItemIndex ?: 0,
            initialFirstVisibleItemScrollOffset = ivyContext.accountsListState?.firstVisibleItemScrollOffset
                ?: 0
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .horizontalSwipeListener(
                sensitivity = 200,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    ivyContext.selectMainTab(com.ivy.legacy.data.model.MainTab.HOME)
                },
                onSwipeRight = {
                    ivyContext.selectMainTab(com.ivy.legacy.data.model.MainTab.HOME)
                }
            ),
        state = listState
    ) {
        if (state.creditCardsEnabled) {
            item(key = "credit_cards_section") {
                CreditCardsSection(
                    baseCurrency = state.baseCurrency,
                    cards = creditCards,
                    onCardClick = { markPaidCard = it },
                    onAddCard = {
                        accountModalData = AccountModalData(
                            account = null,
                            baseCurrency = state.baseCurrency,
                            balance = 0.0,
                            creditCardMode = true
                        )
                    }
                )
            }
        }
        item {
            Spacer(Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(24.dp))

                Column {
                    Text(
                        text = stringResource(R.string.accounts),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.weight(1f))

                ReorderButton {
                    onEvent(
                        AccountsEvent.OnReorderModalVisible(reorderVisible = true)
                    )
                }

                Spacer(Modifier.width(24.dp))
            }
            if (!state.hideTotalBalance) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    IncomeExpensesRow(
                        currency = state.baseCurrency,
                        // Both columns are balance totals (not income/expense), so render them
                        // with a neutral on-surface color rather than the semantic green/red.
                        textColor = MaterialTheme.colorScheme.onSurface,
                        incomeLabel = stringResource(id = R.string.total_balance),
                        income = state.totalBalanceWithoutExcluded.toDoubleOrNull() ?: 0.00,
                        expensesLabel = stringResource(id = R.string.total_balance_excluded),
                        expenses = state.totalBalanceWithExcluded.toDoubleOrNull() ?: 0.00
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        items(normalAccounts, key = { it.account.id.value }) {
            // Spring-based placement so cards bump and settle elastically when reordered —
            // they read as physical objects rather than snapping into place.
            Column(
                modifier = Modifier.animateItem(
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    )
                )
            ) {
                Spacer(Modifier.height(16.dp))
                AccountCard(
                    baseCurrency = state.baseCurrency,
                    accountData = it,
                    compactModeEnabled = state.compactAccountsModeEnabled,
                    onBalanceClick = {
                        nav.navigateTo(
                            TransactionsScreen(
                                accountId = it.account.id.value,
                                categoryId = null
                            )
                        )
                    }
                ) {
                    nav.navigateTo(
                        TransactionsScreen(
                            accountId = it.account.id.value,
                            categoryId = null
                        )
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(150.dp)) // scroll hack
        }
    }

    ReorderModalSingleType(
        visible = state.reorderVisible,
        initialItems = normalAccounts,
        dismiss = {
            onEvent(AccountsEvent.OnReorderModalVisible(reorderVisible = false))
        },
        onReordered = {
            onEvent(AccountsEvent.OnReorder(reorderedList = it))
        }
    ) { _, item ->
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp)
                .padding(vertical = 8.dp),
            text = item.account.name.value,
            style = MaterialTheme.typography.titleMedium,
            color = item.account.color.value.toComposeColor()
        )
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = { onEvent(AccountsEvent.OnCreateAccount(it)) },
        onEditAccount = { account, balance ->
            onEvent(AccountsEvent.OnEditAccount(account, balance))
        },
        dismiss = { accountModalData = null }
    )

    markPaidCard?.let { card ->
        MarkPaidSheet(
            card = card,
            payableAccounts = normalAccounts,
            onPayFromAccount = { acc ->
                onEvent(AccountsEvent.MarkPaidFromAccount(card, acc.account.id))
                markPaidCard = null
            },
            onReset = {
                onEvent(AccountsEvent.MarkPaidReset(card))
                markPaidCard = null
            },
            onEdit = {
                accountModalData = AccountModalData(
                    account = card.toLegacyAccount(),
                    baseCurrency = state.baseCurrency,
                    balance = card.balance,
                    creditCardMode = true
                )
                markPaidCard = null
            },
            onViewTransactions = {
                nav.navigateTo(
                    TransactionsScreen(
                        accountId = card.account.id.value,
                        categoryId = null
                    )
                )
                markPaidCard = null
            },
            onDismiss = { markPaidCard = null }
        )
    }
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

@Composable
private fun AccountCard(
    baseCurrency: String,
    accountData: AccountData,
    compactModeEnabled: Boolean,
    onBalanceClick: () -> Unit,
    onClick: () -> Unit
) {
    // Expressive: an ElevatedCard acting as a physical object — no hard 2dp border, no harsh
    // contrasting split-color header block. The account color survives as a subtle accent chip.
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
        ),
    ) {
        val account = accountData.account
        val currency = account.asset.code

        AccountHeader(
            accountData = accountData,
            currency = currency,
            baseCurrency = baseCurrency,
            onBalanceClick = onBalanceClick
        )

        if (!compactModeEnabled) {
            Spacer(Modifier.height(8.dp))

            IncomeExpensesRow(
                currency = currency,
                incomeLabel = stringResource(R.string.month_income),
                income = accountData.monthlyIncome,
                expensesLabel = stringResource(R.string.month_expenses),
                expenses = accountData.monthlyExpenses
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountHeader(
    accountData: AccountData,
    currency: String,
    baseCurrency: String,
    onBalanceClick: () -> Unit
) {
    val account = accountData.account
    val accentColor = account.color.value.toComposeColor()
    val onAccent = findContrastTextColor(accentColor)
    val onCard = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Account color as a rounded accent chip rather than a full-bleed colored block.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                ItemIconSDefaultIcon(
                    iconName = account.icon?.id,
                    defaultIcon = R.drawable.ic_custom_account_s,
                    tint = onAccent
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = account.name.value,
                style = MaterialTheme.typography.titleMedium,
                color = onCard
            )

            if (!account.includeInBalance) {
                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.excluded),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        BalanceRow(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickableNoIndication(rememberInteractionSource()) {
                    onBalanceClick()
                },
            textColor = onCard,
            currency = currency,
            balance = accountData.balance,

            balanceFontSize = 30.sp,
            currencyFontSize = 30.sp,

            currencyUpfront = false
        )

        if (currency != baseCurrency && accountData.balanceBaseCurrency != null) {
            BalanceRowMini(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickableNoIndication(rememberInteractionSource()) {
                        onBalanceClick()
                    }
                    .testTag("baseCurrencyEquivalent"),
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                currency = baseCurrency,
                balance = accountData.balanceBaseCurrency!!,
                currencyUpfront = false
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Preview
@Suppress("MagicNumber")
@Composable
private fun PreviewAccountsTabCompactModeDisabled(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        val acc1 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Phyre"),
            color = ColorInt(Color(0xFF2E7D32).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc2 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("DSK"),
            color = ColorInt(Color(0xFF66BB6A).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc3 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Revolut"),
            color = ColorInt(Color(0xFF2E7D32).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("revolut"),
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc4 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Cash"),
            color = ColorInt(Color(0xFF2E7D32).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("cash"),
            includeInBalance = true,
            orderNum = 0.0,
        )
        val state = AccountsState(
            baseCurrency = "BGN",
            accountsData = persistentListOf(
                AccountData(
                    account = acc1,
                    balance = 2125.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                AccountData(
                    account = acc2,
                    balance = 12125.21,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                AccountData(
                    account = acc3,
                    balance = 1200.0,
                    balanceBaseCurrency = 1979.64,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 1000.30
                ),
                AccountData(
                    account = acc4,
                    balance = 820.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),
            ),
            totalBalanceWithExcluded = "25.54",
            totalBalanceWithExcludedText = "BGN 25.54",
            totalBalanceWithoutExcluded = "25.54",
            totalBalanceWithoutExcludedText = "BGN 25.54",
            reorderVisible = false,
            compactAccountsModeEnabled = false,
            hideTotalBalance = false
        )
        UI(state = state)
    }
}

@Preview
@Suppress("MagicNumber")
@Composable
private fun PreviewAccountsTabCompactModeEnabled(theme: Theme = Theme.LIGHT) {
    IvyWalletPreview(theme = theme) {
        val acc1 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Phyre"),
            color = ColorInt(Color(0xFF2E7D32).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc2 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("DSK"),
            color = ColorInt(Color(0xFF66BB6A).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = null,
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc3 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Revolut"),
            color = ColorInt(Color(0xFF2E7D32).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("revolut"),
            includeInBalance = true,
            orderNum = 0.0,
        )

        val acc4 = Account(
            id = AccountId(UUID.randomUUID()),
            name = NotBlankTrimmedString.unsafe("Cash"),
            color = ColorInt(Color(0xFF2E7D32).toArgb()),
            asset = AssetCode.unsafe("USD"),
            icon = IconAsset.unsafe("cash"),
            includeInBalance = true,
            orderNum = 0.0,
        )
        val state = AccountsState(
            baseCurrency = "BGN",
            accountsData = persistentListOf(
                AccountData(
                    account = acc1,
                    balance = 2125.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 920.0,
                    monthlyIncome = 3045.0
                ),
                AccountData(
                    account = acc2,
                    balance = 12125.21,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 1350.50,
                    monthlyIncome = 8000.48
                ),
                AccountData(
                    account = acc3,
                    balance = 1200.0,
                    balanceBaseCurrency = 1979.64,
                    monthlyExpenses = 750.0,
                    monthlyIncome = 1000.30
                ),
                AccountData(
                    account = acc4,
                    balance = 820.0,
                    balanceBaseCurrency = null,
                    monthlyExpenses = 340.0,
                    monthlyIncome = 400.0
                ),
            ),
            totalBalanceWithExcluded = "25.54",
            totalBalanceWithExcludedText = "BGN 25.54",
            totalBalanceWithoutExcluded = "25.54",
            totalBalanceWithoutExcludedText = "BGN 25.54",
            reorderVisible = false,
            compactAccountsModeEnabled = true,
            hideTotalBalance = false
        )
        UI(state = state)
    }
}

/** For screen shot testing **/
@Composable
fun AccountsTabNonCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeDisabled(theme)
}

/** For screen shot testing **/
@Composable
fun AccountsTabCompactUITest(dark: Boolean) {
    val theme = when (dark) {
        true -> Theme.DARK
        false -> Theme.LIGHT
    }
    PreviewAccountsTabCompactModeEnabled(theme)
}