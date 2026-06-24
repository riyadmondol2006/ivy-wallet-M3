package com.ivy.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.base.legacy.Theme
import com.ivy.design.system.IvyExpressiveShapes
import com.ivy.legacy.Constants
import com.ivy.legacy.rootScreen
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.openUrl
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.springBounce
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.BudgetScreen
import com.ivy.navigation.CategoriesScreen
import com.ivy.navigation.IvyPreview
import com.ivy.navigation.LoansScreen
import com.ivy.navigation.PlannedPaymentsScreen
import com.ivy.navigation.ReportScreen
import com.ivy.navigation.SearchScreen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1
import java.util.UUID
import kotlin.math.abs

private const val SWIPE_UP_THRESHOLD_CLOSE_MORE_MENU = 300

/**
 * Home "More" panel — a Material 3 surface that slides up over Home when the header menu icon is
 * tapped (or the user swipes down). Replaces the old circular-reveal Canvas overlay. Holds a search
 * field, a quick-access tile grid, a prominent Cloud Sync card, the savings-goal progress, and the
 * open-source fork card. Closing is handled by the close button, swipe-up, or system back
 * (via [AddModalBackHandling]).
 */
@Suppress("LongParameterList", "MagicNumber")
@Composable
fun BoxWithConstraintsScope.MoreMenu(
    expanded: Boolean,

    balance: Double,
    buffer: Double,
    currency: String,
    theme: Theme,

    manualSyncVisible: Boolean,
    syncing: Boolean,

    setExpanded: (Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    onBufferClick: () -> Unit,
    onManualSync: () -> Unit,
    onSetUpSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percentExpanded by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounce(),
        label = "moreMenuExpand"
    )

    if (percentExpanded < 0.01f) return

    val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
    val onSyncClick = { if (manualSyncVisible) onManualSync() else onSetUpSync() }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxSize()
            .zIndex(510f)
            .graphicsLayer {
                translationY = (1f - percentExpanded) * heightPx
                alpha = percentExpanded
            }
            .verticalSwipeListener(
                sensitivity = SWIPE_UP_THRESHOLD_CLOSE_MORE_MENU,
                state = rememberSwipeListenerState(),
                onSwipeUp = { setExpanded(false) }
            )
    ) {
        val modalId = remember { UUID.randomUUID() }
        AddModalBackHandling(modalId = modalId, visible = expanded) {
            setExpanded(false)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            TopBar(
                syncing = syncing,
                onClose = { setExpanded(false) },
                onSync = onSyncClick,
            )

            Spacer(Modifier.height(8.dp))

            val nav = navigation()
            SearchButton { nav.navigateTo(SearchScreen) }

            Spacer(Modifier.height(24.dp))

            QuickAccess(theme = theme, onSwitchTheme = onSwitchTheme)

            Spacer(Modifier.height(24.dp))

            SyncCard(
                configured = manualSyncVisible,
                syncing = syncing,
                onClick = onSyncClick,
            )

            Spacer(Modifier.height(12.dp))

            SavingsGoalCard(
                buffer = buffer,
                balance = balance,
                currency = currency,
                onClick = onBufferClick,
            )

            Spacer(Modifier.height(12.dp))

            OpenSourceCard()
        }
    }
}

@Composable
private fun TopBar(
    syncing: Boolean,
    onClose: () -> Unit,
    onSync: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("more_menu_close"),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.width(4.dp))

        Text(
            text = stringResource(R.string.more),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.weight(1f))

        IconButton(
            onClick = onSync,
            enabled = !syncing,
            modifier = Modifier.testTag("more_menu_sync"),
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
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SearchButton(
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("home_search_button"),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = stringResource(R.string.search_transactions),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickAccess(
    theme: Theme,
    onSwitchTheme: () -> Unit
) {
    val nav = navigation()
    val rootScreen = rootScreen()

    val themeIcon = when (theme) {
        Theme.LIGHT -> R.drawable.home_more_menu_light_mode
        Theme.DARK -> R.drawable.home_more_menu_dark_mode
        Theme.AMOLED_DARK -> R.drawable.home_more_menu_amoled_dark_mode
        Theme.AUTO -> R.drawable.home_more_menu_auto_mode
    }
    val themeLabel = when (theme) {
        Theme.LIGHT -> R.string.light_mode
        Theme.DARK -> R.string.dark_mode
        Theme.AMOLED_DARK -> R.string.amoled_mode
        Theme.AUTO -> R.string.auto_mode
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            text = stringResource(R.string.quick_access),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_settings,
                label = stringResource(R.string.settings),
            ) { nav.navigateTo(SettingsScreen) }
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_categories,
                label = stringResource(R.string.categories),
            ) { nav.navigateTo(CategoriesScreen) }
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = themeIcon,
                label = stringResource(themeLabel),
            ) { onSwitchTheme() }
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_planned_payments,
                label = stringResource(R.string.planned_payments),
            ) { nav.navigateTo(PlannedPaymentsScreen) }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_share,
                label = stringResource(R.string.share_ivy),
            ) { rootScreen.shareIvyWallet() }
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_reports,
                label = stringResource(R.string.reports),
            ) { nav.navigateTo(ReportScreen) }
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_budgets,
                label = stringResource(R.string.budgets),
            ) { nav.navigateTo(BudgetScreen) }
            QuickTile(
                modifier = Modifier.weight(1f),
                icon = R.drawable.home_more_menu_loans,
                label = stringResource(R.string.loans),
            ) { nav.navigateTo(LoansScreen) }
        }
    }
}

@Composable
private fun QuickTile(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = IvyExpressiveShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SyncCard(
    configured: Boolean,
    syncing: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !syncing,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("more_menu_sync_card"),
        shape = IvyExpressiveShapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cloud_sync),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(
                        if (configured) R.string.cloud_sync_now else R.string.cloud_sync_not_set_up
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.width(12.dp))

            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun SavingsGoalCard(
    buffer: Double,
    balance: Double,
    currency: String,
    onClick: () -> Unit,
) {
    val leftToSpend = balance - buffer
    val bufferExceeded = balance < buffer
    val fraction = if (balance != 0.0) {
        (buffer / balance).coerceIn(0.0, 1.0).toFloat()
    } else {
        1f
    }
    val fillColor = when {
        fraction <= 0.25f -> MaterialTheme.colorScheme.tertiary
        fraction <= 0.50f -> MaterialTheme.colorScheme.primary
        fraction <= 0.75f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("savings_goal_row"),
        shape = IvyExpressiveShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.savings_goal),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.weight(1f))

                AmountCurrencyB1(
                    amount = buffer,
                    currency = currency,
                    amountFontWeight = FontWeight.ExtraBold,
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = fillColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(
                    if (bufferExceeded) R.string.buffer_exceeded_by else R.string.left_to_spend
                ) + " " + abs(leftToSpend).format(currency),
                style = MaterialTheme.typography.bodyMedium,
                color = if (bufferExceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun OpenSourceCard() {
    val uriHandler = LocalUriHandler.current
    Surface(
        onClick = {
            openUrl(
                uriHandler = uriHandler,
                url = Constants.URL_IVY_WALLET_REPO
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = IvyExpressiveShapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.github_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ivy_wallet_open_source),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = Constants.URL_IVY_WALLET_REPO,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview_Expanded() {
    IvyPreview {
        MoreMenu(
            expanded = true,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            manualSyncVisible = true,
            syncing = false,
            setExpanded = {},
            onSwitchTheme = {},
            onBufferClick = {},
            onManualSync = {},
            onSetUpSync = {},
        )
    }
}

@Preview
@Composable
private fun BoxWithConstraintsScope.Preview() {
    IvyPreview {
        var expanded by remember { mutableStateOf(false) }

        MoreMenu(
            expanded = expanded,
            balance = 7523.43,
            buffer = 5000.0,
            currency = "BGN",
            theme = Theme.LIGHT,
            manualSyncVisible = false,
            syncing = false,
            setExpanded = { expanded = it },
            onSwitchTheme = {},
            onBufferClick = {},
            onManualSync = {},
            onSetUpSync = {},
        )
    }
}
