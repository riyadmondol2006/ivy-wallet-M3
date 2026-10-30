package com.ivy.loans.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Blue

@Composable
internal fun BoxWithConstraintsScope.LoanBottomBar(
    tab: LoanTab,
    selectTab: (LoanTab) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.add)
            )
        }

        NavigationBar {
            LoanTabItem(
                icon = Icons.Outlined.PendingActions,
                name = "Pending",
                selected = tab == LoanTab.PENDING,
                onClick = { selectTab(LoanTab.PENDING) }
            )
            LoanTabItem(
                icon = Icons.Outlined.TaskAlt,
                name = "Completed",
                selected = tab == LoanTab.COMPLETED,
                onClick = { selectTab(LoanTab.COMPLETED) }
            )
        }
    }
}

@Composable
private fun RowScope.LoanTabItem(
    icon: ImageVector,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        modifier = Modifier.testTag(name.lowercase()),
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        label = { Text(text = name) }
    )
}

@Preview
@Composable
private fun PreviewTabularBottomBar() {
    IvyWalletPreview {
        Column(
            Modifier
                .fillMaxSize()
                .background(Blue)
        ) {
        }

        LoanBottomBar(
            tab = LoanTab.PENDING,
            selectTab = {},
            onAdd = {}
        )
    }
}
