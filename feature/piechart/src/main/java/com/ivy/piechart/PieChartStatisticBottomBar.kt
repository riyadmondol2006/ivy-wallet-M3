package com.ivy.piechart

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.system.income
import com.ivy.design.system.onIncome
import com.ivy.ui.R
import com.ivy.ui.component.IvyBottomActionBar

@Composable
fun BoxWithConstraintsScope.PieChartStatisticBottomBar(
    type: TransactionType,
    onClose: () -> Unit,
    onAdd: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    IvyBottomActionBar(
        onNavigateBack = onClose,
        modifier = modifier,
        navigationIcon = Icons.Rounded.Close,
        navigationContentDescription = stringResource(R.string.close),
    ) {
        val isIncome = type == TransactionType.INCOME
        Button(
            onClick = { onAdd(type) },
            modifier = Modifier.weight(1f),
            colors = if (isIncome) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.income,
                    contentColor = MaterialTheme.colorScheme.onIncome,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (isIncome) R.string.add_income else R.string.add_expense
                )
            )
        }
    }
}

@Preview
@Composable
private fun PreviewBottomBar() {
    com.ivy.legacy.IvyWalletPreview {
        PieChartStatisticBottomBar(
            type = TransactionType.INCOME,
            onAdd = {},
            onClose = {}
        )
    }
}
