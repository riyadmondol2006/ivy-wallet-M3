package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview

/** Native Material 3 divider, colored from the active [MaterialTheme.colorScheme]. */
@Composable
fun IvyDividerLine(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
fun IvyDividerLineRounded(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 50))
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IvyDividerLine()
    }
}
