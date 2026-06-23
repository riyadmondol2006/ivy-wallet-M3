package com.ivy.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.system.IvyMotion
import com.ivy.legacy.utils.format
import com.ivy.ui.R

/**
 * Home credit-cards block. Shown separately from the balance hero: aggregates all cards into a
 * prominent "Limit left" figure plus a To pay / Limit duo and a progress bar. Tapping opens the
 * Accounts tab's Credit group.
 */
@Composable
fun CreditCardsSummaryCard(
    currency: String,
    summary: CreditCardsSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (summary.totalLimit > 0.0) {
        (summary.totalLimitLeft / summary.totalLimit).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = IvyMotion.spatialSpring(),
        label = "credit-summary-progress"
    )

    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.credit_cards),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${summary.totalLimitLeft.format(currency)} $currency",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.limit_left),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.amount_to_pay),
                    value = "${summary.totalOwed.format(currency)} $currency",
                    valueColor = if (summary.totalOwed > 0.0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                SummaryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.limit_label),
                    value = "${summary.totalLimit.format(currency)} $currency",
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
