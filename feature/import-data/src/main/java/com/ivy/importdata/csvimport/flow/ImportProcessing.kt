package com.ivy.importdata.csvimport.flow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.ui.R

@Composable
fun ImportProcessing(
    progressPercent: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(80.dp))

        Text(
            text = stringResource(R.string.please_wait),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "$progressPercent%",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.importing_the_csv_file),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { progressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    com.ivy.legacy.IvyWalletPreview {
        ImportProcessing(
            progressPercent = 49
        )
    }
}
