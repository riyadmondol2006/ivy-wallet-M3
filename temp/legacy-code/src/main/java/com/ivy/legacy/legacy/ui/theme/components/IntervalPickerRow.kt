package com.ivy.wallet.ui.theme.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.model.IntervalType
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletComponentPreview
import com.ivy.legacy.forDisplay
import com.ivy.legacy.utils.capitalizeLocal
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.ui.R

private const val RepeatIntervalCharLimit = 5

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IntervalPickerRow(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalN: (Int) -> Unit,
    onSetIntervalType: (IntervalType) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        var interNTextFieldValue by remember(intervalN) {
            mutableStateOf(selectEndTextFieldValue(intervalN.toString()))
        }

        IvyNumberTextField(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = UI.shapes.rFull
                )
                .padding(vertical = 12.dp),
            value = interNTextFieldValue,
            textColor = MaterialTheme.colorScheme.onSurface,
            hint = "0"
        ) {
            val filteredText = it.text.take(RepeatIntervalCharLimit)
            if (it.text != interNTextFieldValue.text) {
                try {
                    onSetIntervalN(filteredText.toInt())
                } catch (e: Exception) {
                }
            }
            interNTextFieldValue = it.copy(text = filteredText)
        }

        Spacer(Modifier.width(12.dp))

        IntervalTypeSelector(
            intervalN = intervalN,
            intervalType = intervalType
        ) {
            onSetIntervalType(it)
        }

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun RowScope.IntervalTypeSelector(
    intervalN: Int,
    intervalType: IntervalType,

    onSetIntervalType: (IntervalType) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.weight(1f),
        shape = UI.shapes.rFull,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(20.dp))

            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    onSetIntervalType(
                        when (intervalType) {
                            IntervalType.DAY -> IntervalType.YEAR
                            IntervalType.WEEK -> IntervalType.DAY
                            IntervalType.MONTH -> IntervalType.WEEK
                            IntervalType.YEAR -> IntervalType.MONTH
                        }
                    )
                }
            ) {
                Icon(
                    modifier = Modifier.rotate(-180f),
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "interval_type_arrow_left",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = intervalType.forDisplay(intervalN).capitalizeLocal(),
                style = UI.typo.b2.style(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                modifier = Modifier.size(48.dp),
                onClick = {
                    onSetIntervalType(
                        when (intervalType) {
                            IntervalType.DAY -> IntervalType.WEEK
                            IntervalType.WEEK -> IntervalType.MONTH
                            IntervalType.MONTH -> IntervalType.YEAR
                            IntervalType.YEAR -> IntervalType.DAY
                        }
                    )
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "interval_type_arrow_right",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(20.dp))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletComponentPreview {
        IntervalPickerRow(
            intervalN = 1,
            intervalType = IntervalType.WEEK,
            onSetIntervalN = {}
        ) {
        }
    }
}
