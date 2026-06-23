package com.ivy.onboarding.steps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletPreview
import com.ivy.navigation.navigation
import com.ivy.onboarding.components.OnboardingProgressSlider
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.CloseButton

@Composable
fun OnboardingType(
    onStartImport: () -> Unit,
    onStartFresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        val nav = navigation()
        CloseButton(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            nav.onBackPressed()
        }

        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.import_csv_file),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.from_ivy_or_another_app),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        Image(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            painter = painterResource(id = R.drawable.onboarding_illustration_import),
            contentDescription = "import illustration"
        )

        OnboardingProgressSlider(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            selectedStep = 0,
            stepsCount = 4,
        )

        Spacer(Modifier.weight(1f))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.importing_another_time_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onStartImport,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_export_csv),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.import_backup_file),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStartFresh,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.start_fresh),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        OnboardingType(
            onStartImport = {}
        ) {
        }
    }
}
