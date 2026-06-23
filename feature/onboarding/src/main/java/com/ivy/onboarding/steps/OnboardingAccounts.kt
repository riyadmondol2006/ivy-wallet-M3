package com.ivy.onboarding.steps

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.AccountBalance
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingToolbar
import com.ivy.ui.R
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.components.ItemIconMDefaultIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.edit.AccountModal
import com.ivy.wallet.ui.theme.modal.edit.AccountModalData
import com.ivy.wallet.ui.theme.toComposeColor
import com.ivy.wallet.ui.theme.wallet.AmountCurrencyB1Row

/**
 * Material 3 Expressive onboarding step for adding accounts.
 *
 * Rebuilt from scratch: the heavy raster illustration is replaced by lightweight M3 line-art,
 * the split-color account rows become asymmetrical [ElevatedCard]s on a tonal surface, and the
 * rigid suggestion pills become a responsive, reflowing [FlowRow] of [InputChip]s.
 */
@ExperimentalFoundationApi
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoxWithConstraintsScope.OnboardingAccounts(
    baseCurrency: String,

    suggestions: List<CreateAccountData>,
    accounts: List<AccountBalance>,

    onCreateAccount: (CreateAccountData) -> Unit = { },
    onEditAccount: (Account, Double) -> Unit = { _, _ -> },

    onSkip: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var accountModalData: AccountModalData? by remember { mutableStateOf(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        stickyHeader {
            val nav = navigation()
            OnboardingToolbar(
                hasSkip = accounts.isEmpty(),
                onBack = { nav.onBackPressed() },
                onSkip = onSkip
            )
        }

        item {
            Column(
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow,
                    )
                )
            ) {
                Spacer(Modifier.height(8.dp))

                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.add_accounts),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (accounts.isEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    LineArtHero()
                    Spacer(Modifier.height(32.dp))
                } else {
                    Spacer(Modifier.height(24.dp))

                    accounts.forEach { accountBalance ->
                        AccountCard(
                            baseCurrency = baseCurrency,
                            accountBalance = accountBalance,
                            onClick = {
                                accountModalData = AccountModalData(
                                    account = accountBalance.account,
                                    baseCurrency = baseCurrency,
                                    balance = accountBalance.balance,
                                    autoFocusKeyboard = false
                                )
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    text = stringResource(R.string.suggestions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                SuggestionChips(
                    suggestions = suggestions.filter { suggestion ->
                        accounts.map { it.account.name.toLowerCaseLocal() }
                            .contains(suggestion.name.toLowerCaseLocal()).not()
                    },
                    onAddSuggestion = onCreateAccount,
                    onAddNew = {
                        accountModalData = AccountModalData(
                            account = null,
                            baseCurrency = baseCurrency,
                            balance = 0.0
                        )
                    }
                )

                Spacer(Modifier.height(120.dp))
            }
        }
    }

    if (accounts.isNotEmpty()) {
        Button(
            onClick = onDoneClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .height(56.dp)
                .testTag("next"),
            shape = RoundedCornerShape(percent = 50),
        ) {
            Text(
                text = stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    AccountModal(
        modal = accountModalData,
        onCreateAccount = onCreateAccount,
        onEditAccount = onEditAccount,
        dismiss = {
            accountModalData = null
        }
    )
}

/** Lightweight Material 3 line-art hero — replaces the heavy isometric raster illustration. */
@Composable
private fun LineArtHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(68.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

/** Responsive, reflowing suggestion chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChips(
    suggestions: List<CreateAccountData>,
    onAddSuggestion: (CreateAccountData) -> Unit,
    onAddNew: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        suggestions.forEach { suggestion ->
            InputChip(
                selected = false,
                onClick = { onAddSuggestion(suggestion) },
                label = { Text(suggestion.name) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.AvatarSize),
                    )
                },
                shape = RoundedCornerShape(percent = 50),
            )
        }

        AssistChip(
            onClick = onAddNew,
            label = { Text(stringResource(R.string.add_new)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            shape = RoundedCornerShape(percent = 50),
        )
    }
}

@Composable
private fun AccountCard(
    baseCurrency: String,
    accountBalance: AccountBalance,
    onClick: () -> Unit
) {
    val account = accountBalance.account
    val accentColor = account.color.toComposeColor()
    val onAccent = findContrastTextColor(accentColor)

    // Expressive ElevatedCard with a sweeping, asymmetrical corner profile.
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 8.dp,
            bottomEnd = 28.dp,
            bottomStart = 8.dp,
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                ItemIconMDefaultIcon(
                    iconName = account.icon,
                    defaultIcon = R.drawable.ic_custom_account_m,
                    tint = onAccent
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                AmountCurrencyB1Row(
                    amount = accountBalance.balance,
                    currency = account.currency ?: baseCurrency,
                    amountFontWeight = FontWeight.ExtraBold,
                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Empty() {
    IvyWalletPreview {
        val baseCurrency = "BGN"
        OnboardingAccounts(
            baseCurrency = baseCurrency,
            suggestions = listOf(
                CreateAccountData("Cash", baseCurrency, Green, "cash", 0.0),
                CreateAccountData("Bank", baseCurrency, Ivy, "bank", 0.0),
                CreateAccountData("Revolut", baseCurrency, Color(0xFF4DCAFF), "revolut", 0.0),
            ),
            accounts = listOf()
        )
    }
}

@ExperimentalFoundationApi
@Preview
@Composable
private fun Preview_Accounts() {
    IvyWalletPreview {
        val baseCurrency = "BGN"
        OnboardingAccounts(
            baseCurrency = baseCurrency,
            suggestions = listOf(
                CreateAccountData("Cash", baseCurrency, Green, "cash", 0.0),
                CreateAccountData("Bank", baseCurrency, Ivy, "bank", 0.0),
                CreateAccountData("Revolut", baseCurrency, Color(0xFF4DCAFF), "revolut", 0.0),
            ),
            accounts = listOf(
                AccountBalance(
                    account = Account(name = "Cash", color = Green.toArgb(), icon = "cash"),
                    balance = 0.0
                ),
                AccountBalance(
                    account = Account(name = "Revolut", color = IvyDark.toArgb(), icon = "cash"),
                    balance = 0.0
                ),
            )
        )
    }
}
