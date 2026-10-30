package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.wallet.ui.theme.Gray

/**
 * Determinate Material 3 progress bar (kept under its old name so existing callers migrate for free).
 * [percent] is clamped to 0..1 and NaN/infinite values (e.g. a zero total) show as empty.
 * [positiveProgress] is kept for source compatibility; the bar now always uses the primary colour.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ProgressBar(
    modifier: Modifier = Modifier,
    notFilledColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    positiveProgress: Boolean = true,
    percent: Double
) {
    val progress = percent.takeIf { it.isFinite() }?.toFloat()?.coerceIn(0f, 1f) ?: 0f
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        trackColor = notFilledColor,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        ProgressBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 16.dp),
            notFilledColor = Gray,
            percent = 0.6
        )
    }
}
