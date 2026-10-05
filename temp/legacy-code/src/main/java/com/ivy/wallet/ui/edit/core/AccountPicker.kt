package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.legacy.datamodel.Account
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

/** The picker sheet covers most of the screen so long account lists get room to scroll. */
private const val SheetHeightFraction = 0.85f

@Composable
internal fun AccountSelector(
    account: Account?,
    baseCurrency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountIcon(account)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = account?.name ?: stringResource(R.string.choose_account),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (account != null) {
                    Text(
                        text = account.currency ?: baseCurrency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountPickerSheet(
    title: String,
    accounts: ImmutableList<Account>,
    selectedAccount: Account?,
    baseCurrency: String,
    onSelect: (Account) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var closing by remember { mutableStateOf(false) }

    // Finish hiding this dialog before invoking callbacks that can open another modal.
    fun finish(action: () -> Unit) {
        if (closing) return
        closing = true
        keyboard?.hide()
        scope.launch {
            sheetState.hide()
            onDismiss()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        AccountPickerContent(
            title = title,
            accounts = accounts,
            selectedAccount = selectedAccount,
            baseCurrency = baseCurrency,
            query = query,
            onQueryChange = { query = it },
            onSelect = { account -> finish { onSelect(account) } },
            onAddAccount = { finish(onAddAccount) },
            onClose = { finish {} },
            enabled = !closing,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(SheetHeightFraction),
        )
    }
}

internal fun filterPickerAccounts(
    accounts: List<Account>,
    query: String,
    baseCurrency: String,
): List<Account> {
    val term = query.trim()
    return accounts.filter { account ->
        !account.isDeleted && (
            term.isEmpty() || account.name.contains(term, ignoreCase = true) ||
                (account.currency ?: baseCurrency).contains(term, ignoreCase = true)
            )
    }
}

@Composable
internal fun AccountPickerContent(
    title: String,
    accounts: ImmutableList<Account>,
    selectedAccount: Account?,
    baseCurrency: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (Account) -> Unit,
    onAddAccount: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val matches = remember(accounts, query, baseCurrency) {
        filterPickerAccounts(accounts, query, baseCurrency)
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = matches.indexOfFirst {
            it.id == selectedAccount?.id
        }.coerceAtLeast(0),
    )
    var previousQuery by remember { mutableStateOf(query) }
    LaunchedEffect(query) {
        if (query != previousQuery) {
            listState.scrollToItem(0)
            previousQuery = query
        }
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
            )
            IconButton(onClick = onClose, enabled = enabled) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_account_picker),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().testTag("account_picker_search"),
            enabled = enabled,
            label = { Text(stringResource(R.string.search_accounts)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }, enabled = enabled) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_account_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                focusManager.clearFocus()
            }),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.accounts_number, matches.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        if (matches.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(
                        if (accounts.none { !it.isDeleted }) {
                            R.string.no_accounts_to_choose
                        } else {
                            R.string.no_matching_accounts
                        }
                    ),
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .selectableGroup().testTag("account_picker_list"),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(matches, key = { it.id }) { account ->
                    AccountPickerRow(
                        account = account,
                        currency = account.currency ?: baseCurrency,
                        selected = account.id == selectedAccount?.id,
                        enabled = enabled,
                        onClick = { onSelect(account) },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FilledTonalButton(
            onClick = onAddAccount,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                .heightIn(min = 48.dp).testTag("account_picker_add"),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_account))
        }
    }
}

@Composable
private fun AccountPickerRow(
    account: Account,
    currency: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .testTag("account_picker_item_${account.id}")
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountIcon(account)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currency,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Default.Check,
                contentDescription = null, // Selection is announced by the row's semantics.
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun AccountIcon(account: Account?) {
    val color = account?.color?.toComposeColor() ?: MaterialTheme.colorScheme.secondaryContainer
    Box(
        modifier = Modifier.size(40.dp).background(color, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        ItemIconSDefaultIcon(
            iconName = account?.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = findContrastTextColor(color),
        )
    }
}
