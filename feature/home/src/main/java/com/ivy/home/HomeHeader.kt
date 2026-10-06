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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.system.IvySpacing
import com.ivy.design.system.expense
import com.ivy.design.system.financialNumberStyle
import com.ivy.design.system.income
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.shortenAmount
import com.ivy.legacy.utils.shouldShortAmount
import com.ivy.legacy.utils.springBounce
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1Row
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private const val HiddenAmount = "****"

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

        Spacer(Modifier.height(12.dp))

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

        Spacer(Modifier.height(12.dp))

        if (percentExpanded < 0.5f) {
            HorizontalDivider(
                modifier = Modifier.alpha(1f - percentExpanded),
                color = MaterialTheme.colorScheme.outlineVariant,
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
    // Same geometry as a Material 3 top app bar: 16dp start gutter, 4dp end inset so the 48dp
    // icon buttons' glyphs land on the 16dp gutter, and a 48dp minimum height.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(start = IvySpacing.screenGutter, end = 4.dp),
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

            // Collapsed state: the balance on one line, the same height as the greeting, so the
            // header never changes height while it collapses.
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
                    textColor = MaterialTheme.colorScheme.onSurface,
                    currency = currency,
                    balance = balance,
                    shortenBigNumbers = true,
                    hiddenMode = hideBalance,
                )
            }
        }

        Spacer(Modifier.width(IvySpacing.inlineGap))

        PeriodButton(
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
            period = period,
            onClick = onShowMonthModal,
        )

        Spacer(Modifier.width(4.dp))

        if (manualSyncVisible) {
            IconButton(
                onClick = onManualSync,
                enabled = !syncing,
                modifier = Modifier.testTag("home_manual_sync"),
            ) {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CloudSync,
                        contentDescription = stringResource(R.string.cloud_sync_now),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Opens the "More" panel (quick access, sync, savings goal, open-source).
        IconButton(
            onClick = onOpenMoreMenu,
            modifier = Modifier.testTag("home_more_menu_arrow"),
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Material 3 outlined pill with the calendar icon; a fixed min width keeps it from jumping between months. */
@Composable
private fun PeriodButton(
    period: TimePeriod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 130.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            text = period.toDisplayShort(
                startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

@Suppress("LongParameterList")
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
    onBalanceClick: () -> Unit,
    percentExpanded: Float,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nav = navigation()
    // Hero balance card: total balance, the month's income/expense split and the resulting cashflow.
    // No swipe listener here: the whole card must scroll the list like any other item.
    Column(
        modifier = modifier
            .padding(horizontal = IvySpacing.screenGutter)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
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
                hidden = hideIncome,
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
                hidden = false,
                amountColor = MaterialTheme.colorScheme.expense,
                testTag = "home_card_expense",
                onClick = {
                    nav.navigateTo(
                        PieChartStatisticScreen(type = TransactionType.EXPENSE),
                    )
                },
            )
        }

        val cashflow = monthlyIncome - monthlyExpenses
        if (cashflow != 0.0 && !hideBalance && !hideIncome) {
            Spacer(Modifier.height(12.dp))

            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cashflow < 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.income
                },
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
        hiddenMode -> HiddenAmount
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

@Suppress("LongParameterList")
@Composable
private fun BalanceStat(
    @DrawableRes icon: Int,
    label: String,
    amount: Double,
    currency: String,
    hidden: Boolean,
    amountColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 6.dp, horizontal = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
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

        if (hidden) {
            Text(
                text = HiddenAmount,
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
            )
        } else {
            AmountCurrencyB1Row(
                amount = amount,
                currency = currency,
                textColor = amountColor,
                shortenBigNumbers = true,
            )
        }
    }
}
