package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.expense
import com.ivy.design.system.financialNumberStyle
import com.ivy.design.system.income
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.transaction.TransactionsDividerLine
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.shortenAmount
import com.ivy.legacy.utils.shouldShortAmount
import com.ivy.legacy.utils.springBounce
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Suppress("LongParameterList")
@ExperimentalAnimationApi
@Composable
internal fun HomeHeader(
    expanded: Boolean,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    manualSyncVisible: Boolean,
    syncing: Boolean,
    onManualSync: () -> Unit,
    onOpenMoreMenu: () -> Unit,
) {
    Column {
        val percentExpanded by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = springBounce(
                stiffness = Spring.StiffnessLow
            ),
            label = "Home Header Expand Collapse"
        )

        Spacer(Modifier.height(20.dp))

        HeaderStickyRow(
            percentExpanded = percentExpanded,
            name = name,
            period = period,
            currency = currency,
            balance = balance,
            hideBalance = hideBalance,

            onShowMonthModal = onShowMonthModal,
            onBalanceClick = onBalanceClick,
            onHiddenBalanceClick = onHiddenBalanceClick,
            onSelectNextMonth = onSelectNextMonth,
            onSelectPreviousMonth = onSelectPreviousMonth,
            manualSyncVisible = manualSyncVisible,
            syncing = syncing,
            onManualSync = onManualSync,
            onOpenMoreMenu = onOpenMoreMenu,
        )

        Spacer(Modifier.height(16.dp))

        if (percentExpanded < 0.5f) {
            TransactionsDividerLine(
                modifier = Modifier.alpha(1f - percentExpanded),
                paddingHorizontal = 0.dp
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun HeaderStickyRow(
    percentExpanded: Float,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
    manualSyncVisible: Boolean,
    syncing: Boolean,
    onManualSync: () -> Unit,
    onOpenMoreMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .alpha(percentExpanded)
                    .testTag("home_greeting_text"),
                text = if (name.isNotNullOrBlank()) {
                    stringResource(
                        R.string.hi_name,
                        name,
                    )
                } else {
                    stringResource(R.string.hi)
                },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            // Balance mini row
            if (percentExpanded < 1f) {
                BalanceRowMini(
                    modifier = Modifier
                        .alpha(alpha = 1f - percentExpanded)
                        .clickableNoIndication(rememberInteractionSource()) {
                            if (hideBalance) {
                                onHiddenBalanceClick()
                            } else {
                                onBalanceClick()
                            }
                        },
                    currency = currency,
                    balance = balance,
                    shortenBigNumbers = true,
                    hiddenMode = hideBalance,
                    doubleRowDisplay = true,
                )
            }
        }

        IvyOutlinedButton(
            modifier = Modifier.horizontalSwipeListener(
                sensitivity = 75,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    onSelectNextMonth()
                },
                onSwipeRight = {
                    onSelectPreviousMonth()
                },
            ),
            iconStart = R.drawable.ic_calendar,
            text = period.toDisplayShort(
                startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            minWidth = 130.dp,
        ) {
            onShowMonthModal()
        }

        Spacer(Modifier.width(8.dp))

        if (manualSyncVisible) {
            IconButton(
                onClick = onManualSync,
                enabled = !syncing,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("home_manual_sync"),
            ) {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cloud_sync_now),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Opens the redesigned "More" panel (quick access, sync, savings goal, open-source).
        IconButton(
            onClick = onOpenMoreMenu,
            modifier = Modifier
                .size(40.dp)
                .testTag("home_more_menu_arrow"),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun CashFlowInfo(
    currency: String,
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideBalance: Boolean,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onBalanceClick: () -> Unit,
    percentExpanded: Float,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nav = navigation()
    Column(
        modifier = modifier
            .verticalSwipeListener(
                sensitivity = Constants.SWIPE_DOWN_THRESHOLD_OPEN_MORE_MENU,
                state = rememberSwipeListenerState(),
                onSwipeDown = {
                    onOpenMoreMenu()
                },
            )
            .padding(horizontal = 16.dp),
    ) {
        // M3 hero balance card: total balance + inline income/expense split.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UI.shapes.r3)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, UI.shapes.r3)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.total_balance),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(6.dp))

            ExpressiveBalanceRow(
                modifier = Modifier
                    .clickableNoIndication(rememberInteractionSource()) {
                        if (hideBalance) {
                            onHiddenBalanceClick()
                        } else {
                            onBalanceClick()
                        }
                    }
                    .testTag("home_balance"),
                currency = currency,
                balance = balance,
                percentExpanded = percentExpanded,
                hiddenMode = hideBalance,
            )

            Spacer(Modifier.height(20.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BalanceStat(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_income,
                    label = stringResource(R.string.income),
                    amount = monthlyIncome,
                    currency = currency,
                    amountColor = MaterialTheme.colorScheme.income,
                    testTag = "home_card_income",
                    onClick = {
                        if (hideIncome) {
                            onHiddenIncomeClick()
                        } else {
                            nav.navigateTo(
                                PieChartStatisticScreen(type = TransactionType.INCOME),
                            )
                        }
                    },
                )

                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                BalanceStat(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_expense,
                    label = stringResource(R.string.expenses),
                    amount = monthlyExpenses.absoluteValue,
                    currency = currency,
                    amountColor = MaterialTheme.colorScheme.expense,
                    testTag = "home_card_expense",
                    onClick = {
                        nav.navigateTo(
                            PieChartStatisticScreen(type = TransactionType.EXPENSE),
                        )
                    },
                )
            }
        }

        val cashflow = monthlyIncome - monthlyExpenses
        if (cashflow != 0.0 && !hideBalance) {
            Spacer(Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                style = UI.typo.nB2.style(
                    color = if (cashflow < 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.income
                    },
                ),
            )
        }
    }
}

/**
 * Material 3 Expressive total-balance display.
 *
 * The number is rendered with the variable-font [financialNumberStyle]: heavy + large while the
 * header is expanded, smoothly thinning and shrinking as it collapses on scroll. [percentExpanded]
 * is already a spring-animated float, so the morph inherits physics-based motion for free.
 */
@Suppress("MagicNumber")
@Composable
private fun ExpressiveBalanceRow(
    currency: String,
    balance: Double,
    percentExpanded: Float,
    hiddenMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val amountText = when {
        hiddenMode -> "****"
        shouldShortAmount(balance) -> shortenAmount(balance)
        else -> balance.format(currency)
    }

    // Expressive scaling: collapsed (0f) -> 30sp / weight 550 ; expanded (1f) -> 48sp / weight 1000.
    val fontSize = (30f + 18f * percentExpanded).sp
    val weight = (550f + 450f * percentExpanded).roundToInt()

    Text(
        modifier = modifier,
        text = "$currency $amountText",
        style = financialNumberStyle(fontSize = fontSize, weight = weight)
            .copy(color = MaterialTheme.colorScheme.onSurface),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BalanceStat(
    @DrawableRes icon: Int,
    label: String,
    amount: Double,
    currency: String,
    amountColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(UI.shapes.r4)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 6.dp, horizontal = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IvyIcon(
                icon = icon,
                tint = amountColor,
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(4.dp))

        AmountCurrencyB1(
            amount = amount,
            currency = currency,
            textColor = amountColor,
            shortenBigNumbers = true,
        )
    }
}
