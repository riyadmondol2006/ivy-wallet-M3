package com.ivy.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The one list-item card used across the app (accounts, categories, transactions, loans, planned
 * payments): a filled M3 card on `surfaceContainer` with the `large` corner radius and no border.
 */
@Composable
fun IvyListCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = colors,
            content = content,
        )
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = colors,
            content = content,
        )
    }
}
