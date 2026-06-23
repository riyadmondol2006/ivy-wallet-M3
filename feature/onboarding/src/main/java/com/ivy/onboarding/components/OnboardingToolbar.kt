package com.ivy.onboarding.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyToolbar

@Composable
fun OnboardingToolbar(
    hasSkip: Boolean,

    onBack: () -> Unit,
    onSkip: () -> Unit
) {
    IvyToolbar(onBack = onBack) {
        if (hasSkip) {
            Spacer(Modifier.weight(1f))

            TextButton(
                onClick = onSkip,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.skip),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(Modifier.width(8.dp))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        OnboardingToolbar(
            hasSkip = true,
            onBack = {}
        ) {
        }
    }
}
