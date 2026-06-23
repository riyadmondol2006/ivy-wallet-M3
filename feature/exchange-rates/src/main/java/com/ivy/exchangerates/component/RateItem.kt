package com.ivy.exchangerates.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l1_buildingBlocks.SpacerHor
import com.ivy.design.l1_buildingBlocks.SpacerWeight
import com.ivy.exchangerates.data.RateUi
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.utils.format

@Composable
fun RateItem(
    rate: RateUi,
    onDelete: (() -> Unit)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically

        ) {
            val currencyValue: Double = 1.0
            RateColumn(
                label = "Sell",
                rate = rate.from,
                value = currencyValue.format(currencyCode = rate.from)
            )

            SpacerHor(width = 16.dp)
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "arrow to next",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SpacerHor(width = 16.dp)
            RateColumn(
                label = "Buy",
                rate = rate.to,
                value = rate.rate.format(currencyCode = rate.to)
            )

            if (onDelete != null) {
                SpacerWeight(weight = 1f)
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete rate",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RateColumn(label: String, rate: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = rate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// region Preview
@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        RateItem(
            rate = RateUi(
            from = "BGN",
            to = "EUR",
            rate = 1.95583
        ),
            onDelete = null,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun Preview_Delete() {
    IvyWalletComponentPreview {
        RateItem(
            rate = RateUi(
                from = "BGN",
                to = "EUR",
                rate = 1.95583
        ),
            onDelete = { },
            onClick = {}
        )
    }
}
// endregion
