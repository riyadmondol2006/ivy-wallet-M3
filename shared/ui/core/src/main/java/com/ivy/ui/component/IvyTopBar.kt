package com.ivy.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard Material 3 top app bar with the shared [BackButton]. Use it on every pushed screen
 * instead of hand-rolled toolbars so titles, insets and the back affordance match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IvyTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(text = title) },
        navigationIcon = { BackButton(onClick = onBack) },
        actions = actions,
    )
}
