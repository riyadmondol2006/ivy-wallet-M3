package com.ivy.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.R
import kotlinx.coroutines.launch

val FAB_BUTTON_SIZE = 56.dp

/**
 * Standard Material 3 bottom navigation: a [NavigationBar] (Home/Accounts) plus a
 * [FloatingActionButton]. On Home the FAB opens an M3 [ModalBottomSheet] of add-actions; on
 * Accounts it adds an account. The FAB keeps the shared-element [AddTransactionSharedKey]
 * container transform into the edit screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,

    showAddAccountModal: () -> Unit,
) {
    var showAddSheet by remember { mutableStateOf(false) }

    NavigationBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .testTag("main_nav_bar"),
    ) {
        NavigationBarItem(
            selected = tab == MainTab.HOME,
            onClick = { selectTab(MainTab.HOME) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = null,
                )
            },
            label = { Text(text = stringResource(R.string.home)) },
            modifier = Modifier.testTag("home"),
        )
        NavigationBarItem(
            selected = tab == MainTab.ACCOUNTS,
            onClick = { selectTab(MainTab.ACCOUNTS) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_accounts),
                    contentDescription = null,
                )
            },
            label = { Text(text = stringResource(R.string.accounts)) },
            modifier = Modifier.testTag("accounts"),
        )
    }

    FloatingActionButton(
        onClick = {
            when (tab) {
                MainTab.HOME -> showAddSheet = true
                MainTab.ACCOUNTS -> showAddAccountModal()
            }
        },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 88.dp)
            .testTag("fab_add"),
        // Bold, high-contrast primary FAB (the default M3 primaryContainer reads too faint in
        // dynamic dark themes).
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
    ) {
        // Material vector renders a crisp, full-size '+'. The bundled ic_add is a 12dp glyph that
        // looks tiny inside the FAB.
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "fab_add",
        )
    }

    if (showAddSheet) {
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        // Animate the sheet fully closed, then navigate — so the sheet's exit doesn't overlap and
        // fight the screen-enter transition (which looked janky when both ran at once).
        val selectAndClose: (() -> Unit) -> Unit = { action ->
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    showAddSheet = false
                    action()
                }
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = stringResource(R.string.add_transaction),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                )
                AddOption(
                    icon = R.drawable.ic_income,
                    label = stringResource(R.string.add_income),
                    tint = MaterialTheme.colorScheme.tertiary,
                ) { selectAndClose(onAddIncome) }
                AddOption(
                    icon = R.drawable.ic_expense,
                    label = stringResource(R.string.add_expense),
                    tint = MaterialTheme.colorScheme.error,
                ) { selectAndClose(onAddExpense) }
                AddOption(
                    icon = R.drawable.ic_transfer,
                    label = stringResource(R.string.add_transfer),
                    tint = MaterialTheme.colorScheme.primary,
                ) { selectAndClose(onAddTransfer) }
                AddOption(
                    icon = R.drawable.ic_planned_payments,
                    label = stringResource(R.string.add_planned_payment),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                ) { selectAndClose(onAddPlannedPayment) }
            }
        }
    }
}

@Composable
private fun AddOption(
    @DrawableRes icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = label) },
        leadingContent = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("add_option_${label.lowercase()}"),
    )
}
