package com.ivy.categories

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.ui.component.IvyBottomActionBar

@Composable
internal fun BoxWithConstraintsScope.CategoriesBottomBar(
    onClose: () -> Unit,
    onAddCategory: () -> Unit
) {
    IvyBottomActionBar(onNavigateBack = onClose) {
        Button(
            onClick = onAddCategory,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.add_category))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PreviewBottomBar() {
    IvyWalletPreview {
        CategoriesBottomBar(
            onAddCategory = {},
            onClose = {}
        )
    }
}
