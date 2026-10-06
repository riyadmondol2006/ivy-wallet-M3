package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.ivy.design.system.IvySpacing

/** Thin Material 3 separator between a screen's summary area and its transaction list. */
@Composable
fun TransactionsDividerLine(
    modifier: Modifier = Modifier,
    paddingHorizontal: Dp = IvySpacing.screenGutter
) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = paddingHorizontal),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
