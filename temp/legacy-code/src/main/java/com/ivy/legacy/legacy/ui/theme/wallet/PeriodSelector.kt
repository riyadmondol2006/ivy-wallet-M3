package com.ivy.wallet.ui.theme.wallet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.ui.R

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun PeriodSelector(
    period: TimePeriod,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowChoosePeriodModal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (period.month != null) {
            IconButton(
                onClick = onPreviousMonth,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    modifier = Modifier.rotate(-180f),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Previous month"
                )
            }
        }

        Spacer(Modifier.weight(1f))

        AssistChip(
            onClick = onShowChoosePeriodModal,
            label = {
                Text(
                    text = period.toDisplayShort(
                        startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                        timeConverter = LocalTimeConverter.current,
                        timeProvider = LocalTimeProvider.current,
                        timeFormatter = LocalTimeFormatter.current,
                    )
                )
            },
            leadingIcon = {
                Icon(
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    painter = painterResource(id = R.drawable.ic_calendar),
                    contentDescription = "Choose period"
                )
            },
            trailingIcon = {
                Icon(
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                    painter = painterResource(id = R.drawable.ic_expand_more),
                    contentDescription = null
                )
            }
        )

        Spacer(Modifier.weight(1f))

        if (period.month != null) {
            IconButton(
                onClick = onNextMonth,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "Next month"
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        PeriodSelector(
            period = TimePeriod.currentMonth(
                startDayOfMonth = 1
            ), // preview
            onPreviousMonth = { },
            onNextMonth = { },
            onShowChoosePeriodModal = {}
        )
    }
}
