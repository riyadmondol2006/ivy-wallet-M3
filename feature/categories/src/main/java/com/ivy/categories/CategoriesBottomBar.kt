package com.ivy.categories

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R

@Composable
internal fun BoxWithConstraintsScope.CategoriesBottomBar(
    onClose: () -> Unit,
    onAddCategory: () -> Unit
) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }

            Spacer(Modifier.width(12.dp))

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
